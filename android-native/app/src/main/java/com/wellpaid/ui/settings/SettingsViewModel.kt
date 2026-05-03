package com.wellpaid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.BuildConfig
import com.wellpaid.R
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.UserMeDto
import com.wellpaid.core.model.auth.UserProfilePatchDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.AppUpdateApi
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.core.network.UserApi
import com.wellpaid.data.FamilyMeRepository
import com.wellpaid.security.AppSecurityManager
import com.wellpaid.update.AppUpdateInstaller
import com.wellpaid.util.looksLikeUuid
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Locale

data class SettingsUiState(
    val userFirstName: String? = null,
    val userEmail: String? = null,
    val familyModeEnabled: Boolean = false,
    val profileLoaded: Boolean = false,
)

enum class AppUpdateBusy {
    IDLE,
    CHECKING,
    DOWNLOADING,
}

data class AppUpdateOffer(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String?,
    val apkUrl: String,
)

data class AppUpdateUiState(
    val busy: AppUpdateBusy = AppUpdateBusy.IDLE,
    val updateOffer: AppUpdateOffer? = null,
    val needsInstallPermissionHint: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val familyMeRepository: FamilyMeRepository,
    private val appSecurityManager: AppSecurityManager,
    private val userApi: UserApi,
    private val appUpdateApi: AppUpdateApi,
    @Named("refresh") private val refreshHttpClient: OkHttpClient,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _appUpdateState = MutableStateFlow(AppUpdateUiState())
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    private val loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOutEvents = loggedOut.receiveAsFlow()

    private val snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarMessages = snackbarChannel.receiveAsFlow()

    fun refreshProfile() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update { it.copy(profileLoaded = true) }
            return
        }
        viewModelScope.launch {
            runCatching { userApi.getCurrentUser() }
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            userFirstName = greetingFirstName(dto),
                            userEmail = dto.email,
                            familyModeEnabled = dto.familyModeEnabled,
                            profileLoaded = true,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(profileLoaded = true) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val refresh = tokenStorage.getRefreshToken()
            tokenStorage.clear()
            familyMeRepository.clear()
            appSecurityManager.onLoggedOut()
            loggedOut.send(Unit)
            if (!refresh.isNullOrBlank()) {
                launch { runCatching { authApi.logout(LogoutRequestDto(refresh)) } }
            }
        }
    }

    fun setFamilyModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { userApi.patchProfile(UserProfilePatchDto(familyModeEnabled = enabled)) }
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            familyModeEnabled = dto.familyModeEnabled,
                            userFirstName = greetingFirstName(dto),
                            userEmail = dto.email,
                            profileLoaded = true,
                        )
                    }
                }
        }
    }

    fun dismissUpdateOffer() {
        _appUpdateState.update { it.copy(updateOffer = null, needsInstallPermissionHint = false) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _appUpdateState.update {
                it.copy(
                    busy = AppUpdateBusy.CHECKING,
                    updateOffer = null,
                    needsInstallPermissionHint = false,
                )
            }
            runCatching { appUpdateApi.getManifest() }
                .onSuccess { manifest ->
                    val local = BuildConfig.VERSION_CODE
                    when {
                        manifest.versionCode <= local -> {
                            _appUpdateState.update { it.copy(busy = AppUpdateBusy.IDLE) }
                            snackbarChannel.send(appContext.getString(R.string.settings_app_update_up_to_date))
                        }
                        manifest.apkUrl.isBlank() ||
                            !manifest.apkUrl.trim().startsWith("https://") -> {
                            _appUpdateState.update { it.copy(busy = AppUpdateBusy.IDLE) }
                            snackbarChannel.send(
                                appContext.getString(R.string.settings_app_update_manifest_incomplete),
                            )
                        }
                        else -> {
                            _appUpdateState.update {
                                it.copy(
                                    busy = AppUpdateBusy.IDLE,
                                    updateOffer =
                                        AppUpdateOffer(
                                            versionName = manifest.versionName,
                                            versionCode = manifest.versionCode,
                                            releaseNotes = manifest.releaseNotes,
                                            apkUrl = manifest.apkUrl.trim(),
                                        ),
                                )
                            }
                        }
                    }
                }
                .onFailure { e ->
                    _appUpdateState.update { it.copy(busy = AppUpdateBusy.IDLE) }
                    val detail = e.message?.take(120)?.ifBlank { e.javaClass.simpleName } ?: e.javaClass.simpleName
                    snackbarChannel.send(
                        appContext.getString(R.string.settings_app_update_check_failed, detail),
                    )
                }
        }
    }

    fun downloadOfferedUpdate() {
        val offer = _appUpdateState.value.updateOffer ?: return
        viewModelScope.launch {
            _appUpdateState.update {
                it.copy(busy = AppUpdateBusy.DOWNLOADING, needsInstallPermissionHint = false)
            }
            runCatching {
                val file = AppUpdateInstaller.updateApkFile(appContext)
                AppUpdateInstaller.downloadApkToFile(refreshHttpClient, offer.apkUrl, file)
                val err = AppUpdateInstaller.startSystemInstaller(appContext, file)
                when (err) {
                    null -> {
                        _appUpdateState.update {
                            it.copy(busy = AppUpdateBusy.IDLE, updateOffer = null)
                        }
                    }
                    "permission" -> {
                        _appUpdateState.update {
                            it.copy(busy = AppUpdateBusy.IDLE, needsInstallPermissionHint = true)
                        }
                        snackbarChannel.send(
                            appContext.getString(R.string.settings_app_update_need_install_permission),
                        )
                    }
                    else -> {
                        _appUpdateState.update { it.copy(busy = AppUpdateBusy.IDLE) }
                        snackbarChannel.send(err)
                    }
                }
            }.onFailure { e ->
                _appUpdateState.update { it.copy(busy = AppUpdateBusy.IDLE) }
                snackbarChannel.send(e.message?.take(160) ?: appContext.getString(R.string.login_error_network))
            }
        }
    }

    fun openInstallFromUnknownSourcesSettings() {
        AppUpdateInstaller.openInstallFromUnknownSourcesSettings(appContext)
    }

    private fun greetingFirstName(dto: UserMeDto): String? {
        val custom = dto.displayName?.trim().orEmpty()
        if (custom.isNotEmpty() && !custom.looksLikeUuid()) {
            return custom
        }
        val fromFull = dto.fullName?.trim().orEmpty()
        if (fromFull.isNotEmpty()) {
            val first = fromFull.split(Regex("\\s+")).first()
            if (!first.looksLikeUuid()) {
                return first.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
        }
        val local = dto.email.substringBefore("@").trim()
        if (local.isEmpty() || local.looksLikeUuid()) return null
        return local.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }
}
