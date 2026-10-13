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
    val levels: KeyLevels? = null
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

    /**
     * Mirrors the live numpad screen: display controls, digits, operators and the
     * extra function row. "C"/"CE"/"+-" send nothing on purpose - they only shape
     * the on-screen readout, which NumpadScreen mirrors via onKeySent.
     */
    val NUMPAD_DEFAULT = LayoutDefinition("Numpad Default", listOf(
        LayoutRow(listOf(
            LayoutKey("C", width = 0.8f, backgroundColor = 0xFF6A1A1A),
            LayoutKey("CE", backgroundColor = 0xFF6A1A1A),
            LayoutKey("\u232B", keyCode = KC.KEY_BACKSPACE, backgroundColor = 0xFF8B3A3A),
            LayoutKey("Calc", keyCode = 0x7B, width = 0.9f, backgroundColor = 0xFF2A2A5A),
        )),
        LayoutRow(listOf(
            LayoutKey("NumLk", keyCode = KC.KEY_NUM_LOCK, backgroundColor = 0xFF4A2A2A),
            // "(" ")" and "%" need shift; the old screen sent bare W/X/. by mistake.
            LayoutKey("(", comboMods = 0x01, comboKey = 0x26, backgroundColor = 0xFF2A3A4A),
            LayoutKey(")", comboMods = 0x01, comboKey = 0x27, backgroundColor = 0xFF2A3A4A),
            LayoutKey("%", comboMods = 0x01, comboKey = 0x22, backgroundColor = 0xFF3A3A50),
            LayoutKey("/", keyCode = KC.KEY_KP_SLASH, backgroundColor = 0xFF3A3A50),
        )),
        LayoutRow(listOf(
            LayoutKey("7", keyCode = KC.KEY_KP_7),
            LayoutKey("8", keyCode = KC.KEY_KP_8),
            LayoutKey("9", keyCode = KC.KEY_KP_9),
            LayoutKey("\u00D7", keyCode = KC.KEY_KP_ASTERISK, backgroundColor = 0xFF3A3A50),
            LayoutKey("\u232B", keyCode = KC.KEY_BACKSPACE, backgroundColor = 0xFF8B3A3A),
        )),
        LayoutRow(listOf(
            LayoutKey("4", keyCode = KC.KEY_KP_4),
            LayoutKey("5", keyCode = KC.KEY_KP_5),
            LayoutKey("6", keyCode = KC.KEY_KP_6),
            LayoutKey("\u2212", keyCode = KC.KEY_KP_MINUS, backgroundColor = 0xFF3A3A50),
            LayoutKey("CE", backgroundColor = 0xFF6A1A1A),
        )),
        LayoutRow(listOf(
            LayoutKey("1", keyCode = KC.KEY_KP_1),
            LayoutKey("2", keyCode = KC.KEY_KP_2),
            LayoutKey("3", keyCode = KC.KEY_KP_3),
            LayoutKey("+", keyCode = KC.KEY_KP_PLUS, backgroundColor = 0xFF3A3A50),
            LayoutKey("Enter", keyCode = KC.KEY_KP_ENTER, width = 1.2f, backgroundColor = 0xFF2A4A2A),
        )),
        LayoutRow(listOf(
            LayoutKey("\u00B1", backgroundColor = 0xFF3A3A50),
            LayoutKey("0", keyCode = KC.KEY_KP_0, width = 1.5f),
            LayoutKey(".", keyCode = KC.KEY_KP_DOT),
            LayoutKey("00", width = 1.1f, macroSteps = listOf(
                MacroStep("key", KC.KEY_KP_0), MacroStep("key", KC.KEY_KP_0))),
            LayoutKey("000", width = 1.1f, macroSteps = listOf(
                MacroStep("key", KC.KEY_KP_0), MacroStep("key", KC.KEY_KP_0), MacroStep("key", KC.KEY_KP_0))),
        )),
        LayoutRow(listOf(
            LayoutKey("Tab", keyCode = KC.KEY_TAB, width = 1.2f),
            LayoutKey("Space", keyCode = KC.KEY_SPACE, width = 2f),
            LayoutKey("\u2190", keyCode = KC.KEY_ARROW_LEFT),
            LayoutKey("\u2192", keyCode = KC.KEY_ARROW_RIGHT),
        )),
        LayoutRow(listOf(
            LayoutKey("ScrLk", keyCode = KC.KEY_SCROLL_LOCK),
            LayoutKey("Pause", keyCode = KC.KEY_PAUSE),
            LayoutKey("Sleep", keyCode = KC.KEY_SLEEP),
            LayoutKey("Wake", keyCode = KC.KEY_WAKE),
        ))
    ), "numpad")

    /** Everything the Media screen used to hard-code, as ordinary editable keys. */
    val MEDIA_DEFAULT = LayoutDefinition("Media Default", listOf(
        LayoutRow(listOf(
            LayoutKey("Mute", consumerCode = 0x00E2, backgroundColor = 0xFF4A2A2A),
            LayoutKey("Vol\u2212", consumerCode = 0x00EA, backgroundColor = 0xFF3A3A1A),
            LayoutKey("Vol+", consumerCode = 0x00E9, backgroundColor = 0xFF3A3A1A),
        )),
        LayoutRow(listOf(
            LayoutKey("Br\u2212", consumerCode = 0x0070, backgroundColor = 0xFF2A2A4A),
            LayoutKey("Br+", consumerCode = 0x006F, backgroundColor = 0xFF2A2A4A),
        )),
        LayoutRow(listOf(
            LayoutKey("\u23EE", consumerCode = 0x00B6, backgroundColor = 0xFF1A3A3A),
            LayoutKey("\u23EA", consumerCode = 0x00B4, backgroundColor = 0xFF1A3A3A),
            LayoutKey("\u23F8", consumerCode = 0x00B1, backgroundColor = 0xFF1A4A3A),
            LayoutKey("\u25B6", consumerCode = 0x00B0, backgroundColor = 0xFF1A4A3A),
            LayoutKey("\u23ED", consumerCode = 0x00B5, backgroundColor = 0xFF1A3A3A),
            LayoutKey("\u23F9", consumerCode = 0x00B7, backgroundColor = 0xFF3A1A1A),
        )),
        // Navigation pad (the old MediaDpad cross, unwound into one editable row)
        LayoutRow(listOf(
            LayoutKey("\u2191", keyCode = 0x52, width = 1.2f),
            LayoutKey("\u2190", keyCode = 0x50, width = 1.2f),
            LayoutKey("OK", keyCode = 0x28, width = 1.2f, backgroundColor = 0xFF2A4A2A),
            LayoutKey("\u2192", keyCode = 0x4F, width = 1.2f),
            LayoutKey("\u2193", keyCode = 0x51, width = 1.2f),
        )),
        LayoutRow(listOf(
            LayoutKey("Esc", keyCode = 0x29),
            LayoutKey("F11", keyCode = 0x44),
            LayoutKey("Full", keyCode = 0x77, backgroundColor = 0xFF2A3A2A),
            LayoutKey("Sleep", keyCode = 0x82.toByte(), backgroundColor = 0xFF2A3A5A),
            LayoutKey("Wake", keyCode = 0x83.toByte(), backgroundColor = 0xFF2A3A5A),
        )),
        LayoutRow(listOf(
            LayoutKey("Calc", keyCode = 0x7B, backgroundColor = 0xFF2A2A5A),
            LayoutKey("Mail", keyCode = 0x75, backgroundColor = 0xFF4A2A2A),
            LayoutKey("Web", keyCode = 0x74, backgroundColor = 0xFF2A3A5A),
            LayoutKey("MyPC", keyCode = 0x7A, backgroundColor = 0xFF2A4A3A),
            LayoutKey("Search", keyCode = 0x77),
        )),
        LayoutRow(listOf(
            LayoutKey("Copy", comboMods = 0x01, comboKey = 0x06, width = 1.3f),
            LayoutKey("Paste", comboMods = 0x01, comboKey = 0x19, width = 1.3f),
            LayoutKey("Cut", comboMods = 0x01, comboKey = 0x1B, width = 1.3f),
            LayoutKey("Undo", comboMods = 0x01, comboKey = 0x1D, width = 1.3f),
            LayoutKey("Redo", comboMods = 0x01, comboKey = 0x1C, width = 1.3f),
            LayoutKey("Save", comboMods = 0x01, comboKey = 0x16, width = 1.3f),
            LayoutKey("SelAll", comboMods = 0x01, comboKey = 0x04, width = 1.3f),
        )),
        LayoutRow(listOf(
            LayoutKey("Lock", comboMods = 0x08, comboKey = 0x0F, width = 1.3f),
            LayoutKey("Desk", comboMods = 0x08, comboKey = 0x07, width = 1.3f),
            LayoutKey("Run", comboMods = 0x08, comboKey = 0x15, width = 1.3f),
            LayoutKey("Close", comboMods = 0x04, comboKey = 0x3D, width = 1.3f),
            LayoutKey("TaskMgr", comboMods = 0x03, comboKey = 0x29, width = 1.3f),
        ))
    ), "media")

    /**
     * Everything the neon gamepad used to draw: both analog sticks, the d-pad,
     * face buttons, shoulders and the centre cluster - all remappable keys.
     */
    /**
     * Landscape-first controller geometry.
     *
     * Every row uses the same 4 | 4 | 4 weight split, so the three clusters line
     * up column-for-column down the whole pad - that alignment is what makes it
     * read as a controller instead of a stack of unrelated rows. Placement follows
     * the standard Xbox order: shoulders on the top edge (index fingers), left
     * stick above the D-pad, face buttons above the right stick, everything else
     * in the thumb arc. Three rows keeps it inside a landscape viewport.
     */
    val GAMEPAD_DEFAULT = LayoutDefinition("Gamepad Default", listOf(
        // R1 - shoulders + centre pair. 48dp tall (the Material minimum).
        LayoutRow(listOf(
            LayoutKey("LB", isGamepad = true, gamepadButton = 4, width = 2f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("LT", isGamepad = true, gamepadButton = 6, width = 2f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("Sel", isGamepad = true, gamepadButton = 8, width = 2f, backgroundColor = 0xFF2A2A3A),
            LayoutKey("Start", isGamepad = true, gamepadButton = 9, width = 2f, backgroundColor = 0xFF2A2A3A),
            LayoutKey("RB", isGamepad = true, gamepadButton = 5, width = 2f, backgroundColor = 0xFF2A3A4A),
            LayoutKey("RT", isGamepad = true, gamepadButton = 7, width = 2f, backgroundColor = 0xFF2A3A4A)
        )),
        // R2 - left stick upper-left, ABXY diamond upper-right, empty centre.
        LayoutRow(listOf(
            LayoutKey("Left Stick", keyType = "stickL", width = 4f),
            LayoutKey("", keyType = "spacer", width = 4f),
            LayoutKey("ABXY", keyType = "face", width = 4f, backgroundColor = 0xFF14202E)
        )),
        // R3 - D-pad lower-left, right stick lower-right, stick clicks in the middle.
        LayoutRow(listOf(
            LayoutKey("D-Pad", keyType = "dpad", width = 4f, backgroundColor = 0xFF14202E),
            LayoutKey("L3", isGamepad = true, gamepadButton = 10, width = 2f, backgroundColor = 0xFF2A2A3A),
            LayoutKey("R3", isGamepad = true, gamepadButton = 11, width = 2f, backgroundColor = 0xFF2A2A3A),
            LayoutKey("Right Stick", keyType = "stickR", width = 4f)
        ))
    ), "gamepad")

    /** Mirrors the old presenter sections: nav, slide jumps, playback, extras. */
    val PRESENTER_DEFAULT = LayoutDefinition("Presenter Default", listOf(
        LayoutRow(listOf(
            LayoutKey("Prev", keyCode = 0x50.toByte(), width = 1.5f),
            LayoutKey("Next", keyCode = 0x4F.toByte(), width = 1.5f),
        )),
        LayoutRow(listOf(
            LayoutKey("Start", keyCode = 0x4E.toByte()),
            LayoutKey("End", keyCode = 0x4D.toByte()),
        )),
        LayoutRow(listOf(
            LayoutKey("Play", keyCode = 0xB5.toByte(), width = 1.3f),
            LayoutKey("Pause", keyCode = 0xB3.toByte(), width = 1.3f),
            LayoutKey("F11", keyCode = 0x44.toByte()),
        )),
        LayoutRow(listOf(
            LayoutKey("Esc", keyCode = 0x29.toByte()),
            LayoutKey("FullScrn", keyCode = 0x77.toByte()),
            LayoutKey("Bright", keyCode = 0x7C.toByte()),
            LayoutKey("Power", keyCode = 0x66.toByte()),
        ))
    ), "presenter")

    val all = listOf(QWERTY, AZERTY, QWERTZ, DVORAK, COLEMAK, QWERTY_UK, WORKMAN,
        GAMING, MEDIA_PLAYER, NUMPAD, NAVIGATION, PRODUCTIVITY, SYSTEM_SHORTCUTS,
        QUICK_LAUNCH, BROWSER, FUNCTION_ROW,
        DEVELOPER_PRO, DESIGNER_PRO, OFFICE_PRO, GAMING_PRO,
        MOUSE_DEFAULT, NUMPAD_DEFAULT, MEDIA_DEFAULT, GAMEPAD_DEFAULT, PRESENTER_DEFAULT)
}
