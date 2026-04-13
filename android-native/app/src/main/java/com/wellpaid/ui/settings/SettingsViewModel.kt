package com.wellpaid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.UserMeDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.core.network.UserApi
import com.wellpaid.data.FamilyMeRepository
import com.wellpaid.security.AppSecurityManager
import com.wellpaid.util.looksLikeUuid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val userFirstName: String? = null,
    val userEmail: String? = null,
    val profileLoaded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val familyMeRepository: FamilyMeRepository,
    private val appSecurityManager: AppSecurityManager,
    private val userApi: UserApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOutEvents = loggedOut.receiveAsFlow()

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
