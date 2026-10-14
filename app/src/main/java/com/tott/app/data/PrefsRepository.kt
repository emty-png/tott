package com.tott.app.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lightweight prefs store (no extra deps). Backs onboarding + UI customizations. */
@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext ctx: Context
) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("tott", Context.MODE_PRIVATE)

    private val _firstRun = MutableStateFlow(sp.getBoolean("first_run", true))
    val firstRun: StateFlow<Boolean> = _firstRun.asStateFlow()

    private val _themeMode = MutableStateFlow(sp.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _amoled = MutableStateFlow(sp.getBoolean("amoled", false))
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _dynamic = MutableStateFlow(sp.getBoolean("dynamic", false))
    val dynamic: StateFlow<Boolean> = _dynamic.asStateFlow()

    private val _accent = MutableStateFlow(sp.getLong("accent", 0xFF6750A4))
    val accent: StateFlow<Long> = _accent.asStateFlow()

    private val _fontScale = MutableStateFlow(sp.getFloat("font_scale", 1f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _compact = MutableStateFlow(sp.getBoolean("compact", false))
    val compact: StateFlow<Boolean> = _compact.asStateFlow()

    private val _reduceMotion = MutableStateFlow(sp.getBoolean("reduce_motion", false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    fun completeOnboarding() {
        sp.edit().putBoolean("first_run", false).apply()
        _firstRun.value = false
    }

    fun setThemeMode(v: String) { sp.edit().putString("theme_mode", v).apply(); _themeMode.value = v }
    fun setAmoled(v: Boolean) { sp.edit().putBoolean("amoled", v).apply(); _amoled.value = v }
    fun setDynamic(v: Boolean) { sp.edit().putBoolean("dynamic", v).apply(); _dynamic.value = v }
    fun setAccent(argb: Long) { sp.edit().putLong("accent", argb).apply(); _accent.value = argb }
    fun setFontScale(v: Float) { sp.edit().putFloat("font_scale", v).apply(); _fontScale.value = v }
    fun setCompact(v: Boolean) { sp.edit().putBoolean("compact", v).apply(); _compact.value = v }
    fun setReduceMotion(v: Boolean) { sp.edit().putBoolean("reduce_motion", v).apply(); _reduceMotion.value = v }
}
