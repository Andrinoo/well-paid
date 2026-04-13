package com.wellpaid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.auth.UserProfilePatchDto
import com.wellpaid.core.network.UserApi
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.data.FamilyMeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val displayNameDraft: String = "",
    val isLoadingProfile: Boolean = false,
    val isSavingDisplayName: Boolean = false,
    val profileError: String? = null,
    val displayNameSaveError: String? = null,
    /** Mensagem única para Snackbar (definições guardadas). */
    val snackbarMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenStorage: TokenStorage,
    private val familyMeRepository: FamilyMeRepository,
) : ViewModel() {

    private val loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOutEvents = loggedOut.receiveAsFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingProfile = true, profileError = null)
            }
            val result = runCatching { userApi.getCurrentUser() }
            _uiState.update { s ->
                result.fold(
                    onSuccess = { dto ->
                        val hint = dto.displayName?.trim().orEmpty().ifEmpty {
                            dto.fullName?.trim().orEmpty().split(Regex("\\s+")).firstOrNull().orEmpty()
                        }
                        s.copy(
                            isLoadingProfile = false,
                            displayNameDraft = hint,
                            profileError = null,
                        )
                    },
                    onFailure = { e ->
                        s.copy(
                            isLoadingProfile = false,
                            profileError = e.message,
                        )
                    },
                )
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayNameDraft = value, displayNameSaveError = null) }
    }

    fun saveDisplayName() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        val raw = _uiState.value.displayNameDraft.trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingDisplayName = true, displayNameSaveError = null)
            }
            val result = runCatching {
                userApi.patchProfile(UserProfilePatchDto(displayName = raw.ifEmpty { "" }))
            }
            _uiState.update { s ->
                result.fold(
                    onSuccess = { dto ->
                        val hint = dto.displayName?.trim().orEmpty().ifEmpty {
                            dto.fullName?.trim().orEmpty().split(Regex("\\s+")).firstOrNull().orEmpty()
                        }
                        s.copy(
                            isSavingDisplayName = false,
                            displayNameDraft = hint,
                            displayNameSaveError = null,
                            snackbarMessage = appContext.getString(R.string.settings_display_name_saved),
                        )
                    },
                    onFailure = { e ->
                        s.copy(
                            isSavingDisplayName = false,
                            displayNameSaveError = e.message,
                        )
                    },
                )
            }
        }
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            val refresh = tokenStorage.getRefreshToken()
            if (!refresh.isNullOrBlank()) {
                runCatching { authApi.logout(LogoutRequestDto(refresh)) }
            }
            tokenStorage.clear()
            familyMeRepository.clear()
            loggedOut.send(Unit)
        }
    }
}
