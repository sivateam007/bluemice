package com.example.bluemice.model

import com.example.bluemice.HidReportBuilder
import kotlinx.serialization.Serializable

@Serializable
data class MacroStep(
    val type: String,
    val keyCode: Byte = 0,
    val consumerCode: Int = 0,
    val mouseButton: Int = 0,
    val delayMs: Int = 0,
    val text: String = ""
)

@Serializable
data class LayoutDefinition(
    val name: String,
    val rows: List<LayoutRow>,
    val mode: String = "keyboard"
)

@Serializable
data class LayoutRow(
    val keys: List<LayoutKey>
)

@Serializable
data class KeyLevels(
    val base: String = "",
    val shift: String = "",
    val alt: String = "",
    val altShift: String = ""
)

@Serializable
data class LayoutKey(
    val label: String,
    val keyCode: Byte = 0,
    val width: Float = 1f,
    val isModifier: Boolean = false,
    val isSpecial: Boolean = false,
    val consumerCode: Int = 0,
    val mouseButton: Int = 0,
    val comboMods: Int = 0,
    val comboKey: Byte = 0,
    val gamepadButton: Int = 0,
    val isGamepad: Boolean = false,
    val mouseScroll: Int = 0,
    val keyType: String = "keyboard",
    val height: Float = 1f,
    val backgroundColor: Long = 0xFF3A3A3A,
    val textColor: Long? = null,
    val textBlock: String = "",
    val macroSteps: List<MacroStep> = emptyList(),
    val fullWidth: Boolean = false,
    val align: String = "start",
    val repeat: Boolean = false,
    val levels: KeyLevels? = null,
    // Appearance knobs. Every one is defaulted so layouts saved before they
    // existed still load, and still look exactly as they did.
    val cornerRadius: Float = 8f,
    val borderWidth: Float = 1f,
    val glow: Float = 0f,
    val fontSize: Float = 0f,
    val boldText: Boolean = false,
    val opacity: Float = 1f
)

