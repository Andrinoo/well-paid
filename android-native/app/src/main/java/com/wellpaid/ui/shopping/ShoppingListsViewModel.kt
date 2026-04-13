package com.wellpaid.ui.shopping

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.shopping.ShoppingListCreateDto
import com.wellpaid.core.model.shopping.ShoppingListSummaryDto
import com.wellpaid.core.network.ShoppingListsApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingListsUiState(
    val lists: List<ShoppingListSummaryDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class ShoppingListsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: ShoppingListsApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListsUiState())
    val uiState: StateFlow<ShoppingListsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun createEmptyList(onSuccess: (String) -> Unit) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        viewModelScope.launch {
            runCatching { api.createShoppingList(ShoppingListCreateDto()) }
                .onSuccess { detail ->
                    refresh()
                    onSuccess(detail.id)
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            errorMessage = FastApiErrorMapper.message(appContext, t)
                                .ifBlank { appContext.getString(R.string.shopping_error_load) },
                        )
                    }
                }
        }
    }

    fun refresh() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(isLoading = false, lists = emptyList(), errorMessage = null)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { api.listShoppingLists() }
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, lists = list, errorMessage = null) }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t)
                                .ifBlank { appContext.getString(R.string.shopping_error_load) },
                        )
                    }
                }
        }
    }
}
