package com.tuapp.eventos.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun setDarkMode(isDarkMode: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
        applyTheme(isDarkMode)
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean("is_dark_mode", false)
    }

    fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun applySavedTheme() {
        applyTheme(isDarkMode())
    }
}