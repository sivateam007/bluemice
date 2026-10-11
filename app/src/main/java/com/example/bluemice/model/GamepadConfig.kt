package com.example.bluemice.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single binding option offered in the gamepad remap UI.
 *
 * [code] is the value written into the HID report. It is a USB HID button
 * usage for the gamepad targets, a consumer-page usage for media targets, a
 * mouse-button bitmask for mouse targets, or a raw keyboard usage ID.
 */
data class GamepadAction(
    val id: String,
    val label: String,
    val kind: Kind,
    val code: Int
) {
    enum class Kind { GAMEPAD, KEYBOARD, SHORTCUT, CONSUMER, MOUSE, MACRO, HEX }
}

/**
 * Everything the gamepad screen needs that isn't a key layout:
 * button remapping plus stick/trigger tuning.
 *
 * [bindings] maps a *physical* button bit (0..15, as pressed on screen) to the
 * logical bit reported to the PC. Identity by default, so untouched configs
 * behave exactly like the old hard-coded mapping.
 */
data class GamepadConfig(
    /** physical bit -> logical bit for the 16 digital buttons. */
    val bindings: Map<Int, Int> = defaultBindings(),
    /** Raw HID usage the physical bit sends, when [bindings] routes to a non-gamepad action. */
    val overrides: Map<Int, GamepadAction> = emptyMap(),
    val stickDeadzone: Float = 0.12f,
    val stickSensitivity: Float = 1f,
    val invertLeftX: Boolean = false,
    val invertLeftY: Boolean = false,
    val invertRightX: Boolean = false,
    val invertRightY: Boolean = false,
    /** 0..1 — travel before a digital trigger registers. */
    val triggerThreshold: Float = 0.35f,
    val preset: String = PRESET_XBOX
) {
    companion object {
        const val PRESET_XBOX = "Xbox"
        const val PRESET_NINTENDO = "Nintendo"
        const val PRESET_WASD = "Keyboard WASD"

        /**
         * Standard Xbox layout, which is what the HID descriptor declares.
         * 0 A, 1 B, 2 X, 3 Y, 4 LB, 5 RB, 6 LT, 7 RT, 8 Select, 9 Start,
         * 10 L3, 11 R3, 12..15 D-pad up/left/down/right.
         */
        fun defaultBindings(): Map<Int, Int> = (0..15).associateWith { it }

        /**
         * Nintendo swaps A<->B and X<->Y relative to Xbox. Only the four face
         * buttons move; shoulders, menu buttons and the d-pad stay put.
         */
        fun nintendoBindings(): Map<Int, Int> = defaultBindings().toMutableMap().apply {
            put(0, 1)  // physical A (bottom) -> logical B
            put(1, 0)  // physical B (right)  -> logical A
            put(2, 3)  // physical X (left)   -> logical Y
            put(3, 2)  // physical Y (top)    -> logical X
        }

        /** All face/shoulder buttons become keyboard keys; see [wasdActions]. */
        fun wasdBindings(): Map<Int, Int> = defaultBindings()
    }
}

/**
 * The keyboard remap used by the WASD preset. Keys without an entry fall back
 * to the standard gamepad binding.
 */
val wasdActions: Map<Int, GamepadAction> = mapOf(
    12 to GamepadAction("w", "W", GamepadAction.Kind.KEYBOARD, 0x1A),
    13 to GamepadAction("a", "A", GamepadAction.Kind.KEYBOARD, 0x04),
    14 to GamepadAction("s", "S", GamepadAction.Kind.KEYBOARD, 0x16),
    15 to GamepadAction("d", "D", GamepadAction.Kind.KEYBOARD, 0x07),
    0  to GamepadAction("space", "Space", GamepadAction.Kind.KEYBOARD, 0x2C),
    1  to GamepadAction("shift", "Shift", GamepadAction.Kind.KEYBOARD, 0xE1),
    2  to GamepadAction("ctrl", "Ctrl", GamepadAction.Kind.KEYBOARD, 0xE0),
    3  to GamepadAction("esc", "Esc", GamepadAction.Kind.KEYBOARD, 0x29),
    4  to GamepadAction("q", "Q", GamepadAction.Kind.KEYBOARD, 0x14),
    5  to GamepadAction("e", "E", GamepadAction.Kind.KEYBOARD, 0x08)
)

