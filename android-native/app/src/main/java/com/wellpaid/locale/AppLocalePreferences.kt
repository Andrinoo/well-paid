package com.wellpaid.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocalePreferences {
    private const val PREFS = "well_paid_prefs"
    private const val KEY_LOCALE = "app_interface_locale"

    /** Persisted: `"pt"` (default) or `"en"`. */
    fun applyStored(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LOCALE)) {
            prefs.edit().putString(KEY_LOCALE, "pt").commit()
        }
        val tag = prefs.getString(KEY_LOCALE, "pt") ?: "pt"
        val locales = if (tag.equals("en", ignoreCase = true)) {
            LocaleListCompat.forLanguageTags("en-US")
        } else {
            LocaleListCompat.forLanguageTags("pt-BR")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun interfaceLocaleTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, "pt") ?: "pt"

    fun isEnglish(context: Context): Boolean =
        interfaceLocaleTag(context).equals("en", ignoreCase = true)

    /**
     * What the UI is actually using: [AppCompatDelegate] override if set, otherwise SharedPreferences.
     * (When the delegate list was still empty, the app followed the **system** locale while prefs
     * defaulted to `"pt"`, so Settings showed Portuguese but strings were English.)
     */
    fun isEnglishInterface(context: Context): Boolean {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            val lang = locales[0]?.language ?: return isEnglish(context)
            return lang.equals("en", ignoreCase = true)
        }
        return isEnglish(context)
    }

    fun setAndApply(context: Context, useEnglish: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LOCALE, if (useEnglish) "en" else "pt")
            .commit()
        applyStored(context)
    }
}
