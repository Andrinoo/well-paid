package com.wellpaid.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atrasos pequenos entre ecrãs prefetched no [com.wellpaid.ui.main.MainShellScreen],
 * para espalhar picos de rede após o login em vez de N pedidos simultâneos.
 * Valores conservadores; ajustar com base a métricas reais.
 */
@Singleton
class MainPrefetchTiming @Inject constructor() {
    /**
     * Home não usa delay no `init` — a prioridade de rede é a primeira pintura.
     * Demais abas abrem após atrasos maiores (menos contenda com overview/cashflow/announcements).
     */
    val expensesDelayMs: Long = 180L
    val incomesDelayMs: Long = 280L
    val goalsDelayMs: Long = 400L
    val emergencyDelayMs: Long = 480L
    val shoppingDelayMs: Long = 580L

    /** [com.wellpaid.data.FamilyMeRepository] após o arranque do shell (janela pós-Home). */
    val familyAfterMainDelayMs: Long = 120L
}
