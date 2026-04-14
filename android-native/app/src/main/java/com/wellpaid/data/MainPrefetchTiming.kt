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
    val expensesDelayMs: Long = 20L
    val incomesDelayMs: Long = 45L
    val goalsDelayMs: Long = 70L
    val emergencyDelayMs: Long = 95L
    val shoppingDelayMs: Long = 120L

    /** [com.wellpaid.data.FamilyMeRepository] após o arranque do dashboard (user/overview). */
    val familyAfterMainDelayMs: Long = 40L
}
