package ar.matias.listmaster.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

class AppPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "list_master_settings",
        Context.MODE_PRIVATE,
    )

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = preferences.getString(KEY_THEME, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
