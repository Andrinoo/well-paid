package com.wellpaid.ui.goals

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalCreateDto
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.model.goal.GoalPreviewFromUrlRequestDto
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.core.model.goal.GoalProductSearchRequestDto
import com.wellpaid.core.model.goal.GoalUpdateDto
import com.wellpaid.core.network.GoalsApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.SearchQueryUrlParser
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatMinorCurrencyFromCents
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class GoalFormUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isRefreshingPrice: Boolean = false,
    val isSearchingProducts: Boolean = false,
    val loaded: GoalDto? = null,
    val title: String = "",
    val targetText: String = "",
    val initialText: String = "",
    val targetUrl: String = "",
    val isActive: Boolean = true,
    val referencePriceLabel: String? = null,
    /** Preenchido ao criar meta (sem `loaded`) a partir da pesquisa ou do preview por URL. */
    val draftReferenceProductName: String? = null,
    val draftReferencePriceCents: Int? = null,
    val draftReferenceCurrency: String = "BRL",
    val draftPriceSource: String? = null,
    val productSearchResults: List<GoalProductHitDto> = emptyList(),
    /** True após uma pesquisa concluída sem resultados (SerpAPI / Google Shopping). */
    val lastProductSearchHadNoResults: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
)

@HiltViewModel
class GoalFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val goalsApi: GoalsApi,
) : ViewModel() {

    companion object {
        /** Máximo de resultados no seletor (pesquisa Google Shopping via servidor). */
        const val PRODUCT_SEARCH_MAX_RESULTS: Int = 5
    }

    private val goalId: String? = savedStateHandle.get<String>("goalId")

    private var titleSearchDebounceJob: Job? = null
    private var urlSearchDebounceJob: Job? = null
    private val titleSearchNonce = AtomicInteger(0)

    private val _uiState = MutableStateFlow(GoalFormUiState())
    val uiState: StateFlow<GoalFormUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = goalId != null

    init {
        val id = goalId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                runCatching { goalsApi.getGoal(id) }
                    .onSuccess { g ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loaded = g,
                                title = g.title,
                                targetText = centsToBrlInput(g.targetCents),
                                targetUrl = g.targetUrl.orEmpty(),
                                isActive = g.isActive,
                                referencePriceLabel = g.referencePriceCents?.let { c ->
                                    formatBrlFromCents(c)
                                },
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { t ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = FastApiErrorMapper.message(appContext, t),
                            )
                        }
                    }
            }
        }
    }

    fun setTitle(value: String) {
        _uiState.update { it.copy(title = value) }
        scheduleProductSearchFromTitle(value)
    }

    /** Pesquisa automática (Google Shopping via SerpAPI no backend) ao escrever o título. */
    private fun scheduleProductSearchFromTitle(titleInput: String) {
        titleSearchDebounceJob?.cancel()
        val t = titleInput.trim()
        if (t.length < 2) {
            titleSearchNonce.incrementAndGet()
            _uiState.update {
                it.copy(
                    productSearchResults = emptyList(),
                    lastProductSearchHadNoResults = false,
                    isSearchingProducts = false,
                )
            }
            return
        }
        val id = titleSearchNonce.incrementAndGet()
        titleSearchDebounceJob = viewModelScope.launch {
            delay(420)
            coroutineContext.ensureActive()
            if (id != titleSearchNonce.get()) return@launch
            performProductSearch(query = t, syncTitleFromQuery = false)
        }
    }

    fun setTargetText(value: String) {
        _uiState.update { it.copy(targetText = value) }
    }

    fun setInitialText(value: String) {
        _uiState.update { it.copy(initialText = value) }
    }

    fun setActive(value: Boolean) {
        _uiState.update { it.copy(isActive = value) }
    }

    /**
     * Guarda o URL. Se for link de pesquisa com `q=` (ou equivalente), dispara a mesma pesquisa (debounce).
     * URLs só de produto não disparam pesquisa — servem para o botão de preview de preço.
     */
    fun onTargetUrlChange(value: String) {
        _uiState.update { it.copy(targetUrl = value) }
        urlSearchDebounceJob?.cancel()
        val q = SearchQueryUrlParser.extractSearchQuery(value) ?: return
        if (q.length < 2) return
        val snapshotUrl = value.trim()
        urlSearchDebounceJob = viewModelScope.launch {
            delay(550)
            if (_uiState.value.targetUrl.trim() != snapshotUrl) return@launch
            val syncTitle = _uiState.value.title.isBlank()
            performProductSearch(query = q, syncTitleFromQuery = syncTitle)
        }
    }

    fun loadPriceFromLink() {
        if (isEditMode) {
            refreshReferencePrice()
        } else {
            previewFromUrl()
        }
    }

    private fun previewFromUrl() {
        val url = _uiState.value.targetUrl.trim()
        if (url.isEmpty()) {
            val titleQ = _uiState.value.title.trim()
            if (titleQ.length >= 2) {
                viewModelScope.launch {
                    performProductSearch(query = titleQ, syncTitleFromQuery = false)
                }
                return
            }
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.goal_error_query_title_or_search_url))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPrice = true, errorMessage = null) }
            runCatching {
                goalsApi.previewFromUrl(GoalPreviewFromUrlRequestDto(url = url))
            }
                .onSuccess { p ->
                    val suggested = p.suggestedTargetCents
                    val name = p.referenceProductName
                    _uiState.update { s ->
                        var nextTitle = s.title
                        if (nextTitle.isBlank() && !name.isNullOrBlank()) {
                            nextTitle = name.take(200)
                        }
                        s.copy(
                            isRefreshingPrice = false,
                            title = nextTitle,
                            targetText = suggested?.let { centsToBrlInput(it) } ?: s.targetText,
                            referencePriceLabel = p.referencePriceCents?.let { c -> formatBrlFromCents(c) }
                                ?: s.referencePriceLabel,
                            draftReferenceProductName = name?.take(200) ?: s.draftReferenceProductName,
                            draftReferencePriceCents = p.referencePriceCents ?: s.draftReferencePriceCents,
                            draftReferenceCurrency = p.referenceCurrency,
                            draftPriceSource = p.priceSource ?: s.draftPriceSource,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshingPrice = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun refreshReferencePrice() {
        val id = goalId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPrice = true, errorMessage = null) }
            runCatching { goalsApi.refreshReferencePrice(id) }
                .onSuccess { g ->
                    _uiState.update {
                        it.copy(
                            isRefreshingPrice = false,
                            loaded = g,
                            targetText = centsToBrlInput(g.targetCents),
                            targetUrl = g.targetUrl.orEmpty(),
                            referencePriceLabel = g.referencePriceCents?.let { c -> formatBrlFromCents(c) },
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshingPrice = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    /** Pesquisa manual: título da meta ou termo extraído de um URL de pesquisa no campo de link. */
    fun refreshProductSearch() {
        val titleQ = _uiState.value.title.trim()
        val urlQ = SearchQueryUrlParser.extractSearchQuery(_uiState.value.targetUrl)?.trim()
        val q = when {
            titleQ.length >= 2 -> titleQ
            urlQ != null && urlQ.length >= 2 -> urlQ
            else -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.goal_error_query_title_or_search_url))
                }
                return
            }
        }
        viewModelScope.launch {
            performProductSearch(query = q, syncTitleFromQuery = false)
        }
    }

    private suspend fun performProductSearch(query: String, syncTitleFromQuery: Boolean) {
        val q = query.trim()
        if (q.length < 2) return
        _uiState.update { s ->
            s.copy(
                title = if (syncTitleFromQuery) q.take(200) else s.title,
                isSearchingProducts = true,
                errorMessage = null,
                lastProductSearchHadNoResults = false,
            )
        }
        try {
            val results = goalsApi.productSearch(GoalProductSearchRequestDto(query = q)).results
            coroutineContext.ensureActive()
            val capped = results.take(PRODUCT_SEARCH_MAX_RESULTS)
            _uiState.update { s ->
                s.copy(
                    isSearchingProducts = false,
                    productSearchResults = capped,
                    lastProductSearchHadNoResults = capped.isEmpty(),
                    errorMessage = null,
                )
            }
        } catch (e: CancellationException) {
            _uiState.update { it.copy(isSearchingProducts = false) }
            throw e
        } catch (t: Exception) {
            _uiState.update {
                it.copy(
                    isSearchingProducts = false,
                    productSearchResults = emptyList(),
                    lastProductSearchHadNoResults = true,
                    errorMessage = FastApiErrorMapper.message(appContext, t),
                )
            }
        }
    }

    /** Aplica preço e link ao formulário mantendo o título da meta (nome que escreveste). */
    fun applyProductListing(hit: GoalProductHitDto) {
        _uiState.update { applyHitToState(it, hit, preserveUserTitle = true) }
    }

    /** Preenche valor, link e referência a partir de um anúncio (só na app, sem navegador). */
    private fun applyHitToState(
        s: GoalFormUiState,
        hit: GoalProductHitDto,
        preserveUserTitle: Boolean,
    ): GoalFormUiState {
        val isBrl = hit.currencyId.equals("BRL", ignoreCase = true)
        val refLabel = formatMinorCurrencyFromCents(hit.priceCents, hit.currencyId)
        val nextLoaded = s.loaded?.copy(
            referenceProductName = hit.title.take(200),
            referencePriceCents = if (isBrl) hit.priceCents else null,
            referenceCurrency = hit.currencyId,
            priceSource = hit.source,
            targetUrl = hit.url,
        )
        return s.copy(
            title = if (preserveUserTitle) s.title else hit.title.take(200),
            targetUrl = hit.url,
            targetText = if (isBrl) centsToBrlInput(hit.priceCents) else s.targetText,
            referencePriceLabel = refLabel,
            draftReferenceProductName = hit.title.take(200),
            draftReferencePriceCents = if (isBrl) hit.priceCents else null,
            draftReferenceCurrency = hit.currencyId,
            draftPriceSource = hit.source,
            lastProductSearchHadNoResults = false,
            errorMessage = null,
            loaded = nextLoaded ?: s.loaded,
        )
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun requestDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun canDelete(): Boolean {
        val g = _uiState.value.loaded ?: return false
        return g.isMine && g.currentCents == 0
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        val title = s.title.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.goal_error_title)) }
            return
        }
        val target = parseBrlToCents(s.targetText)
        if (target == null || target <= 0) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.goal_error_target)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val initialForCreate: Int? = if (goalId == null) {
                if (s.initialText.isBlank()) {
                    0
                } else {
                    parseBrlToCents(s.initialText) ?: run {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = appContext.getString(R.string.goal_error_initial),
                            )
                        }
                        return@launch
                    }
                }
            } else {
                null
            }
            val result = runCatching {
                if (goalId == null) {
                    val url = s.targetUrl.trim().takeIf { it.isNotEmpty() }
                    goalsApi.createGoal(
                        GoalCreateDto(
                            title = title,
                            targetCents = target,
                            currentCents = initialForCreate!!,
                            isActive = s.isActive,
                            targetUrl = url,
                            referenceProductName = s.draftReferenceProductName,
                            referencePriceCents = s.draftReferencePriceCents,
                            referenceCurrency = s.draftReferenceCurrency,
                            priceSource = s.draftPriceSource,
                        ),
                    )
                } else {
                    val loaded = s.loaded ?: error("missing")
                    val url = s.targetUrl.trim().takeIf { it.isNotEmpty() }
                    goalsApi.updateGoal(
                        goalId,
                        GoalUpdateDto(
                            title = title,
                            targetCents = target,
                            currentCents = loaded.currentCents,
                            isActive = s.isActive,
                            targetUrl = url,
                            referenceProductName = loaded.referenceProductName,
                            referencePriceCents = loaded.referencePriceCents,
                            referenceCurrency = loaded.referenceCurrency,
                            priceSource = loaded.priceSource,
                        ),
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val id = goalId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showDeleteConfirm = false) }
            runCatching {
                val resp = goalsApi.deleteGoal(id)
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code()}")
                }
            }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
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
