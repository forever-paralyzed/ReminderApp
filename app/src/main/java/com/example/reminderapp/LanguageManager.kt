
package com.example.reminderapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun applyLanguage(context: Context) {
        val language = getLanguage(context)
        if (language.isNotEmpty()) {
            val appLocale = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun setLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "") ?: ""
    }
}
