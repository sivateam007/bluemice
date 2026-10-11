package com.example.bluemice

import android.content.Context
import android.content.SharedPreferences
import com.example.bluemice.model.toJson
import java.util.Locale

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bluemice_settings", Context.MODE_PRIVATE)

    var pointerSpeed: Float
        get() = prefs.getFloat("pointer_speed", 1.5f)
        set(v) = prefs.edit().putFloat("pointer_speed", v).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat("scroll_speed", 1f)
        set(v) = prefs.edit().putFloat("scroll_speed", v).apply()

    var invertScroll: Boolean
        get() = prefs.getBoolean("invert_scroll", false)
        set(v) = prefs.edit().putBoolean("invert_scroll", v).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(v) = prefs.edit().putBoolean("haptics", v).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", false)
        set(v) = prefs.edit().putBoolean("keep_screen_on", v).apply()

    var overLockScreen: Boolean
        get() = prefs.getBoolean("over_lock_screen", false)
        set(v) = prefs.edit().putBoolean("over_lock_screen", v).apply()

    var fullscreen: Boolean
        get() = prefs.getBoolean("fullscreen", false)
        set(v) = prefs.edit().putBoolean("fullscreen", v).apply()

    var airMouseEnabled: Boolean
        get() = prefs.getBoolean("air_mouse", false)
        set(v) = prefs.edit().putBoolean("air_mouse", v).apply()

    var airMouseSensitivity: Float
        get() = prefs.getFloat("air_mouse_sensitivity", 1f)
        set(v) = prefs.edit().putFloat("air_mouse_sensitivity", v).apply()

    var mouseJiggleMode: String
        get() = prefs.getString("mouse_jiggle_mode", "disabled") ?: "disabled"
        set(v) = prefs.edit().putString("mouse_jiggle_mode", v).apply()

    var jigglerIntervalSeconds: Int
        get() = prefs.getInt("jiggler_interval", 30)
        set(v) = prefs.edit().putInt("jiggler_interval", v.coerceIn(5, 600)).apply()

    var penMode: Boolean
        get() = prefs.getBoolean("pen_mode", false)
        set(v) = prefs.edit().putBoolean("pen_mode", v).apply()

    var tapToClick: Boolean
        get() = prefs.getBoolean("tap_to_click", true)
        set(v) = prefs.edit().putBoolean("tap_to_click", v).apply()

    var showMouseButtonsAbove: Boolean
        get() = prefs.getBoolean("show_mouse_buttons_above", false)
        set(v) = prefs.edit().putBoolean("show_mouse_buttons_above", v).apply()

    var visibleMouseButtons: List<String>
        get() = (prefs.getString("visible_mouse_buttons", "left,right") ?: "left,right")
            .split(",").filter { it.isNotEmpty() }
        set(v) = prefs.edit().putString("visible_mouse_buttons", v.joinToString(",")).apply()

    var scrollBarLeft: Boolean
        get() = prefs.getBoolean("scroll_bar_left", false)
        set(v) = prefs.edit().putBoolean("scroll_bar_left", v).apply()

    var showScrollBar: Boolean
        get() = prefs.getBoolean("show_scroll_bar", true)
        set(v) = prefs.edit().putBoolean("show_scroll_bar", v).apply()

    var tapSuppression: Boolean
        get() = prefs.getBoolean("tap_suppression", false)
        set(v) = prefs.edit().putBoolean("tap_suppression", v).apply()

    var showMediaButtons: Boolean
        get() = prefs.getBoolean("show_media_buttons", true)
        set(v) = prefs.edit().putBoolean("show_media_buttons", v).apply()

    var volUpButton: String
        get() = prefs.getString("vol_up_button", "system") ?: "system"
        set(v) = prefs.edit().putString("vol_up_button", v).apply()

    var volDownButton: String
        get() = prefs.getString("vol_down_button", "system") ?: "system"
        set(v) = prefs.edit().putString("vol_down_button", v).apply()

    var scannerSendEnter: Boolean
        get() = prefs.getBoolean("scanner_send_enter", false)
        set(v) = prefs.edit().putBoolean("scanner_send_enter", v).apply()

    var scannerContinuous: Boolean
        get() = prefs.getBoolean("scanner_continuous", false)
        set(v) = prefs.edit().putBoolean("scanner_continuous", v).apply()

    var scannerSkipDuplicate: Boolean
        get() = prefs.getBoolean("scanner_skip_duplicate", false)
        set(v) = prefs.edit().putBoolean("scanner_skip_duplicate", v).apply()

    var autoConnect: Boolean
        get() = prefs.getBoolean("auto_connect", false)
        set(v) = prefs.edit().putBoolean("auto_connect", v).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(v) = prefs.edit().putBoolean("dark_mode", v).apply()

    var themeColor: String
        get() = prefs.getString("theme_color", "default") ?: "default"
        set(v) = prefs.edit().putString("theme_color", v).apply()

    var outlineTheme: Boolean
        get() = prefs.getBoolean("outline_theme", false)
        set(v) = prefs.edit().putBoolean("outline_theme", v).apply()

    var screenBrightness: Float
        get() = prefs.getFloat("screen_brightness", 1f)
        set(v) = prefs.edit().putFloat("screen_brightness", v.coerceIn(0.1f, 1f)).apply()

    var pagerSwipeLocked: Boolean
        get() = prefs.getBoolean("pager_swipe_locked", false)
        set(v) = prefs.edit().putBoolean("pager_swipe_locked", v).apply()

    var landscape: Boolean
        get() = prefs.getBoolean("landscape", false)
        set(v) = prefs.edit().putBoolean("landscape", v).apply()

    /**
     * Gamepad remap + stick/trigger tuning, stored as one JSON blob.
     * Parsed leniently so a config written by an older build still loads.
     */
    var gamepadConfig: com.example.bluemice.model.GamepadConfig
        get() = runCatching {
            val raw = prefs.getString("gamepad_config", null) ?: return com.example.bluemice.model.GamepadConfig()
            com.example.bluemice.model.gamepadConfigFromJson(org.json.JSONObject(raw))
        }.getOrDefault(com.example.bluemice.model.GamepadConfig())
        set(v) = prefs.edit().putString("gamepad_config", v.toJson().toString()).apply()

    var keyboardMode: String
        get() = prefs.getString("keyboard_mode", "layout") ?: "layout"
        set(v) = prefs.edit().putString("keyboard_mode", v).apply()

    var selectedLanguage: String
        get() = prefs.getString("keyboard_language", defaultLanguage()) ?: defaultLanguage()
        set(v) = prefs.edit().putString("keyboard_language", v).apply()

    var connectionMode: String
        get() = prefs.getString("connection_mode", "ble") ?: "ble"
        set(v) = prefs.edit().putString("connection_mode", v).apply()

    var lastScanned: String
        get() = prefs.getString("last_scanned", "") ?: ""
        set(v) = prefs.edit().putString("last_scanned", v).apply()

    private fun defaultLanguage(): String {
        val l = Locale.getDefault()
        return l.language
    }
}