/** The remap targets the UI offers for any single physical button. */
fun gamepadActionCatalog(): List<GamepadAction> = listOf(
    GamepadAction("gp_a", "A", GamepadAction.Kind.GAMEPAD, 0),
    GamepadAction("gp_b", "B", GamepadAction.Kind.GAMEPAD, 1),
    GamepadAction("gp_x", "X", GamepadAction.Kind.GAMEPAD, 2),
    GamepadAction("gp_y", "Y", GamepadAction.Kind.GAMEPAD, 3),
    GamepadAction("gp_lb", "LB", GamepadAction.Kind.GAMEPAD, 4),
    GamepadAction("gp_rb", "RB", GamepadAction.Kind.GAMEPAD, 5),
    GamepadAction("gp_lt", "LT", GamepadAction.Kind.GAMEPAD, 6),
    GamepadAction("gp_rt", "RT", GamepadAction.Kind.GAMEPAD, 7),
    GamepadAction("gp_sel", "Select", GamepadAction.Kind.GAMEPAD, 8),
    GamepadAction("gp_st", "Start", GamepadAction.Kind.GAMEPAD, 9),
    GamepadAction("gp_l3", "L3", GamepadAction.Kind.GAMEPAD, 10),
    GamepadAction("gp_r3", "R3", GamepadAction.Kind.GAMEPAD, 11),
    GamepadAction("gp_up", "D-pad Up", GamepadAction.Kind.GAMEPAD, 12),
    GamepadAction("gp_left", "D-pad Left", GamepadAction.Kind.GAMEPAD, 13),
    GamepadAction("gp_down", "D-pad Down", GamepadAction.Kind.GAMEPAD, 14),
    GamepadAction("gp_right", "D-pad Right", GamepadAction.Kind.GAMEPAD, 15),

    GamepadAction("kb_enter", "Enter", GamepadAction.Kind.KEYBOARD, 0x28),
    GamepadAction("kb_esc", "Escape", GamepadAction.Kind.KEYBOARD, 0x29),
    GamepadAction("kb_backspace", "Backspace", GamepadAction.Kind.KEYBOARD, 0x2A),
    GamepadAction("kb_tab", "Tab", GamepadAction.Kind.KEYBOARD, 0x2B),
    GamepadAction("kb_space", "Space", GamepadAction.Kind.KEYBOARD, 0x2C),
    GamepadAction("kb_f", "F", GamepadAction.Kind.KEYBOARD, 0x09),

    GamepadAction("sc_next", "Next Track", GamepadAction.Kind.CONSUMER, 0x00B5),
    GamepadAction("sc_prev", "Prev Track", GamepadAction.Kind.CONSUMER, 0x00B6),
    GamepadAction("sc_pause", "Play/Pause", GamepadAction.Kind.CONSUMER, 0x00CD),
    GamepadAction("sc_volup", "Vol +", GamepadAction.Kind.CONSUMER, 0x00E9),
    GamepadAction("sc_voldn", "Vol -", GamepadAction.Kind.CONSUMER, 0x00EA),
    GamepadAction("sc_mute", "Mute", GamepadAction.Kind.CONSUMER, 0x00E2),

    GamepadAction("ms_left", "Left Click", GamepadAction.Kind.MOUSE, 0x01),
    GamepadAction("ms_right", "Right Click", GamepadAction.Kind.MOUSE, 0x02),
    GamepadAction("ms_mid", "Middle Click", GamepadAction.Kind.MOUSE, 0x04),

    GamepadAction("none", "None", GamepadAction.Kind.HEX, 0)
)

