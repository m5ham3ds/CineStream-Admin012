package com.example.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ARABIC("ar", "العربية", "العربية"),
    ENGLISH("en", "English", "English");

    val layoutDirection: LayoutDirection
        get() = if (this == ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
}

object AppSettings {
    private const val PREFS_NAME = "cinestream_admin_prefs"
    private const val KEY_DARK_THEME = "key_dark_theme"
    private const val KEY_LANGUAGE = "key_language"
    private const val KEY_NOTIFICATIONS = "key_notifications"
    private const val KEY_SOUND = "key_sound"
    private const val KEY_VIBRATION = "key_vibration"

    private var prefs: SharedPreferences? = null

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ARABIC)
    val language = _language.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _isDarkTheme.value = sp.getBoolean(KEY_DARK_THEME, true)
        val langCode = sp.getString(KEY_LANGUAGE, "ar") ?: "ar"
        _language.value = if (langCode == "en") AppLanguage.ENGLISH else AppLanguage.ARABIC
        _notificationsEnabled.value = sp.getBoolean(KEY_NOTIFICATIONS, true)
        _soundEnabled.value = sp.getBoolean(KEY_SOUND, true)
        _vibrationEnabled.value = sp.getBoolean(KEY_VIBRATION, true)
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_THEME, enabled)?.apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs?.edit()?.putString(KEY_LANGUAGE, lang.code)?.apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_NOTIFICATIONS, enabled)?.apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_SOUND, enabled)?.apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_VIBRATION, enabled)?.apply()
    }
}
