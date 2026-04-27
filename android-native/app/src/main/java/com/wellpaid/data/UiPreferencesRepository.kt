package com.wellpaid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiPreferencesDataStore by preferencesDataStore(name = "ui_prefs")

/**
 * Preferências de UI não sensíveis (pesquisa automática de preços, etc.).
 */
@Singleton
class UiPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.uiPreferencesDataStore

    private object Keys {
        val shoppingAutoGroceryHints = booleanPreferencesKey("shopping_auto_grocery_hints")
        val goalAutoProductSearch = booleanPreferencesKey("goal_auto_product_search")
        val keypadHapticsEnabled = booleanPreferencesKey("keypad_haptics_enabled")
        /** Só pede período em meses no plano de reserva; data-alvo fim é derivada e o campo fica oculto. */
        val emergencyPlanHideTargetEndDate = booleanPreferencesKey("emergency_plan_hide_target_end")
        /** Se true, permite capturas de ecrã (remove FLAG_SECURE). Por defeito false = bloqueio ativo. */
        val screenshotsAllowed = booleanPreferencesKey("screenshots_allowed")
        /** Última escolha do toggle "Informar investimentos" no fluxo de reserva de emergência. */
        val emergencyInformInvestments = booleanPreferencesKey("emergency_inform_investments")
    }

    val shoppingAutoGroceryHintsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.shoppingAutoGroceryHints] ?: true }

    val goalAutoProductSearchFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.goalAutoProductSearch] ?: true }

    val keypadHapticsEnabledFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.keypadHapticsEnabled] ?: true }

    val emergencyPlanHideTargetEndFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.emergencyPlanHideTargetEndDate] ?: false }

    val screenshotsAllowedFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.screenshotsAllowed] ?: false }

    val emergencyInformInvestmentsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.emergencyInformInvestments] ?: false }

    suspend fun setShoppingAutoGroceryHints(value: Boolean) {
        dataStore.edit { it[Keys.shoppingAutoGroceryHints] = value }
    }

    suspend fun setGoalAutoProductSearch(value: Boolean) {
        dataStore.edit { it[Keys.goalAutoProductSearch] = value }
    }

    suspend fun setKeypadHapticsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.keypadHapticsEnabled] = value }
    }

    suspend fun setEmergencyPlanHideTargetEndDate(value: Boolean) {
        dataStore.edit { it[Keys.emergencyPlanHideTargetEndDate] = value }
    }

    suspend fun setScreenshotsAllowed(value: Boolean) {
        dataStore.edit { it[Keys.screenshotsAllowed] = value }
    }

    suspend fun setEmergencyInformInvestments(value: Boolean) {
        dataStore.edit { it[Keys.emergencyInformInvestments] = value }
    }
}
