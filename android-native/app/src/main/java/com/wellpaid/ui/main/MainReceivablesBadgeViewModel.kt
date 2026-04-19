package com.wellpaid.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.model.receivable.ReceivableDto
import com.wellpaid.core.network.ReceivablesApi
import com.wellpaid.ui.expenses.DueUrgency
import com.wellpaid.ui.expenses.daysUntilDue
import com.wellpaid.ui.expenses.dueUrgencyForDays
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainReceivablesBadgeState(
    val isLoading: Boolean = true,
    /** Receivables abertos (família) — soma de a receber + a pagar. */
    val openCount: Int = 0,
    /** Urgência mais alta entre os vencimentos (alinhado a despesas). */
    val worstUrgency: DueUrgency? = null,
    val hadError: Boolean = false,
) {
    val showInBottomBar: Boolean get() = !isLoading && openCount > 0
}

@HiltViewModel
class MainReceivablesBadgeViewModel @Inject constructor(
    private val receivablesApi: ReceivablesApi,
) : ViewModel() {

    private val _state = MutableStateFlow(MainReceivablesBadgeState())
    val state: StateFlow<MainReceivablesBadgeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hadError = false) }
            runCatching {
                val bucket = receivablesApi.listReceivables()
                val all = bucket.asCreditor + bucket.asDebtor
                val worst = worstUrgencyAmong(all)
                _state.update {
                    MainReceivablesBadgeState(
                        isLoading = false,
                        openCount = all.size,
                        worstUrgency = worst,
                        hadError = false,
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        isLoading = false,
                        openCount = 0,
                        worstUrgency = null,
                        hadError = true,
                    )
                }
            }
        }
    }

    private fun worstUrgencyAmong(rows: List<ReceivableDto>): DueUrgency? {
        if (rows.isEmpty()) return null
        val urgencies = rows.mapNotNull { row ->
            parseSettleLocalDate(row.settleBy)?.let { d ->
                dueUrgencyForDays(daysUntilDue(d))
            }
        }
        if (urgencies.isEmpty()) return DueUrgency.Safe
        return urgencies.minBy { urgencyRank(it) }
    }

    private fun urgencyRank(u: DueUrgency): Int = when (u) {
        DueUrgency.Overdue -> 0
        DueUrgency.DueToday -> 1
        DueUrgency.DueSoon -> 2
        DueUrgency.Upcoming -> 3
        DueUrgency.Safe -> 4
    }

    private fun parseSettleLocalDate(iso: String): LocalDate? =
        try {
            LocalDate.parse(iso.trim().take(10))
        } catch (_: DateTimeParseException) {
            null
        }
}
