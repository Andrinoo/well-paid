package com.wellpaid.ui.session

import androidx.lifecycle.ViewModel
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    tokenStorage: TokenStorage,
) : ViewModel() {

    private val _startRoute = MutableStateFlow(
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            NavRoutes.Login
        } else {
            NavRoutes.Main
        },
    )
    val startRoute: StateFlow<String> = _startRoute.asStateFlow()
}
