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
    }

    val shoppingAutoGroceryHintsFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.shoppingAutoGroceryHints] ?: true }

    val goalAutoProductSearchFlow: Flow<Boolean> =
        dataStore.data.map { it[Keys.goalAutoProductSearch] ?: true }

    suspend fun setShoppingAutoGroceryHints(value: Boolean) {
        dataStore.edit { it[Keys.shoppingAutoGroceryHints] = value }
    }

    suspend fun setGoalAutoProductSearch(value: Boolean) {
        dataStore.edit { it[Keys.goalAutoProductSearch] = value }
    }
}