object PredefinedLayouts {
    private val KC = HidReportBuilder.KeyCodes
    val QWERTY = LayoutDefinition("QWERTY (US)", listOf(
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("W", 0x1A), LayoutKey("E", 0x08),
            LayoutKey("R", 0x15), LayoutKey("T", 0x17), LayoutKey("Y", 0x1C),
            LayoutKey("U", 0x18), LayoutKey("I", 0x0C), LayoutKey("O", 0x12),
            LayoutKey("P", 0x13)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("S", 0x16), LayoutKey("D", 0x07),
            LayoutKey("F", 0x09), LayoutKey("G", 0x0A), LayoutKey("H", 0x0B),
            LayoutKey("J", 0x0D), LayoutKey("K", 0x0E), LayoutKey("L", 0x0F)
        )),
        LayoutRow(listOf(
            LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19), LayoutKey("B", 0x05), LayoutKey("N", 0x11),
            LayoutKey("M", 0x10)
        )),
        // Function row - was the hardcoded "External Keys" block below the layout.
        LayoutRow(listOf(
            LayoutKey("Esc", keyCode = KC.KEY_ESCAPE, width = 0.8f),
            LayoutKey("F1", keyCode = KC.KEY_F1), LayoutKey("F2", keyCode = KC.KEY_F2),
            LayoutKey("F3", keyCode = KC.KEY_F3), LayoutKey("F4", keyCode = KC.KEY_F4),
            LayoutKey("F5", keyCode = KC.KEY_F5), LayoutKey("F6", keyCode = KC.KEY_F6),
            LayoutKey("F7", keyCode = KC.KEY_F7),
        )),
        LayoutRow(listOf(
            LayoutKey("F8", keyCode = KC.KEY_F8), LayoutKey("F9", keyCode = KC.KEY_F9),
            LayoutKey("F10", keyCode = KC.KEY_F10), LayoutKey("F11", keyCode = KC.KEY_F11),
            LayoutKey("F12", keyCode = KC.KEY_F12),
            LayoutKey("PrtSc", keyCode = KC.KEY_PRINT_SCREEN, width = 1.3f),
        )),
        LayoutRow(listOf(
            LayoutKey("Tab", keyCode = KC.KEY_TAB, width = 1.2f),
            LayoutKey("Enter", keyCode = KC.KEY_ENTER, width = 1.3f, backgroundColor = 0xFF4CAF50),
            LayoutKey("Del", keyCode = KC.KEY_DELETE), LayoutKey("BkSp", keyCode = KC.KEY_BACKSPACE),
            LayoutKey("Ins", keyCode = KC.KEY_INSERT), LayoutKey("Home", keyCode = KC.KEY_HOME),
            LayoutKey("End", keyCode = KC.KEY_END), LayoutKey("PgUp", keyCode = KC.KEY_PAGE_UP),
            LayoutKey("PgDn", keyCode = KC.KEY_PAGE_DOWN),
        )),
        LayoutRow(listOf(
            LayoutKey("PlayPause", consumerCode = 0x00CD, width = 1.4f),
            LayoutKey("Stop", consumerCode = 0x00B7, width = 0.9f),
            LayoutKey("Prev", consumerCode = 0x00B6, width = 0.9f),
            LayoutKey("Next", consumerCode = 0x00B5, width = 0.9f),
            LayoutKey("Vol+", consumerCode = 0x00E9, width = 0.9f),
            LayoutKey("Vol-", consumerCode = 0x00EA, width = 0.9f),
            LayoutKey("Mute", consumerCode = 0x00E2, width = 0.9f),
        )),
        LayoutRow(listOf(
            LayoutKey("\u2191", keyCode = KC.KEY_ARROW_UP, width = 1.5f),
            LayoutKey("\u2193", keyCode = KC.KEY_ARROW_DOWN, width = 1.5f),
            LayoutKey("\u2190", keyCode = KC.KEY_ARROW_LEFT, width = 1.5f),
            LayoutKey("\u2192", keyCode = KC.KEY_ARROW_RIGHT, width = 1.5f),
            // Language keyboard / type-bar row — rendered via the languageBoard slot.
            LayoutKey("Language", keyType = "languageBoard", width = 12f, height = 1f, backgroundColor = 0xFF16162A)
        ))
    ), "keyboard")

    val AZERTY = LayoutDefinition("AZERTY (French)", listOf(
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("Z", 0x1D), LayoutKey("E", 0x08),
            LayoutKey("R", 0x15), LayoutKey("T", 0x17), LayoutKey("Y", 0x1C),
            LayoutKey("U", 0x18), LayoutKey("I", 0x0C), LayoutKey("O", 0x12),
            LayoutKey("P", 0x13)
        )),
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("S", 0x16), LayoutKey("D", 0x07),
            LayoutKey("F", 0x09), LayoutKey("G", 0x0A), LayoutKey("H", 0x0B),
            LayoutKey("J", 0x0D), LayoutKey("K", 0x0E), LayoutKey("L", 0x0F),
            LayoutKey("M", 0x10)
        )),
        LayoutRow(listOf(
            LayoutKey("W", 0x1A), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19), LayoutKey("B", 0x05), LayoutKey("N", 0x11)
        ))
    ), "keyboard")

    val QWERTZ = LayoutDefinition("QWERTZ (German)", listOf(
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("W", 0x1A), LayoutKey("E", 0x08),
            LayoutKey("R", 0x15), LayoutKey("T", 0x17), LayoutKey("Z", 0x1D),
            LayoutKey("U", 0x18), LayoutKey("I", 0x0C), LayoutKey("O", 0x12),
            LayoutKey("P", 0x13)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("S", 0x16), LayoutKey("D", 0x07),
            LayoutKey("F", 0x09), LayoutKey("G", 0x0A), LayoutKey("H", 0x0B),
            LayoutKey("J", 0x0D), LayoutKey("K", 0x0E), LayoutKey("L", 0x0F)
        )),
        LayoutRow(listOf(
            LayoutKey("Y", 0x1C), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19), LayoutKey("B", 0x05), LayoutKey("N", 0x11),
            LayoutKey("M", 0x10)
        ))
    ), "keyboard")

    val DVORAK = LayoutDefinition("Dvorak", listOf(
        LayoutRow(listOf(
            LayoutKey("P", 0x13), LayoutKey("Y", 0x1C), LayoutKey("F", 0x09),
            LayoutKey("G", 0x0A), LayoutKey("C", 0x06), LayoutKey("R", 0x15),
            LayoutKey("L", 0x0F), LayoutKey("/", 0x38)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("O", 0x12), LayoutKey("E", 0x08),
            LayoutKey("U", 0x18), LayoutKey("I", 0x0C), LayoutKey("D", 0x07),
            LayoutKey("H", 0x0B), LayoutKey("T", 0x17), LayoutKey("N", 0x11),
            LayoutKey("S", 0x16)
        )),
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("J", 0x0D), LayoutKey("K", 0x0E),
            LayoutKey("X", 0x1B), LayoutKey("B", 0x05), LayoutKey("M", 0x10),
            LayoutKey("W", 0x1A), LayoutKey("V", 0x19), LayoutKey("Z", 0x1D)
        ))
    ), "keyboard")

    val COLEMAK = LayoutDefinition("Colemak", listOf(
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("W", 0x1A), LayoutKey("F", 0x09),
            LayoutKey("P", 0x13), LayoutKey("G", 0x0A), LayoutKey("J", 0x0D),
            LayoutKey("L", 0x0F), LayoutKey("U", 0x18), LayoutKey("Y", 0x1C),
            LayoutKey(";", 0x33)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("R", 0x15), LayoutKey("S", 0x16),
            LayoutKey("T", 0x17), LayoutKey("D", 0x07), LayoutKey("H", 0x0B),
            LayoutKey("N", 0x11), LayoutKey("E", 0x08), LayoutKey("I", 0x0C),
            LayoutKey("O", 0x12)
        )),
        LayoutRow(listOf(
            LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19), LayoutKey("B", 0x05), LayoutKey("K", 0x0E),
            LayoutKey("M", 0x10)
        ))
    ), "keyboard")

    val QWERTY_UK = LayoutDefinition("QWERTY (UK)", listOf(
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("W", 0x1A), LayoutKey("E", 0x08),
            LayoutKey("R", 0x15), LayoutKey("T", 0x17), LayoutKey("Y", 0x1C),
            LayoutKey("U", 0x18), LayoutKey("I", 0x0C), LayoutKey("O", 0x12),
            LayoutKey("P", 0x13)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("S", 0x16), LayoutKey("D", 0x07),
            LayoutKey("F", 0x09), LayoutKey("G", 0x0A), LayoutKey("H", 0x0B),
            LayoutKey("J", 0x0D), LayoutKey("K", 0x0E), LayoutKey("L", 0x0F)
        )),
        LayoutRow(listOf(
            LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19), LayoutKey("B", 0x05), LayoutKey("N", 0x11),
            LayoutKey("M", 0x10)
        ))
    ), "keyboard")

    val GAMING = LayoutDefinition("Gaming (WASD)", listOf(
        LayoutRow(listOf(
            LayoutKey("Esc", 0x29, 0.8f), LayoutKey("1", 0x1E), LayoutKey("2", 0x1F),
            LayoutKey("3", 0x20), LayoutKey("4", 0x21), LayoutKey("5", 0x22),
            LayoutKey("6", 0x23)
        )),
        LayoutRow(listOf(
            LayoutKey("Tab", 0x2B, 1.2f), LayoutKey("Q", 0x14), LayoutKey("W", 0x1A),
            LayoutKey("E", 0x08), LayoutKey("R", 0x15)
        )),
        LayoutRow(listOf(
            LayoutKey("Caps", 0x39, 1.2f), LayoutKey("A", 0x04), LayoutKey("S", 0x16),
            LayoutKey("D", 0x07), LayoutKey("F", 0x09), LayoutKey("G", 0x0A)
        )),
        LayoutRow(listOf(
            LayoutKey("Shift", 0xE1.toByte(), 1.4f), LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B),
            LayoutKey("C", 0x06), LayoutKey("V", 0x19), LayoutKey("B", 0x05)
        )),
        LayoutRow(listOf(
            LayoutKey("Ctrl", 0xE0.toByte(), 1.2f), LayoutKey("Win", 0xE3.toByte(), 1f),
            LayoutKey("Space", 0x2C, 3f), LayoutKey("Alt", 0xE2.toByte(), 1f)
        ))
    ), "keyboard")

    val MEDIA_PLAYER = LayoutDefinition("Media Player", listOf(
        LayoutRow(listOf(
            LayoutKey("Play/Pause", 0, 1.5f, consumerCode = 0x00CD),
            LayoutKey("Stop", 0, 1f, consumerCode = 0x00B7),
        )),
        LayoutRow(listOf(
            LayoutKey("Prev", 0, 1f, consumerCode = 0x00B6),
            LayoutKey("Next", 0, 1f, consumerCode = 0x00B5),
        )),
        LayoutRow(listOf(
            LayoutKey("Vol−", 0, 1f, consumerCode = 0x00EA),
            LayoutKey("Vol+", 0, 1f, consumerCode = 0x00E9),
            LayoutKey("Mute", 0, 1f, consumerCode = 0x00E2),
        )),
        LayoutRow(listOf(
            LayoutKey("Br−", 0, 1f, consumerCode = 0x0070),
            LayoutKey("Br+", 0, 1f, consumerCode = 0x006F),
        ))
    ), "keyboard")

    val NUMPAD = LayoutDefinition("Numpad", listOf(
        LayoutRow(listOf(
            LayoutKey("NumLk", 0x53), LayoutKey("/", 0x54), LayoutKey("*", 0x55),
            LayoutKey("−", 0x56)
        )),
        LayoutRow(listOf(
            LayoutKey("7", 0x5F), LayoutKey("8", 0x60), LayoutKey("9", 0x61),
            LayoutKey("+", 0x57, 1f)
        )),
        LayoutRow(listOf(
            LayoutKey("4", 0x5C), LayoutKey("5", 0x5D), LayoutKey("6", 0x5E)
        )),
        LayoutRow(listOf(
            LayoutKey("1", 0x59), LayoutKey("2", 0x5A), LayoutKey("3", 0x5B),
            LayoutKey("Enter", 0x58, 1.5f)
        )),
        LayoutRow(listOf(
            LayoutKey("0", 0x62, 2f), LayoutKey(".", 0x63)
        ))
    ), "keyboard")

    val NAVIGATION = LayoutDefinition("Navigation", listOf(
        LayoutRow(listOf(
            LayoutKey("Ins", 0x49), LayoutKey("Del", 0x4C)
        )),
        LayoutRow(listOf(
            LayoutKey("Home", 0x4A), LayoutKey("End", 0x4D)
        )),
        LayoutRow(listOf(
            LayoutKey("PgUp", 0x4B), LayoutKey("PgDn", 0x4E)
        )),
        LayoutRow(listOf(
            LayoutKey("↑", 0x52, 1f)
        )),
        LayoutRow(listOf(
            LayoutKey("←", 0x50), LayoutKey("↓", 0x51), LayoutKey("→", 0x4F)
        ))
    ), "keyboard")

    val PRODUCTIVITY = LayoutDefinition("Productivity", listOf(
        LayoutRow(listOf(
            LayoutKey("Copy", 0, 1f, comboMods = 0x01, comboKey = 0x06),
            LayoutKey("Cut", 0, 1f, comboMods = 0x01, comboKey = 0x1B),
            LayoutKey("Paste", 0, 1f, comboMods = 0x01, comboKey = 0x19),
        )),
        LayoutRow(listOf(
            LayoutKey("Undo", 0, 1f, comboMods = 0x01, comboKey = 0x1D),
            LayoutKey("Redo", 0, 1f, comboMods = 0x01, comboKey = 0x1C),
            LayoutKey("Save", 0, 1f, comboMods = 0x01, comboKey = 0x16),
        )),
        LayoutRow(listOf(
            LayoutKey("SelAll", 0, 1f, comboMods = 0x01, comboKey = 0x04),
            LayoutKey("Find", 0, 1f, comboMods = 0x01, comboKey = 0x09),
            LayoutKey("PermDel", 0, 1.2f, comboMods = 0x02, comboKey = 0x4C),
        )),
        LayoutRow(listOf(
            LayoutKey("←", 0x50), LayoutKey("↑", 0x52), LayoutKey("↓", 0x51), LayoutKey("→", 0x4F)
        )),
        LayoutRow(listOf(
            LayoutKey("Home", 0x4A), LayoutKey("End", 0x4D),
            LayoutKey("BkSp", 0x2A, 1.2f), LayoutKey("Del", 0x4C)
        ))
    ), "keyboard")

    val SYSTEM_SHORTCUTS = LayoutDefinition("System Shortcuts", listOf(
        LayoutRow(listOf(
            LayoutKey("Ctrl+Alt+Del", 0, 1.6f, comboMods = 0x05, comboKey = 0x4C),
            LayoutKey("Ctrl+Shift+Esc", 0, 1.6f, comboMods = 0x03, comboKey = 0x29),
        )),
        LayoutRow(listOf(
            LayoutKey("Alt+Tab", 0, 1.2f, comboMods = 0x04, comboKey = 0x2B),
            LayoutKey("Win+Tab", 0, 1.2f, comboMods = 0x08, comboKey = 0x2B),
        )),
        LayoutRow(listOf(
            LayoutKey("Win+D", 0, 1f, comboMods = 0x08, comboKey = 0x07),
            LayoutKey("Win+E", 0, 1f, comboMods = 0x08, comboKey = 0x08),
            LayoutKey("Win+L", 0, 1f, comboMods = 0x08, comboKey = 0x0F),
            LayoutKey("Win+R", 0, 1f, comboMods = 0x08, comboKey = 0x15),
        )),
        LayoutRow(listOf(
            LayoutKey("Win+I", 0, 1f, comboMods = 0x08, comboKey = 0x0C),
            LayoutKey("Win+S", 0, 1f, comboMods = 0x08, comboKey = 0x16),
            LayoutKey("Win+X", 0, 1f, comboMods = 0x08, comboKey = 0x1B),
        ))
    ), "keyboard")

    val QUICK_LAUNCH = LayoutDefinition("Quick Launch", listOf(
        LayoutRow(listOf(
            LayoutKey("Win+D (Desktop)", 0, 1.3f, comboMods = 0x08, comboKey = 0x07),
            LayoutKey("Win+E (Explorer)", 0, 1.3f, comboMods = 0x08, comboKey = 0x08),
            LayoutKey("Win+I (Settings)", 0, 1.3f, comboMods = 0x08, comboKey = 0x0C),
        )),
        LayoutRow(listOf(
            LayoutKey("Win+R (Run)", 0, 1.2f, comboMods = 0x08, comboKey = 0x15),
            LayoutKey("Win+S (Search)", 0, 1.2f, comboMods = 0x08, comboKey = 0x16),
            LayoutKey("Win+X (Menu)", 0, 1.2f, comboMods = 0x08, comboKey = 0x1B),
        )),
        LayoutRow(listOf(
            LayoutKey("Win+L (Lock)", 0, 1.2f, comboMods = 0x08, comboKey = 0x0F),
            LayoutKey("Win+V (Clip)", 0, 1.2f, comboMods = 0x08, comboKey = 0x19),
            LayoutKey("Win+. (Emoji)", 0, 1.2f, comboMods = 0x08, comboKey = 0x37),
        )),
        LayoutRow(listOf(
            LayoutKey("Win+M (MinAll)", 0, 1.3f, comboMods = 0x08, comboKey = 0x10),
            LayoutKey("Win+P (Project)", 0, 1.3f, comboMods = 0x08, comboKey = 0x13),
            LayoutKey("Win+K (Cast)", 0, 1.3f, comboMods = 0x08, comboKey = 0x0B),
        ))
    ), "keyboard")

    val BROWSER = LayoutDefinition("Browser", listOf(
        LayoutRow(listOf(
            LayoutKey("Ctrl+T (NewTab)", 0, 1.3f, comboMods = 0x01, comboKey = 0x17),
            LayoutKey("Ctrl+W (Close)", 0, 1.3f, comboMods = 0x01, comboKey = 0x1A),
            LayoutKey("Ctrl+N (NewWin)", 0, 1.3f, comboMods = 0x01, comboKey = 0x11),
        )),
        LayoutRow(listOf(
            LayoutKey("Ctrl+Shift+T", 0, 1.3f, comboMods = 0x03, comboKey = 0x17),
            LayoutKey("Ctrl+Tab", 0, 1.2f, comboMods = 0x01, comboKey = 0x2B),
        )),
        LayoutRow(listOf(
            LayoutKey("Ctrl+R (Ref)", 0, 1.2f, comboMods = 0x01, comboKey = 0x15),
            LayoutKey("Ctrl+F (Find)", 0, 1.2f, comboMods = 0x01, comboKey = 0x09),
            LayoutKey("Ctrl+H (Hist)", 0, 1.2f, comboMods = 0x01, comboKey = 0x0B),
        )),
        LayoutRow(listOf(
            LayoutKey("Ctrl+D (Bmk)", 0, 1.2f, comboMods = 0x01, comboKey = 0x07),
            LayoutKey("Ctrl+J (DL)", 0, 1.2f, comboMods = 0x01, comboKey = 0x0D),
        )),
        LayoutRow(listOf(
            LayoutKey("Alt+← (Back)", 0, 1.2f, comboMods = 0x04, comboKey = 0x50),
            LayoutKey("Alt+→ (Fwd)", 0, 1.2f, comboMods = 0x04, comboKey = 0x4F),
            LayoutKey("Alt+D (Addr)", 0, 1.2f, comboMods = 0x04, comboKey = 0x07),
        ))
    ), "keyboard")

    val FUNCTION_ROW = LayoutDefinition("Function Row", listOf(
        LayoutRow(listOf(
            LayoutKey("Esc", 0x29, 0.8f), LayoutKey("F1", 0x3A), LayoutKey("F2", 0x3B),
            LayoutKey("F3", 0x3C), LayoutKey("F4", 0x3D)
        )),
        LayoutRow(listOf(
            LayoutKey("F5", 0x3E), LayoutKey("F6", 0x3F), LayoutKey("F7", 0x40),
            LayoutKey("F8", 0x41)
        )),
        LayoutRow(listOf(
            LayoutKey("F9", 0x42), LayoutKey("F10", 0x43), LayoutKey("F11", 0x44),
            LayoutKey("F12", 0x45)
        )),
        LayoutRow(listOf(
            LayoutKey("PrtSc", 0x46), LayoutKey("ScrLk", 0x47), LayoutKey("Pause", 0x48)
        ))
    ), "keyboard")

    val WORKMAN = LayoutDefinition("Workman", listOf(
        LayoutRow(listOf(
            LayoutKey("Q", 0x14), LayoutKey("D", 0x07), LayoutKey("R", 0x15),
            LayoutKey("W", 0x1A), LayoutKey("B", 0x05), LayoutKey("J", 0x0D),
            LayoutKey("F", 0x09), LayoutKey("U", 0x18), LayoutKey("P", 0x13),
            LayoutKey(";", 0x33)
        )),
        LayoutRow(listOf(
            LayoutKey("A", 0x04), LayoutKey("S", 0x16), LayoutKey("H", 0x0B),
            LayoutKey("T", 0x17), LayoutKey("G", 0x0A), LayoutKey("Y", 0x1C),
            LayoutKey("N", 0x11), LayoutKey("E", 0x08), LayoutKey("O", 0x12),
            LayoutKey("I", 0x0C)
        )),
        LayoutRow(listOf(
            LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B), LayoutKey("M", 0x10),
            LayoutKey("C", 0x06), LayoutKey("V", 0x19), LayoutKey("K", 0x0E),
            LayoutKey("L", 0x0F)
        ))
    ), "keyboard")

    val DEVELOPER_PRO = LayoutDefinition("Developer Pro", listOf(
        LayoutRow(listOf(
            LayoutKey("Save", 0, 1f, comboMods = 0x01, comboKey = 0x16),
            LayoutKey("Undo", 0, 1f, comboMods = 0x01, comboKey = 0x1D),
            LayoutKey("Redo", 0, 1f, comboMods = 0x01, comboKey = 0x1C),
            LayoutKey("Find", 0, 1f, comboMods = 0x01, comboKey = 0x09),
            LayoutKey("FindAll", 0, 1.3f, comboMods = 0x03, comboKey = 0x09),
        )),
        LayoutRow(listOf(
            LayoutKey("F5 (Run)", 0, 0.9f, comboMods = 0, comboKey = 0x3E),
            LayoutKey("F6 (Build)", 0, 1f, comboMods = 0, comboKey = 0x3F),
            LayoutKey("F7 (Debug)", 0, 1f, comboMods = 0, comboKey = 0x40),
            LayoutKey("F8 (Step)", 0, 0.9f, comboMods = 0, comboKey = 0x41),
            LayoutKey("F9 (Cont)", 0, 1f, comboMods = 0, comboKey = 0x42),
        )),
        LayoutRow(listOf(
            LayoutKey("Build", 0, 1f, comboMods = 0x01, comboKey = 0x05),
            LayoutKey("Run", 0, 0.9f, comboMods = 0x01, comboKey = 0x15),
            LayoutKey("Refactor", 0, 1.2f, comboMods = 0x03, comboKey = 0x15),
            LayoutKey("Recent", 0, 1.1f, comboMods = 0x01, comboKey = 0x08),
        )),
        LayoutRow(listOf(
            LayoutKey("DupLine", 0, 1f, comboMods = 0x01, comboKey = 0x07),
            LayoutKey("DelLine", 0, 1f, comboMods = 0x01, comboKey = 0x1C),
            LayoutKey("Comment", 0, 1.2f, comboMods = 0x01, comboKey = 0x38),
            LayoutKey("Format", 0, 1.2f, comboMods = 0x05, comboKey = 0x0C),
        )),
        LayoutRow(listOf(
            LayoutKey("←W", 0, 0.8f, comboMods = 0x01, comboKey = 0x50),
            LayoutKey("→W", 0, 0.8f, comboMods = 0x01, comboKey = 0x4F),
            LayoutKey("Home", 0x4A), LayoutKey("End", 0x4D),
            LayoutKey("PgUp", 0x4B), LayoutKey("PgDn", 0x4E),
        ))
    ), "keyboard")

    val DESIGNER_PRO = LayoutDefinition("Designer Pro", listOf(
        LayoutRow(listOf(
            LayoutKey("Undo", 0, 1f, comboMods = 0x01, comboKey = 0x1D),
            LayoutKey("Redo", 0, 1f, comboMods = 0x01, comboKey = 0x1C),
            LayoutKey("Copy", 0, 1f, comboMods = 0x01, comboKey = 0x06),
            LayoutKey("Cut", 0, 0.8f, comboMods = 0x01, comboKey = 0x1B),
            LayoutKey("Paste", 0, 1f, comboMods = 0x01, comboKey = 0x19),
        )),
        LayoutRow(listOf(
            LayoutKey("SelAll", 0, 1f, comboMods = 0x01, comboKey = 0x04),
            LayoutKey("SelInv", 0, 1.2f, comboMods = 0x03, comboKey = 0x0C),
            LayoutKey("Deselect", 0, 1.2f, comboMods = 0x01, comboKey = 0x07),
        )),
        LayoutRow(listOf(
            LayoutKey("Zoom+", 0, 1f, comboMods = 0x01, comboKey = 0x2E),
            LayoutKey("Zoom−", 0, 1f, comboMods = 0x01, comboKey = 0x2D),
            LayoutKey("Fit", 0, 0.8f, comboMods = 0x01, comboKey = 0x27),
            LayoutKey("Actual", 0, 1f, comboMods = 0x01, comboKey = 0x1E),
        )),
        LayoutRow(listOf(
            LayoutKey("B (Brush)", 0x05), LayoutKey("E (Eraser)", 0x08),
            LayoutKey("L (Line)", 0x0F), LayoutKey("M (Marquee)", 0x10),
            LayoutKey("V (Move)", 0x19), LayoutKey("G (Grad)", 0x0A),
        )),
        LayoutRow(listOf(
            LayoutKey("[ (Small)", 0x2F), LayoutKey("] (Large)", 0x30),
            LayoutKey("Space (Pan)", 0x2C, 2f),
            LayoutKey("Tab", 0x2B, 1.2f),
        ))
    ), "keyboard")

    val OFFICE_PRO = LayoutDefinition("Office Pro", listOf(
        LayoutRow(listOf(
            LayoutKey("New", 0, 1f, comboMods = 0x01, comboKey = 0x11),
            LayoutKey("Open", 0, 1f, comboMods = 0x01, comboKey = 0x12),
            LayoutKey("Save", 0, 1f, comboMods = 0x01, comboKey = 0x16),
            LayoutKey("Print", 0, 1f, comboMods = 0x01, comboKey = 0x13),
        )),
        LayoutRow(listOf(
            LayoutKey("Bold", 0, 1f, comboMods = 0x01, comboKey = 0x05),
            LayoutKey("Italic", 0, 1f, comboMods = 0x01, comboKey = 0x0C),
            LayoutKey("Under", 0, 1.1f, comboMods = 0x01, comboKey = 0x18),
            LayoutKey("Strike", 0, 1.2f, comboMods = 0x03, comboKey = 0x16),
        )),
        LayoutRow(listOf(
            LayoutKey("Left", 0, 1f, comboMods = 0x01, comboKey = 0x0F),
            LayoutKey("Center", 0, 1.1f, comboMods = 0x01, comboKey = 0x08),
            LayoutKey("Right", 0, 1f, comboMods = 0x01, comboKey = 0x15),
            LayoutKey("Justify", 0, 1.2f, comboMods = 0x01, comboKey = 0x0D),
        )),
        LayoutRow(listOf(
            LayoutKey("Font+", 0, 1f, comboMods = 0x03, comboKey = 0x2E),
            LayoutKey("Font−", 0, 1f, comboMods = 0x03, comboKey = 0x2D),
            LayoutKey("Find", 0, 0.9f, comboMods = 0x01, comboKey = 0x09),
            LayoutKey("Replace", 0, 1.2f, comboMods = 0x01, comboKey = 0x1A),
        )),
        LayoutRow(listOf(
            LayoutKey("F7 (Spell)", 0, 1.1f, comboMods = 0, comboKey = 0x40),
            LayoutKey("Link", 0, 1f, comboMods = 0x01, comboKey = 0x0B),
            LayoutKey("Indent+", 0, 1f, comboMods = 0x01, comboKey = 0x10),
            LayoutKey("Indent−", 0, 1.2f, comboMods = 0x03, comboKey = 0x10),
        ))
    ), "keyboard")

    val GAMING_PRO = LayoutDefinition("Gaming Pro", listOf(
        LayoutRow(listOf(
            LayoutKey("Esc", 0x29, 0.8f),
            LayoutKey("1", 0x1E), LayoutKey("2", 0x1F), LayoutKey("3", 0x20),
            LayoutKey("4", 0x21), LayoutKey("5", 0x22), LayoutKey("6", 0x23),
        )),
        LayoutRow(listOf(
            LayoutKey("Tab", 0x2B, 1.2f),
            LayoutKey("Q", 0x14), LayoutKey("W", 0x1A), LayoutKey("E", 0x08),
            LayoutKey("R", 0x15),
        )),
        LayoutRow(listOf(
            LayoutKey("Caps", 0x39, 1.2f),
            LayoutKey("A", 0x04), LayoutKey("S", 0x16), LayoutKey("D", 0x07),
            LayoutKey("F", 0x09),
        )),
        LayoutRow(listOf(
            LayoutKey("Shift", 0xE1.toByte(), 1.4f, isModifier = true),
            LayoutKey("Z", 0x1D), LayoutKey("X", 0x1B), LayoutKey("C", 0x06),
            LayoutKey("V", 0x19),
        )),
        LayoutRow(listOf(
            LayoutKey("Ctrl", 0xE0.toByte(), 1.2f, isModifier = true),
            LayoutKey("Space", 0x2C, 3f),
            LayoutKey("Alt", 0xE2.toByte(), 1f, isModifier = true),
        ))
    ), "keyboard")

    // Default layouts per mode
    /**
     * The full mouse surface: pointer pad, mouse buttons, arrows, the scroll
     * wheel and a small utility row. Everything here is an ordinary layout key,
     * so the user can move, resize, restyle or delete every single one of them.
     */
    /**
     * Trackpad-first mouse remote.
     *
     * Every row sums to the same 12 units, so the columns line up down the pad -
     * which is what lets the arrows sit as a genuine cross (Up centred, with Left /
     * Down / Right beneath it) instead of a flat strip, while Enter and Esc flank
     * the top of the cross and Tab and BkSp the bottom. The touchpad row fills the
     * panel and the scroll wheel fills the space beside it like the wheel on a real
     * mouse; the buttons sit under the pad with Left Click widest, because it is
     * the one that gets hit most.
     */
    val MOUSE_DEFAULT = LayoutDefinition("Mouse Default", listOf(
        // R1 - hero: trackpad with the scroll wheel down its right edge.
        // height only matters in the editor - live, both stretch with the row.
        LayoutRow(listOf(
            LayoutKey("Touchpad", keyType = "touchpad", width = 9f, height = 1.5f),
            LayoutKey("Scroll", keyType = "scroll", width = 3f, height = 1.5f)
        )),
        // R2 - thumb row under the pad: Left > Right > Middle.
        LayoutRow(listOf(
            LayoutKey("Left Click", mouseButton = 1, width = 5f, height = 1.2f),
            LayoutKey("Right Click", mouseButton = 2, width = 4f, height = 1.2f),
            LayoutKey("Middle Click", mouseButton = 4, width = 3f, height = 1.2f)
        )),
        // R3 - top of the arrow cross, with Enter and Esc in the wings.
        LayoutRow(listOf(
            LayoutKey("Enter", keyCode = 0x28, width = 3f),
            LayoutKey("", keyType = "spacer", width = 2f),
            LayoutKey("↑", keyCode = 0x52, width = 2f),
            LayoutKey("", keyType = "spacer", width = 2f),
            LayoutKey("Esc", keyCode = 0x29, width = 3f)
        )),
        // R4 - bottom of the cross; Tab and BkSp mirror Enter and Esc above.
        LayoutRow(listOf(
            LayoutKey("Tab", keyCode = 0x2B, width = 3f),
            LayoutKey("←", keyCode = 0x50, width = 2f),
            LayoutKey("↓", keyCode = 0x51, width = 2f),
            LayoutKey("→", keyCode = 0x4F, width = 2f),
            LayoutKey("BkSp", keyCode = 0x2A, width = 3f)
        ))
    ), "mouse")

    /** Everything the Media screen used to hard-code, as ordinary editable keys. */
    /**
     * Media remote built around the pattern every good transport UI uses:
     * Previous · Play/Pause · Next as one dominant bar, then grouped control
     * bands underneath - secondary transport, sound, light, navigation. Groups
     * are tinted so you can find them without reading, every key is at least
     * 48dp, and the hero row is 72dp so Play/Pause is the obvious target.
     * Rows share the same 12-unit split, so the nav arrows form a real cross
     * with OK dead centre - the way a TV remote lays it out.
     */
    val MEDIA_DEFAULT = LayoutDefinition("Media Default", listOf(
        // R1 - hero: prev / play-pause / next, play-pause widest. 72dp tall.
        LayoutRow(listOf(
            LayoutKey("⏮", consumerCode = 0x00B6, width = 3.5f, height = 1.5f, backgroundColor = 0xFF1A5A52),
            LayoutKey("⏯", consumerCode = 0x00CD, width = 5f, height = 1.5f, backgroundColor = 0xFF1A5A52),
            LayoutKey("⏭", consumerCode = 0x00B5, width = 3.5f, height = 1.5f, backgroundColor = 0xFF1A5A52)
        )),
        // R2 - the rest of the transport deck, same family, one shade quieter.
        LayoutRow(listOf(
            LayoutKey("⏹", consumerCode = 0x00B7, backgroundColor = 0xFF1A3A3A),
            LayoutKey("⏪", consumerCode = 0x00B4, backgroundColor = 0xFF1A3A3A),
            LayoutKey("⏩", consumerCode = 0x00B3, backgroundColor = 0xFF1A3A3A)
        )),
        // R3 - sound band, amber.
        LayoutRow(listOf(
            LayoutKey("Vol−", consumerCode = 0x00EA, backgroundColor = 0xFF3A3A1A),
            LayoutKey("Mute", consumerCode = 0x00E2, backgroundColor = 0xFF4A2A2A),
            LayoutKey("Vol+", consumerCode = 0x00E9, backgroundColor = 0xFF3A3A1A)
        )),
        // R4 - light band, blue.
        LayoutRow(listOf(
            LayoutKey("Br−", consumerCode = 0x0070, width = 6f, backgroundColor = 0xFF2A2A4A),
            LayoutKey("Br+", consumerCode = 0x006F, width = 6f, backgroundColor = 0xFF2A2A4A)
        )),
        // R5-R7 - navigation. 4 | 4 | 4 on all three rows puts Up / OK / Down in
        // one column and Left / Right either side of OK, i.e. a true cross.
        LayoutRow(listOf(
            LayoutKey("Esc", keyCode = 0x29, backgroundColor = 0xFF2A3A2A),
            LayoutKey("↑", keyCode = 0x52),
            LayoutKey("Full", keyCode = 0x77, backgroundColor = 0xFF2A3A2A)
        )),
        LayoutRow(listOf(
            LayoutKey("←", keyCode = 0x50),
            LayoutKey("OK", keyCode = 0x28, backgroundColor = 0xFF2A4A2A),
            LayoutKey("→", keyCode = 0x4F)
        )),
        LayoutRow(listOf(
            LayoutKey("Sleep", keyCode = 0x82.toByte(), backgroundColor = 0xFF2A3A5A),
            LayoutKey("↓", keyCode = 0x51),
            LayoutKey("Wake", keyCode = 0x83.toByte(), backgroundColor = 0xFF2A3A5A)
        ))
    ), "media")

    /**
     * Landscape-first controller geometry, designed around where the hands are.
     *
     * All three rows share one 6 | 4 | 6 weight split, so the centre gap is a
     * single aligned column from top to bottom. That alignment is what makes it
     * read as a controller instead of a stack of unrelated rows - and it is the
     * reason the thumb clusters never drift into each other.
     *
     * Placement follows two findings rather than the Xbox shape:
     *
     * - The MuC '24 smartphone-gamepad study found a fixed Xbox clone scored
     *   *worst* and the customisable one best, and that Select/Start "serve no
     *   gameplay purpose in most games". They are dropped here and stay one tap
     *   away in the palette, so anyone who wants them adds them back.
     * - Otten's thumb-reach envelopes put the comfortable zone in the lower
     *   corners. The sticks are the primary control, so they own the bottom row;
     *   the D-pad and face buttons sit above them, where a thumb reaches up
     *   rather than settling. Shoulders stay on the very top edge for the index
     *   fingers, grouped left and right with the middle left open.
     *
     * Sticks and pads run at 1.5x height (120dp natural) so the pad has real
     * vertical mass to fill a landscape viewport with - [GAMEPAD_DEFAULT] is
     * 300dp before play mode scales it to fit.
     */
    val GAMEPAD_DEFAULT = LayoutDefinition("Gamepad Default", listOf(
        // R1 - shoulders on the top edge, grouped per side.
        LayoutRow(listOf(
            LayoutKey("LB", isGamepad = true, gamepadButton = 4, width = 3f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("LT", isGamepad = true, gamepadButton = 6, width = 3f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("", keyType = "spacer", width = 4f),
            LayoutKey("RT", isGamepad = true, gamepadButton = 7, width = 3f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("RB", isGamepad = true, gamepadButton = 5, width = 3f, backgroundColor = 0xFF2A3A4A)
        )),
        // R2 - secondary thumb controls, above the sticks.
        LayoutRow(listOf(
            LayoutKey("D-Pad", keyType = "dpad", width = 6f, height = 1.5f, backgroundColor = 0xFF14202E),
            LayoutKey("", keyType = "spacer", width = 4f),
            LayoutKey("ABXY", keyType = "face", width = 6f, height = 1.5f, backgroundColor = 0xFF14202E)
        )),
        // R3 - both sticks in the bottom corners, where the thumbs rest.
        LayoutRow(listOf(
            LayoutKey("Left Stick", keyType = "stickL", width = 6f, height = 1.5f),
            LayoutKey("", keyType = "spacer", width = 4f),
            LayoutKey("Right Stick", keyType = "stickR", width = 6f, height = 1.5f)
        ))
    ), "gamepad")

    /**
     * Presenter remote.
     *
     * You press Next about ninety per cent of the time, so Next is the biggest
     * thing on the screen: half again as wide as Prev and 120dp tall, taking the
     * full row between them. Everything else drops to a 48dp grid of short labels
     * underneath - slide jumps, playback, then light and power - and all three
     * grid rows share one 12-unit split so they line up column for column.
     *
     * Play, Pause and Brightness used to be wired to Next-Track, Fast-Forward and
     * Copy; the labels and the codes disagreed. They now send what they say.
     */
    val PRESENTER_DEFAULT = LayoutDefinition("Presenter Default", listOf(
        // R1 - hero. Next is 50% wider than Prev because Next is the one you use.
        LayoutRow(listOf(
            LayoutKey("Prev", keyCode = 0x50, width = 4.8f, height = 2.5f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("Next", keyCode = 0x4F, width = 7.2f, height = 2.5f, backgroundColor = 0xFF1A5A52)
        )),
        // R2 - slide jumps, plus PowerPoint's B key to blank the screen.
        LayoutRow(listOf(
            LayoutKey("Start", keyCode = 0x4E),
            LayoutKey("End", keyCode = 0x4D),
            LayoutKey("Esc", keyCode = 0x29),
            LayoutKey("Black", keyCode = 0x05, backgroundColor = 0xFF3A2A2A)
        )),
        // R3 - playback. F11 is the one that actually toggles full screen.
        LayoutRow(listOf(
            LayoutKey("Play", keyCode = 0xB0.toByte()),
            LayoutKey("Pause", keyCode = 0xB1.toByte()),
            LayoutKey("F11", keyCode = 0x44, backgroundColor = 0xFF2A3A2A)
        )),
        // R4 - light and power.
        LayoutRow(listOf(
            LayoutKey("Br−", consumerCode = 0x0070, backgroundColor = 0xFF2A2A4A),
            LayoutKey("Br+", consumerCode = 0x006F, backgroundColor = 0xFF2A2A4A),
            LayoutKey("Power", keyCode = 0x66, backgroundColor = 0xFF4A2A2A)
        ))
    ), "presenter")

    val all = listOf(QWERTY, AZERTY, QWERTZ, DVORAK, COLEMAK, QWERTY_UK, WORKMAN,
        GAMING, MEDIA_PLAYER, NUMPAD, NAVIGATION, PRODUCTIVITY, SYSTEM_SHORTCUTS,
        QUICK_LAUNCH, BROWSER, FUNCTION_ROW,
        DEVELOPER_PRO, DESIGNER_PRO, OFFICE_PRO, GAMING_PRO,
        MOUSE_DEFAULT, MEDIA_DEFAULT, GAMEPAD_DEFAULT, PRESENTER_DEFAULT)
}
