package com.wellpaid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.data.FamilyMeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val familyMeRepository: FamilyMeRepository,
) : ViewModel() {

    private val loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOutEvents = loggedOut.receiveAsFlow()

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
