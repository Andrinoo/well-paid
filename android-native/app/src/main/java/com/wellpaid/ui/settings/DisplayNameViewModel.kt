package com.wellpaid.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.auth.UserProfilePatchDto
import com.wellpaid.core.network.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DisplayNameUiState(
    val draft: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
)

@HiltViewModel
class DisplayNameViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userApi: UserApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisplayNameUiState())
    val uiState: StateFlow<DisplayNameUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val r = runCatching { userApi.getCurrentUser() }
            _uiState.update { s ->
                r.fold(
                    onSuccess = { dto ->
                        val draft = dto.displayName?.trim().orEmpty().ifEmpty {
                            dto.fullName?.trim().orEmpty().split(Regex("\\s+")).firstOrNull().orEmpty()
                        }
                        s.copy(isLoading = false, draft = draft, loadError = null)
                    },
                    onFailure = { e ->
                        s.copy(isLoading = false, loadError = e.message)
                    },
                )
            }
        }
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value, saveError = null) }
    }

    fun save(onSuccess: () -> Unit) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        val raw = _uiState.value.draft.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            val r = runCatching {
                userApi.updateDisplayName(UserProfilePatchDto(displayName = raw))
            }
            r.fold(
                onSuccess = { dto ->
                    val draft = dto.displayName?.trim().orEmpty().ifEmpty {
                        dto.fullName?.trim().orEmpty().split(Regex("\\s+")).firstOrNull().orEmpty()
                    }
                    _uiState.update {
                        it.copy(isSaving = false, draft = draft, saveError = null)
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = e.message
                                ?: appContext.getString(R.string.settings_display_name_save_error),
                        )
                    }
                },
            )
        }
    }
}
