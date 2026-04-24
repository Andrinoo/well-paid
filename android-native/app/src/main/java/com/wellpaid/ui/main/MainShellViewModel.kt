package com.wellpaid.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.auth.LogoutRequestDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.data.FamilyMeRepository
import com.wellpaid.data.HomeDashboardCacheRepository
import com.wellpaid.data.MainPrefetchTiming
import com.wellpaid.security.AppSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainShellViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val familyMeRepository: FamilyMeRepository,
    private val appSecurityManager: AppSecurityManager,
    private val prefetchTiming: MainPrefetchTiming,
    private val homeDashboardCache: HomeDashboardCacheRepository,
) : ViewModel() {

    private val loggedOut = Channel<Unit>(Channel.BUFFERED)
    val loggedOutEvents = loggedOut.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(prefetchTiming.familyAfterMainDelayMs)
            familyMeRepository.refresh()
        }
    }

    fun logout() {
        viewModelScope.launch {
            val refresh = tokenStorage.getRefreshToken()
            tokenStorage.clear()
            homeDashboardCache.clear()
            familyMeRepository.clear()
            appSecurityManager.onLoggedOut()
            loggedOut.send(Unit)
            if (!refresh.isNullOrBlank()) {
                launch { runCatching { authApi.logout(LogoutRequestDto(refresh)) } }
            }
        }
    }
}
