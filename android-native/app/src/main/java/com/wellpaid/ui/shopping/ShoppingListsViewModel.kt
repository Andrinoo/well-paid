package com.wellpaid.ui.shopping

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.shopping.ShoppingListCreateDto
import com.wellpaid.core.model.shopping.ShoppingListDetailDto
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
    val isRefreshing: Boolean = false,
    val isCreatingList: Boolean = false,
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

    /**
     * Creates a list with a non-blank [title]. Callers must validate; this method trims and no-ops if empty.
     */
    fun createListWithTitle(title: String, onSuccess: (String) -> Unit) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || tokenStorage.getAccessToken().isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingList = true, errorMessage = null) }
            runCatching { api.createShoppingList(ShoppingListCreateDto(title = trimmed)) }
                .onSuccess { detail ->
                    _uiState.update { state ->
                        val summary = detail.toSummary()
                        val merged = listOf(summary) + state.lists.filter { it.id != summary.id }
                        state.copy(
                            isCreatingList = false,
                            lists = merged,
                            errorMessage = null,
                        )
                    }
                    onSuccess(detail.id)
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isCreatingList = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t)
                                .ifBlank { appContext.getString(R.string.shopping_error_load) },
                        )
                    }
                }
        }
    }

    fun deleteList(id: String) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        val snapshot = _uiState.value.lists
        _uiState.update {
            it.copy(lists = it.lists.filter { row -> row.id != id }, errorMessage = null)
        }
        viewModelScope.launch {
            runCatching { api.deleteShoppingList(id) }
                .onSuccess { resp ->
                    if (!resp.isSuccessful) {
                        _uiState.update {
                            it.copy(
                                lists = snapshot,
                                errorMessage = appContext.getString(R.string.shopping_error_load),
                            )
                        }
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            lists = snapshot,
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
                it.copy(isLoading = false, isRefreshing = false, lists = emptyList(), errorMessage = null)
            }
            return
        }
        viewModelScope.launch {
            val silent = _uiState.value.lists.isNotEmpty()
            _uiState.update {
                if (silent) {
                    it.copy(isRefreshing = true, errorMessage = null)
                } else {
                    it.copy(isLoading = true, errorMessage = null)
                }
            }
            runCatching { api.listShoppingLists() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            lists = list,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t)
                                .ifBlank { appContext.getString(R.string.shopping_error_load) },
                        )
                    }
                }
        }
    }
}

private fun ShoppingListDetailDto.toSummary() = ShoppingListSummaryDto(
    id = id,
    ownerUserId = ownerUserId,
    isMine = isMine,
    title = title,
    storeName = storeName,
    status = status,
    completedAt = completedAt,
    expenseId = expenseId,
    totalCents = totalCents,
    itemsCount = items.size,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
