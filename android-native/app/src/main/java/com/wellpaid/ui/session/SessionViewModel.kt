package com.wellpaid.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val dest = if (tokenStorage.getAccessToken().isNullOrBlank()) {
                NavRoutes.Login
            } else {
                NavRoutes.Main
            }
            _startRoute.value = dest
        }
    }
}