/** Human-readable name of the button a physical bit normally drives. */
fun defaultActionFor(bit: Int): GamepadAction =
    gamepadActionCatalog().firstOrNull { it.kind == GamepadAction.Kind.GAMEPAD && it.code == bit }
        ?: GamepadAction("gp_$bit", "Button $bit", GamepadAction.Kind.GAMEPAD, bit)

// ---------------------------------------------------------------- serialization

fun GamepadConfig.toJson(): JSONObject = JSONObject().apply {
    put("preset", preset)
    put("deadzone", stickDeadzone.toDouble())
    put("sensitivity", stickSensitivity.toDouble())
    put("invertLX", invertLeftX)
    put("invertLY", invertLeftY)
    put("invertRX", invertRightX)
    put("invertRY", invertRightY)
    put("triggerThreshold", triggerThreshold.toDouble())

    val b = JSONArray()
    bindings.forEach { (k, v) -> b.put(JSONObject().put("p", k).put("l", v)) }
    put("bindings", b)

    val o = JSONArray()
    overrides.forEach { (k, a) ->
        o.put(JSONObject().put("p", k).put("id", a.id))
    }
    put("overrides", o)
}

/**
 * Lenient parse: any missing or malformed field falls back to the default, so
 * a config written by an older build (or hand-edited) still loads.
 */
fun gamepadConfigFromJson(obj: JSONObject?): GamepadConfig {
    if (obj == null) return GamepadConfig()
    return runCatching {
        val catalog = gamepadActionCatalog()

        val bindingsArr = obj.optJSONArray("bindings")
        val bindings = (0 until (bindingsArr?.length() ?: 0)).mapNotNull { i ->
            val b = bindingsArr.optJSONObject(i) ?: return@mapNotNull null
            val p = b.optInt("p", -1)
            val l = b.optInt("l", -1)
            if (p in 0..15 && l in 0..15) p to l else null
        }.toMap().ifEmpty { GamepadConfig.defaultBindings() }

        val overrides = runCatching {
            val arr = obj.optJSONArray("overrides") ?: return@runCatching emptyMap()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val p = o.optInt("p", -1)
                val act = catalog.firstOrNull { it.id == o.optString("id") }
                if (p in 0..15 && act != null && act.kind != GamepadAction.Kind.GAMEPAD) p to act else null
            }.toMap()
        }.getOrDefault(emptyMap())

        GamepadConfig(
            bindings = bindings,
            overrides = overrides,
            stickDeadzone = obj.optDouble("deadzone", 0.12).toFloat().coerceIn(0f, 0.6f),
            stickSensitivity = obj.optDouble("sensitivity", 1.0).toFloat().coerceIn(0.25f, 4f),
            invertLeftX = obj.optBoolean("invertLX", false),
            invertLeftY = obj.optBoolean("invertLY", false),
            invertRightX = obj.optBoolean("invertRX", false),
            invertRightY = obj.optBoolean("invertRY", false),
            triggerThreshold = obj.optDouble("triggerThreshold", 0.35).toFloat().coerceIn(0f, 1f),
            preset = obj.optString("preset", GamepadConfig.PRESET_XBOX)
        )
    }.getOrDefault(GamepadConfig())
}

/** Builds a full config for a named preset. */
fun gamepadPreset(name: String): GamepadConfig = when (name) {
    GamepadConfig.PRESET_NINTENDO -> GamepadConfig(
        bindings = GamepadConfig.nintendoBindings(),
        preset = GamepadConfig.PRESET_NINTENDO
    )
    GamepadConfig.PRESET_WASD -> GamepadConfig(
        bindings = GamepadConfig.wasdBindings(),
        overrides = wasdActions,
        preset = GamepadConfig.PRESET_WASD
    )
    else -> GamepadConfig(preset = GamepadConfig.PRESET_XBOX)
}
