package com.wellpaid.ui.shopping

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.shopping.ShoppingListCreateDto
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

data class ShoppingListFormUiState(
    val title: String = "",
    val storeName: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ShoppingListFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: ShoppingListsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListFormUiState())
    val uiState: StateFlow<ShoppingListFormUiState> = _uiState.asStateFlow()

    fun setTitle(v: String) {
        _uiState.update { it.copy(title = v) }
    }

    fun setStoreName(v: String) {
        _uiState.update { it.copy(storeName = v) }
    }

    fun save(onSuccess: (String) -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val body = ShoppingListCreateDto(
                title = s.title.trim().ifEmpty { null },
                storeName = s.storeName.trim().ifEmpty { null },
            )
            runCatching { api.createShoppingList(body) }
                .onSuccess { detail ->
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess(detail.id)
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
