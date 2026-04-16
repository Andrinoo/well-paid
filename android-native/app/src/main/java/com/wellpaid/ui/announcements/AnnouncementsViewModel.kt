package com.wellpaid.ui.announcements

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.announcement.AnnouncementDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.AnnouncementsApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnnouncementsUiState(
    val isLoading: Boolean = true,
    val items: List<AnnouncementDto> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class AnnouncementsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val announcementsApi: AnnouncementsApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnnouncementsUiState())
    val uiState: StateFlow<AnnouncementsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val token = tokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                AnnouncementsUiState(
                    isLoading = false,
                    items = emptyList(),
                    errorMessage = appContext.getString(R.string.announcements_need_login),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val banner = async { runCatching { announcementsApi.listActive("home_banner", 50) } }
            val feed = async { runCatching { announcementsApi.listActive("home_feed", 50) } }
            val finance = async { runCatching { announcementsApi.listActive("finance_tab", 50) } }
            val announcementsOnly =
                async { runCatching { announcementsApi.listActive("announcements_tab", 50) } }
            val bannerResult = banner.await()
            val feedResult = feed.await()
            val financeResult = finance.await()
            val announcementsTabResult = announcementsOnly.await()
            val merged = LinkedHashMap<String, AnnouncementDto>()
            listOf(bannerResult, feedResult, financeResult, announcementsTabResult).forEach { result ->
                result.getOrNull()?.items?.forEach { row -> merged[row.id] = row }
            }
            val sorted = merged.values.sortedWith(
                compareByDescending<AnnouncementDto> { it.priority }
                    .thenByDescending { it.createdAt.orEmpty() },
            )
            val errors = listOf(bannerResult, feedResult, financeResult, announcementsTabResult)
                .mapNotNull { it.exceptionOrNull() }
            val errorMessage = when {
                sorted.isNotEmpty() -> null
                errors.isNotEmpty() -> FastApiErrorMapper.message(appContext, errors.first())
                else -> null
            }
            _uiState.update {
                AnnouncementsUiState(
                    isLoading = false,
                    items = sorted,
                    errorMessage = errorMessage,
                )
            }
        }
    }
}
