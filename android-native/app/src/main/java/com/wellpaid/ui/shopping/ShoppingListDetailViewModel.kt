package com.wellpaid.ui.shopping

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.expense.CategoryDto
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.core.model.shopping.ShoppingListCompleteDto
import com.wellpaid.core.model.shopping.ShoppingListDetailDto
import com.wellpaid.core.model.shopping.ShoppingListGroceryPriceRequestDto
import com.wellpaid.core.model.shopping.ShoppingListItemCreateDto
import com.wellpaid.core.model.shopping.ShoppingListPatchDto
import com.wellpaid.core.network.CategoriesApi
import com.wellpaid.core.network.ShoppingListsApi
import com.wellpaid.core.network.shoppingListItemPatchJson
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class ShoppingListDetailUiState(
    val detail: ShoppingListDetailDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val groceryPriceHits: List<GoalProductHitDto> = emptyList(),
    val groceryPriceSearchLoading: Boolean = false,
)

@HiltViewModel
class ShoppingListDetailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val api: ShoppingListsApi,
    private val categoriesApi: CategoriesApi,
) : ViewModel() {

    private val listId: String = savedStateHandle["listId"] ?: ""

    private var groceryPriceSearchJob: Job? = null
    private val groceryPriceSearchNonce = AtomicInteger(0)

    private val _uiState = MutableStateFlow(ShoppingListDetailUiState())
    val uiState: StateFlow<ShoppingListDetailUiState> = _uiState.asStateFlow()

    init {
        if (listId.isNotBlank()) {
            viewModelScope.launch {
                runCatching { categoriesApi.listCategories() }
                    .onSuccess { list ->
                        _uiState.update {
                            it.copy(categories = list.sortedBy { c -> c.sortOrder })
                        }
                    }
            }
            refresh()
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = appContext.getString(R.string.shopping_error_invalid_list),
                )
            }
        }
    }

    fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    /** Debounced: chamar ao escrever o nome do item (add/edit) para sugestões mercearia. */
    fun onShoppingItemLabelForPriceHints(label: String) {
        groceryPriceSearchJob?.cancel()
        val t = label.trim()
        if (t.length < 2) {
            groceryPriceSearchNonce.incrementAndGet()
            _uiState.update {
                it.copy(groceryPriceHits = emptyList(), groceryPriceSearchLoading = false)
            }
            return
        }
        val id = groceryPriceSearchNonce.incrementAndGet()
        groceryPriceSearchJob = viewModelScope.launch {
            delay(280)
            coroutineContext.ensureActive()
            if (id != groceryPriceSearchNonce.get()) return@launch
            _uiState.update { it.copy(groceryPriceSearchLoading = true) }
            try {
                val resp = api.groceryPriceSuggestions(
                    ShoppingListGroceryPriceRequestDto(query = t, unit = null),
                )
                coroutineContext.ensureActive()
                if (id != groceryPriceSearchNonce.get()) return@launch
                _uiState.update {
                    it.copy(
                        groceryPriceSearchLoading = false,
                        groceryPriceHits = resp.results.take(12),
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(groceryPriceSearchLoading = false) }
                throw e
            } catch (_: Exception) {
                if (id != groceryPriceSearchNonce.get()) return@launch
                _uiState.update {
                    it.copy(
                        groceryPriceSearchLoading = false,
                        groceryPriceHits = emptyList(),
                    )
                }
            }
        }
    }

    fun clearGroceryPriceHints() {
        groceryPriceSearchJob?.cancel()
        groceryPriceSearchNonce.incrementAndGet()
        _uiState.update {
            it.copy(groceryPriceHits = emptyList(), groceryPriceSearchLoading = false)
        }
    }

    fun refresh() {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { api.getShoppingList(listId) }
                .onSuccess { d ->
                    _uiState.update { it.copy(isLoading = false, detail = d, errorMessage = null) }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t)
                                .ifBlank { appContext.getString(R.string.shopping_error_detail) },
                        )
                    }
                }
        }
    }

    fun patchListMetadata(title: String?, storeName: String?, onDone: () -> Unit = {}) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val body = ShoppingListPatchDto(
                title = title?.trim()?.ifEmpty { null },
                storeName = storeName?.trim()?.ifEmpty { null },
            )
            runCatching { api.patchShoppingList(listId, body) }
                .onSuccess { d ->
                    _uiState.update { it.copy(isSaving = false, detail = d) }
                    onDone()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun deleteList(onDeleted: () -> Unit) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { api.deleteShoppingList(listId) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _uiState.update { it.copy(isSaving = false) }
                        onDeleted()
                    } else {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = appContext.getString(R.string.shopping_error_detail),
                            )
                        }
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun addItem(label: String, quantity: Int, lineAmountCents: Int?, onSuccess: (() -> Unit)? = null) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val body = ShoppingListItemCreateDto(
                label = label.trim(),
                quantity = quantity.coerceIn(1, 9999),
                lineAmountCents = lineAmountCents,
            )
            runCatching { api.addShoppingListItem(listId, body) }
                .onSuccess { d ->
                    _uiState.update { it.copy(isSaving = false, detail = d) }
                    onSuccess?.invoke()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun patchItem(
        itemId: String,
        label: String? = null,
        quantity: Int? = null,
        lineAmountCents: Int? = null,
        clearLineAmount: Boolean = false,
    ) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val json = shoppingListItemPatchJson(
                label = label,
                quantity = quantity,
                lineAmountCents = lineAmountCents,
                clearLineAmount = clearLineAmount,
            )
            runCatching { api.patchShoppingListItem(listId, itemId, json) }
                .onSuccess { d -> _uiState.update { it.copy(isSaving = false, detail = d) } }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun removeItem(itemId: String) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { api.deleteShoppingListItem(listId, itemId) }
                .onSuccess { d -> _uiState.update { it.copy(isSaving = false, detail = d) } }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun syncTotalFromLines() {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val body = ShoppingListPatchDto(syncTotalFromLineItems = true)
            runCatching { api.patchShoppingList(listId, body) }
                .onSuccess { d ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            detail = d,
                            infoMessage = appContext.getString(R.string.shopping_total_synced_message),
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun completePurchase(
        categoryId: String,
        expenseDateIso: String,
        totalCents: Int?,
        discountCents: Int?,
        onSuccess: () -> Unit,
    ) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val body = ShoppingListCompleteDto(
                categoryId = categoryId,
                expenseDate = expenseDateIso,
                totalCents = totalCents,
                discountCents = discountCents,
            )
            runCatching { api.completeShoppingList(listId, body) }
                .onSuccess { d ->
                    _uiState.update {
                        it.copy(isSaving = false, detail = d)
                    }
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }
}
