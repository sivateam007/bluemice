package com.example.bluemice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.model.MacroStep
import com.example.bluemice.ui.theme.*

private val KC = HidReportBuilder.KeyCodes

data class PalKey(
    val label: String, val code: Byte = 0, val w: Float = 1f,
    val consumerCode: Int = 0, val mouseButton: Int = 0,
    val comboMods: Int = 0, val comboKey: Byte = 0,
    val mouseScroll: Int = 0, val gamepadButton: Int = 0,
    val isGamepad: Boolean = false,
    val keyType: String = "keyboard", val textBlock: String = "",
    val h: Float = 1f
)

private data class PalSection(val title: String, val keys: List<PalKey>)

class LayoutHistory {
    val undoStack = androidx.compose.runtime.snapshots.SnapshotStateList<List<List<LayoutKey>>>()
    val redoStack = androidx.compose.runtime.snapshots.SnapshotStateList<List<List<LayoutKey>>>()

    fun push(snapshot: List<List<LayoutKey>>) {
        undoStack.add(snapshot)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo(current: List<List<LayoutKey>>): List<List<LayoutKey>>? {
        val prev = undoStack.removeLastOrNull() ?: return null
        redoStack.add(current)
        return prev.map { it.toMutableList() }
    }

    fun redo(current: List<List<LayoutKey>>): List<List<LayoutKey>>? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.add(current)
        return next.map { it.toMutableList() }
    }

    fun clear() { undoStack.clear(); redoStack.clear() }
}

data class FnKey(val label: String, val description: String, val code: Byte = 0, val consumerCode: Int = 0)

private data class FnSection(val title: String, val icon: ImageVector, val keys: List<FnKey>)

val palColors = listOf(
    0xFF3A3A3A, 0xFF4A90D9, 0xFF8B3A3A, 0xFF3A8B3A,
    0xFF8B6F3A, 0xFF6A3A8B, 0xFF3A6F8B, 0xFF8B3A6F
)

private val letterKeys = ('A'..'Z').map { PalKey(it.toString(), keyCodeFor(it), 0.9f) }
private val numberKeys = (0..9).map { PalKey(it.toString(), (0x1E + it).toByte(), 0.9f) }
private val fnKeys = (1..12).map { PalKey("F$it", (KC.KEY_F1.toInt() + it - 1).toByte(), 0.8f) }
private val navKeys = listOf(
    PalKey("Tab", KC.KEY_TAB, 1.2f), PalKey("Caps", KC.KEY_CAPS_LOCK, 1.2f),
    PalKey("Shift", KC.KEY_LEFT_SHIFT, 1.4f), PalKey("Ctrl", (-32).toByte(), 1.2f),
    PalKey("Alt", (-30).toByte(), 1f), PalKey("Win", (-29).toByte(), 1.2f),
    PalKey("Space", KC.KEY_SPACE, 3f), PalKey("Enter", KC.KEY_ENTER, 1.4f),
    PalKey("BkSp", KC.KEY_BACKSPACE, 1.3f), PalKey("Del", KC.KEY_DELETE, 1f),
    PalKey("Ins", KC.KEY_INSERT, 1f), PalKey("Home", KC.KEY_HOME, 1f),
    PalKey("End", KC.KEY_END, 1f), PalKey("PgUp", KC.KEY_PAGE_UP, 1f),
    PalKey("PgDn", KC.KEY_PAGE_DOWN, 1f),
)
private val arrowKeys = listOf(
    PalKey("\u2191", KC.KEY_ARROW_UP, 0.8f), PalKey("\u2193", KC.KEY_ARROW_DOWN, 0.8f),
    PalKey("\u2190", KC.KEY_ARROW_LEFT, 0.8f), PalKey("\u2192", KC.KEY_ARROW_RIGHT, 0.8f),
)
private val punctKeys = listOf(
    PalKey(",", KC.KEY_COMMA), PalKey(".", KC.KEY_DOT),
    PalKey("/", KC.KEY_SLASH), PalKey(";", KC.KEY_SEMICOLON),
    PalKey("'", KC.KEY_QUOTE), PalKey("[", KC.KEY_LEFT_BRACKET),
    PalKey("]", KC.KEY_RIGHT_BRACKET), PalKey("\\", KC.KEY_BACKSLASH),
    PalKey("`", KC.KEY_GRAVE), PalKey("-", KC.KEY_MINUS),
    PalKey("=", KC.KEY_EQUAL),
)
private val specialKeys = listOf(
    PalKey("Esc", KC.KEY_ESCAPE, 0.8f), PalKey("PrtSc", KC.KEY_PRINT_SCREEN, 1f),
    PalKey("ScrLk", KC.KEY_SCROLL_LOCK, 1f), PalKey("Pause", KC.KEY_PAUSE, 1f),
    PalKey("NumLk", KC.KEY_NUM_LOCK, 1f),
)
private val mouseKeys = listOf(
    PalKey("Left Click", mouseButton = 1, w = 1.2f),
    PalKey("Right Click", mouseButton = 2, w = 1.2f),
    PalKey("Middle Click", mouseButton = 4, w = 1.2f),
    PalKey("Scrl\u2191", mouseScroll = 1, w = 1f),
    PalKey("Scrl\u2193", mouseScroll = -1, w = 1f),
)
private val mediaKeys = listOf(
    PalKey("PlayPause", consumerCode = 0x00CD, w = 1.4f),
    PalKey("Play", consumerCode = 0x00B0), PalKey("Pause", consumerCode = 0x00B1),
    PalKey("Stop", consumerCode = 0x00B7), PalKey("Next", consumerCode = 0x00B5),
    PalKey("Prev", consumerCode = 0x00B6), PalKey("Vol+", consumerCode = 0x00E9),
    PalKey("Vol\u2212", consumerCode = 0x00EA), PalKey("Mute", consumerCode = 0x00E2),
    PalKey("Br+", consumerCode = 0x006F), PalKey("Br\u2212", consumerCode = 0x0070),
    PalKey("Eject", consumerCode = 0x00B8), PalKey("Record", consumerCode = 0x00B2),
)
private val numpadKeys = listOf(
    PalKey("0", KC.KEY_KP_0), PalKey("1", KC.KEY_KP_1), PalKey("2", KC.KEY_KP_2),
    PalKey("3", KC.KEY_KP_3), PalKey("4", KC.KEY_KP_4), PalKey("5", KC.KEY_KP_5),
    PalKey("6", KC.KEY_KP_6), PalKey("7", KC.KEY_KP_7), PalKey("8", KC.KEY_KP_8),
    PalKey("9", KC.KEY_KP_9), PalKey(".", KC.KEY_KP_DOT), PalKey("+", KC.KEY_KP_PLUS),
    PalKey("\u2212", KC.KEY_KP_MINUS), PalKey("*", KC.KEY_KP_ASTERISK),
    PalKey("/", KC.KEY_KP_SLASH), PalKey("Enter", KC.KEY_KP_ENTER, 1.5f),
)
private val comboKeys = listOf(
    PalKey("Ctrl+Alt+Del", comboMods = 0x01 or 0x04, comboKey = KC.KEY_DELETE, w = 1.6f),
    PalKey("Ctrl+Shift+Esc", comboMods = 0x01 or 0x02, comboKey = KC.KEY_ESCAPE, w = 1.6f),
    PalKey("Alt+Tab", comboMods = 0x04, comboKey = KC.KEY_TAB, w = 1.1f),
    PalKey("Win+D", comboMods = 0x08, comboKey = KC.KEY_D, w = 1.4f),
    PalKey("Win+E", comboMods = 0x08, comboKey = KC.KEY_E, w = 1.4f),
    PalKey("Win+L", comboMods = 0x08, comboKey = KC.KEY_L, w = 1.3f),
    PalKey("Win+R", comboMods = 0x08, comboKey = KC.KEY_R, w = 1.2f),
    PalKey("Copy", comboMods = 0x01, comboKey = KC.KEY_C, w = 0.8f),
    PalKey("Cut", comboMods = 0x01, comboKey = KC.KEY_X, w = 0.7f),
    PalKey("Paste", comboMods = 0x01, comboKey = KC.KEY_V, w = 0.9f),
    PalKey("Undo", comboMods = 0x01, comboKey = KC.KEY_Z, w = 0.8f),
    PalKey("Redo", comboMods = 0x01, comboKey = KC.KEY_Y, w = 0.8f),
    PalKey("SelAll", comboMods = 0x01, comboKey = KC.KEY_A, w = 0.9f),
    PalKey("Save", comboMods = 0x01, comboKey = KC.KEY_S, w = 0.8f),
    PalKey("Find", comboMods = 0x01, comboKey = KC.KEY_F, w = 0.8f),
)

private val gamepadKeys = listOf(
    PalKey("A", gamepadButton = 0, isGamepad = true),
    PalKey("B", gamepadButton = 1, isGamepad = true),
    PalKey("X", gamepadButton = 2, isGamepad = true),
    PalKey("Y", gamepadButton = 3, isGamepad = true),
    PalKey("LB", gamepadButton = 4, w = 1.2f, isGamepad = true),
    PalKey("RB", gamepadButton = 5, w = 1.2f, isGamepad = true),
    PalKey("LT", gamepadButton = 6, w = 1.2f, isGamepad = true),
    PalKey("RT", gamepadButton = 7, w = 1.2f, isGamepad = true),
    PalKey("Back", gamepadButton = 8, w = 1f, isGamepad = true),
    PalKey("Start", gamepadButton = 9, w = 1f, isGamepad = true),
    PalKey("LStick", gamepadButton = 10, w = 1.2f, isGamepad = true),
    PalKey("RStick", gamepadButton = 11, w = 1.2f, isGamepad = true),
    PalKey("DPad\u2191", gamepadButton = 12, isGamepad = true),
    PalKey("DPad\u2193", gamepadButton = 14, isGamepad = true),
    PalKey("DPad\u2190", gamepadButton = 13, isGamepad = true),
    PalKey("DPad\u2192", gamepadButton = 15, isGamepad = true),
    PalKey("Analog L", keyType = "stickL", w = 2f, h = 2f),
    PalKey("Analog R", keyType = "stickR", w = 2f, h = 2f),
)

// Modifiers (left/right) - use comboMods since HID encodes L/R in modifier byte
private val modifierKeys = listOf(
    PalKey("L-Ctrl", comboMods = KC.MOD_LEFT_CTRL, w = 1.2f),
    PalKey("R-Ctrl", comboMods = KC.MOD_RIGHT_CTRL, w = 1.2f),
    PalKey("L-Shift", comboMods = KC.MOD_LEFT_SHIFT, w = 1.4f),
    PalKey("R-Shift", comboMods = KC.MOD_RIGHT_SHIFT, w = 1.4f),
    PalKey("L-Alt", comboMods = KC.MOD_LEFT_ALT, w = 1f),
    PalKey("R-Alt", comboMods = KC.MOD_RIGHT_ALT, w = 1f),
    PalKey("L-Win", comboMods = KC.MOD_LEFT_META, w = 1.2f),
    PalKey("R-Win", comboMods = KC.MOD_RIGHT_META, w = 1.2f),
)

// System / OS keys
private val systemKeys = listOf(
    PalKey("Esc", KC.KEY_ESCAPE, 0.8f),
    PalKey("PrtSc", KC.KEY_PRINT_SCREEN, 1f),
    PalKey("ScrLk", KC.KEY_SCROLL_LOCK, 1f),
    PalKey("Pause", KC.KEY_PAUSE, 1f),
    PalKey("Ins", KC.KEY_INSERT, 1f),
    PalKey("Del", KC.KEY_DELETE, 1f),
    PalKey("Home", KC.KEY_HOME, 1f),
    PalKey("End", KC.KEY_END, 1f),
    PalKey("PgUp", KC.KEY_PAGE_UP, 1f),
    PalKey("PgDn", KC.KEY_PAGE_DOWN, 1f),
    PalKey("Sleep", KC.KEY_SLEEP, 1f),
    PalKey("Wake", KC.KEY_WAKE, 1f),
)

// Browser / App shortcuts (common consumer codes)
private val browserKeys = listOf(
    PalKey("Back", consumerCode = 0x0224, w = 1f),   // AC Back
    PalKey("Forward", consumerCode = 0x0225, w = 1f), // AC Forward
    PalKey("Refresh", consumerCode = 0x0227, w = 1f), // AC Refresh
    PalKey("Stop", consumerCode = 0x0226, w = 1f),    // AC Stop
    PalKey("Search", consumerCode = 0x0221, w = 1f),  // AC Search
    PalKey("Favorites", consumerCode = 0x022A, w = 1f), // AC Favorites
    PalKey("Home", consumerCode = 0x0223, w = 1f),    // AC Home
    PalKey("Mail", consumerCode = 0x018A, w = 1f),    // AL Email Reader
    PalKey("Calc", consumerCode = 0x0192, w = 1f),    // AL Calculator
    PalKey("MyPC", consumerCode = 0x0194, w = 1f),    // AL Local Machine Browser
)

// Text/Script templates
private val textTemplates = listOf(
    PalKey("Lorem Ipsum", keyType = "textBlock", textBlock = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.", w = 2f),
    PalKey("Email Sig", keyType = "textBlock", textBlock = "Best regards,\n[Your Name]\n[Title]\n[Company]\n[Phone]", w = 2f),
    PalKey("Date/Time", keyType = "textBlock", textBlock = "[[DATE]] [[TIME]]", w = 1.5f),
    PalKey("UUID", keyType = "textBlock", textBlock = "[[UUID]]", w = 1.5f),
)

// Custom / User templates placeholder
private val customKeys = listOf<PalKey>()

private val fnSections = listOf(
    FnSection("Function Keys", Icons.Default.Extension, listOf(
        FnKey("F1", "Help / System Info", KC.KEY_F1),
        FnKey("F2", "Rename", KC.KEY_F2),
        FnKey("F3", "Search", KC.KEY_F3),
        FnKey("F4", "Close Window", KC.KEY_F4),
        FnKey("F5", "Refresh", KC.KEY_F5),
        FnKey("F6", "Address Bar", KC.KEY_F6),
        FnKey("F7", "Caret / Spell Check", KC.KEY_F7),
        FnKey("F8", "Safe Mode", KC.KEY_F8),
        FnKey("F9", "Menu Key", KC.KEY_F9),
        FnKey("F10", "Menu Bar", KC.KEY_F10),
        FnKey("F11", "Fullscreen Toggle", KC.KEY_F11),
        FnKey("F12", "Developer Tools", KC.KEY_F12),
    )),
    FnSection("Media", Icons.Default.PlayArrow, listOf(
        FnKey("Play / Pause", "Play or pause media", consumerCode = 0x00CD),
        FnKey("Stop", "Stop playback", consumerCode = 0x00B7),
        FnKey("Next", "Next track", consumerCode = 0x00B5),
        FnKey("Previous", "Previous track", consumerCode = 0x00B6),
        FnKey("Volume +", "Increase volume", consumerCode = 0x00E9),
        FnKey("Volume -", "Decrease volume", consumerCode = 0x00EA),
        FnKey("Mute", "Toggle mute", consumerCode = 0x00E2),
    )),
    FnSection("Brightness", Icons.Default.WbSunny, listOf(
        FnKey("Brightness +", "Increase brightness", consumerCode = 0x006F),
        FnKey("Brightness -", "Decrease brightness", consumerCode = 0x0070),
    )),
    FnSection("Navigation", Icons.Default.Keyboard, listOf(
        FnKey("Up", "Arrow Up", KC.KEY_ARROW_UP),
        FnKey("Down", "Arrow Down", KC.KEY_ARROW_DOWN),
        FnKey("Left", "Arrow Left", KC.KEY_ARROW_LEFT),
        FnKey("Right", "Arrow Right", KC.KEY_ARROW_RIGHT),
        FnKey("Enter", "Confirm / Enter", KC.KEY_ENTER),
        FnKey("Backspace", "Delete previous char", KC.KEY_BACKSPACE),
        FnKey("Home", "Go to beginning", KC.KEY_HOME),
        FnKey("End", "Go to end", KC.KEY_END),
        FnKey("PgUp", "Page up", KC.KEY_PAGE_UP),
        FnKey("PgDn", "Page down", KC.KEY_PAGE_DOWN),
    )),
)

private fun keyCodeFor(c: Char): Byte = when (c) {
    'A' -> 0x04; 'B' -> 0x05; 'C' -> 0x06; 'D' -> 0x07; 'E' -> 0x08
    'F' -> 0x09; 'G' -> 0x0A; 'H' -> 0x0B; 'I' -> 0x0C; 'J' -> 0x0D
    'K' -> 0x0E; 'L' -> 0x0F; 'M' -> 0x10; 'N' -> 0x11; 'O' -> 0x12
    'P' -> 0x13; 'Q' -> 0x14; 'R' -> 0x15; 'S' -> 0x16; 'T' -> 0x17
    'U' -> 0x18; 'V' -> 0x19; 'W' -> 0x1A; 'X' -> 0x1B; 'Y' -> 0x1C
    'Z' -> 0x1D; else -> 0
}

internal fun palKeyToLayoutKey(pk: PalKey) = LayoutKey(
    label = pk.label, keyCode = pk.code, width = pk.w,
    consumerCode = pk.consumerCode, mouseButton = pk.mouseButton,
    comboMods = pk.comboMods, comboKey = pk.comboKey,
    mouseScroll = pk.mouseScroll, gamepadButton = pk.gamepadButton,
    isGamepad = pk.isGamepad,
    keyType = pk.keyType, textBlock = pk.textBlock,
    height = pk.h,
    backgroundColor = when {
        pk.keyType == "stickL" || pk.keyType == "stickR" -> 0xFF14202E
        pk.consumerCode in 0x00B0..0x00CD -> 0xFF3A4A3A
        pk.consumerCode in 0x00E9..0x00EA -> 0xFF2A3A4A
        pk.consumerCode in 0x006F..0x0070 -> 0xFF3A3A2A
        else -> 0xFF3A3A3A
    }
)

private fun fnKeyToLayoutKey(fk: FnKey) = LayoutKey(
    label = fk.label, keyCode = fk.code, consumerCode = fk.consumerCode,
    width = 1.2f,
    backgroundColor = when {
        fk.label.startsWith("F") -> 0xFF4A3A3A
        fk.consumerCode in 0x00B0..0x00CD -> 0xFF3A4A3A
        fk.consumerCode in 0x0060..0x00EA -> 0xFF3A3A4A
        else -> 0xFF3A3A3A
    }
)

private val allPalSections = listOf(
    PalSection("Letters", letterKeys),
    PalSection("Numbers", numberKeys),
    PalSection("Function", fnKeys),
    PalSection("Numpad", numpadKeys),
    PalSection("Navigation", navKeys),
    PalSection("Arrows", arrowKeys),
    PalSection("Modifiers", modifierKeys),
    PalSection("System", systemKeys),
    PalSection("Punctuation", punctKeys),
    PalSection("Browser", browserKeys),
    PalSection("Mouse", mouseKeys),
    PalSection("Media", mediaKeys),
    PalSection("Combos", comboKeys),
    PalSection("Gamepad", gamepadKeys),
    PalSection("Text Templates", textTemplates),
)

/** Display names for the 16 digital gamepad buttons, in bit order. */
private val gamepadButtonLabels = mapOf(
    0 to "A", 1 to "B", 2 to "X", 3 to "Y",
    4 to "LB", 5 to "RB", 6 to "LT", 7 to "RT",
    8 to "Sel", 9 to "St", 10 to "L3", 11 to "R3",
    12 to "D↑", 13 to "D←", 14 to "D↓", 15 to "D→"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditModeBar(
    layoutName: String,
    layoutNames: List<String>,
    hasKeys: Boolean,
    onLoadLayout: (String) -> Unit,
    onSave: () -> Unit,
    onAddKey: () -> Unit,
    onAddRow: () -> Unit,
    onImport: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onNewBlank: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    onExit: () -> Unit
) {
    var showLoadMenu by remember { mutableStateOf(false) }

    // Save and Done are pinned outside the scrollable group. This bar carries
    // ~10 controls on a phone; letting them all run to the right edge is what
    // pushed Save off screen in the first place.
    Row(Modifier.fillMaxWidth().background(CardSurface)
        .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A3A))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showLoadMenu = true }
            .padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(layoutName.ifEmpty { "New Layout" }, color = TextSecondary, fontSize = 11.sp, maxLines = 1,
                    modifier = Modifier.width(64.dp))
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.NavigateNext, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = showLoadMenu, onDismissRequest = { showLoadMenu = false }) {
                DropdownMenuItem(
                    text = { Text("+ New Blank Layout", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) },
                    onClick = { onNewBlank(); showLoadMenu = false })
                if (layoutNames.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A3A4A)))
                }
                layoutNames.forEach { name ->
                    DropdownMenuItem(text = { Text(name, fontSize = 12.sp) },
                        onClick = { onLoadLayout(name); showLoadMenu = false })
                }
                if (layoutNames.isEmpty()) {
                    DropdownMenuItem(text = { Text("No saved layouts yet", fontSize = 12.sp, color = TextSecondary) },
                        onClick = { showLoadMenu = false }, enabled = false)
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text("|", color = TextSecondary.copy(alpha = 0.3f), fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A3A2A))
            .clickable { onAddKey() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Key", color = Color(0xFF4CAF50), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A3A))
            .clickable { onAddRow() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Row", color = TextSecondary, fontSize = 11.sp)
            }
        }
        if (onImport != null) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A3A4A))
                .clickable { onImport() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Import", color = Color(0xFF64B5F6), fontSize = 11.sp)
                }
            }
        }
        if (onExport != null) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A3A4A))
                .clickable { onExport() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Export", color = Color(0xFF64B5F6), fontSize = 11.sp)
                }
            }
        }
        if (onUndo != null) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (canUndo) Color(0xFF2A2A4A) else Color(0xFF222230))
                .clickable(enabled = canUndo) { onUndo() }.padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Undo, "Undo", tint = if (canUndo) Color(0xFFB39DDB) else Color(0xFF555566), modifier = Modifier.size(14.dp))
            }
        }
        if (onRedo != null) {
            Spacer(Modifier.width(2.dp))
            Box(Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (canRedo) Color(0xFF2A2A4A) else Color(0xFF222230))
                .clickable(enabled = canRedo) { onRedo() }.padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Redo, "Redo", tint = if (canRedo) Color(0xFFB39DDB) else Color(0xFF555566), modifier = Modifier.size(14.dp))
            }
        }
        }
        // Always offer Save so a deliberately blank layout can be persisted
        // (previously hidden when empty, which trapped blank layouts).
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A4A2A))
            .clickable { onSave() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Save", color = Color(0xFF4CAF50), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A1A1A))
            .clickable { onExit() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Done", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeySheet(
    onAddPaletteKey: (PalKey) -> Unit,
    onAddFnKey: (FnKey) -> Unit,
    onAddTextBlock: (String) -> Unit,
    onAddSpecial: (type: String, label: String, bgColor: Long) -> Unit,
    onAddMacro: (List<MacroStep>) -> Unit,
    onAddCustomKey: (label: String, keyCode: Byte, consumerCode: Int, mouseButton: Int) -> Unit = { _, _, _, _ -> },
    onAddTemplate: (templateName: String) -> Unit = {},
    onDismiss: () -> Unit,
    mode: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showFunctionsSheet by remember { mutableStateOf(false) }
    var showTextBlockInput by remember { mutableStateOf(false) }
    var showMacroEditor by remember { mutableStateOf(false) }
    var showMouseSheet by remember { mutableStateOf(false) }
    var showGamepadSheet by remember { mutableStateOf(false) }
    var textBlockInput by remember { mutableStateOf("") }
    var macroSteps by remember { mutableStateOf(listOf<MacroStep>()) }
    var showCustomKeyDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (mode == "mouse") "Add Mouse Control" else "Add Key", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (mode == "mouse") {
                // Touchpad
                Text("Touchpad", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2D2D2D))
                    .clickable {
                        onAddSpecial("touchpad", "Touchpad", 0xFF3A3A3A)
                        onDismiss()
                    }, contentAlignment = Alignment.Center) {
                    Text("Touchpad Surface", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                MousePaletteSection("Buttons & Scroll", mouseActions, onAddPaletteKey, onDismiss)
                MousePaletteSection("Arrows", arrowKeys, onAddPaletteKey, onDismiss)
                MousePaletteSection("Media & Brightness", mediaKeys, onAddPaletteKey, onDismiss)
                MousePaletteSection("Window Shortcuts", comboKeys, onAddPaletteKey, onDismiss)
                MousePaletteSection("Browser & Apps", browserKeys, onAddPaletteKey, onDismiss)
                MousePaletteSection("Function Keys", fnKeys, onAddPaletteKey, onDismiss)
                MousePaletteSection("Navigation", navKeys, onAddPaletteKey, onDismiss)

                Text("Components", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.TextSnippet, "Text Block", Periwinkle) {
                        textBlockInput = ""; showTextBlockInput = true
                    }
                    CategoryCard(Icons.Default.Mic, "Speech", Color(0xFF4A90D9)) {
                        onAddSpecial("speech", "Speech", 0xFF3A4A6F)
                        onDismiss()
                    }
                    CategoryCard(Icons.Default.Visibility, "Kbd Toggle", Color(0xFF8B3A6F)) {
                        onAddSpecial("keyboard_toggle", "Kbd Toggle", 0xFF4A3A4A)
                        onDismiss()
                    }
                    CategoryCard(Icons.Default.Fullscreen, "Fullscreen", Color(0xFF3A8B3A)) {
                        onAddSpecial("fullscreen_toggle", "Fullscreen", 0xFF3A4A3A)
                        onDismiss()
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.Build, "Macros", Color(0xFF4A90D9)) {
                        macroSteps = emptyList(); showMacroEditor = true
                    }
                    CategoryCard(Icons.Default.Keyboard, "Keyboard", MaterialTheme.colorScheme.primary) {
                        showPaletteSheet = true
                    }
                    CategoryCard(Icons.Default.Extension, "Functions", Color(0xFF8B6F3A)) {
                        showFunctionsSheet = true
                    }
                    CategoryCard(Icons.Default.PlayArrow, "Gamepad", Color(0xFF4A90D9)) {
                        showGamepadSheet = true
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.AddCircle, "Custom Key", Color(0xFF4CAF50)) {
                        showCustomKeyDialog = true
                    }
                    CategoryCard(Icons.Default.Widgets, "Template", Periwinkle) {
                        showTemplatePicker = true
                    }
                }
            } else {
                // Category cards in 2 rows
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.Keyboard, "Keyboard", MaterialTheme.colorScheme.primary) {
                        showPaletteSheet = true
                    }
                    CategoryCard(Icons.Default.TextSnippet, "Text Block", Periwinkle) {
                        textBlockInput = ""; showTextBlockInput = true
                    }
                    CategoryCard(Icons.Default.Extension, "Functions", Color(0xFF8B6F3A)) {
                        showFunctionsSheet = true
                    }
                    CategoryCard(Icons.Default.TouchApp, "Touchpad", Color(0xFF8B6F3A)) {
                        onAddSpecial("touchpad", "Touchpad", 0xFF3A3A3A)
                        onDismiss()
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.Mic, "Speech", Color(0xFF4A90D9)) {
                        onAddSpecial("speech", "Speech", 0xFF3A4A6F)
                        onDismiss()
                    }
                    CategoryCard(Icons.Default.Visibility, "Kbd Toggle", Color(0xFF8B3A6F)) {
                        onAddSpecial("keyboard_toggle", "Kbd Toggle", 0xFF4A3A4A)
                        onDismiss()
                    }
                    CategoryCard(Icons.Default.Fullscreen, "Fullscreen", Color(0xFF3A8B3A)) {
                        onAddSpecial("fullscreen_toggle", "Fullscreen", 0xFF3A4A3A)
                        onDismiss()
                    }
                    CategoryCard(Icons.Default.Build, "Macros", Color(0xFF4A90D9)) {
                        macroSteps = emptyList(); showMacroEditor = true
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.TouchApp, "Mouse", Color(0xFF3A8B3A)) {
                        showMouseSheet = true
                    }
                    CategoryCard(Icons.Default.PlayArrow, "Gamepad", Color(0xFF4A90D9)) {
                        showGamepadSheet = true
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryCard(Icons.Default.AddCircle, "Custom Key", Color(0xFF4CAF50)) {
                        showCustomKeyDialog = true
                    }
                    CategoryCard(Icons.Default.Widgets, "Template", Periwinkle) {
                        showTemplatePicker = true
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showPaletteSheet) {
        KeyboardPaletteSheet(onAdd = { pk ->
            onAddPaletteKey(pk)
            showPaletteSheet = false
            onDismiss()
        }, onDismiss = { showPaletteSheet = false })
    }

    if (showFunctionsSheet) {
        FunctionsSheet(onAdd = { fk ->
            onAddFnKey(fk)
            showFunctionsSheet = false
            onDismiss()
        }, onDismiss = { showFunctionsSheet = false })
    }

    if (showMouseSheet) {
        MouseActionsSheet(
            onAddPaletteKey = { pk ->
                onAddPaletteKey(pk)
                showMouseSheet = false
                onDismiss()
            },
            onAddSpecial = { type, label, bg ->
                onAddSpecial(type, label, bg)
                showMouseSheet = false
                onDismiss()
            },
            onDismiss = { showMouseSheet = false }
        )
    }

    if (showGamepadSheet) {
        GamepadPaletteSheet(onAdd = { pk ->
            onAddPaletteKey(pk)
            showGamepadSheet = false
            onDismiss()
        }, onDismiss = { showGamepadSheet = false })
    }

    if (showTextBlockInput) {
        TextBlockDialog(text = textBlockInput, onTextChange = { textBlockInput = it },
            onAdd = {
                if (it.isNotBlank()) {
                    onAddTextBlock(it); showTextBlockInput = false; onDismiss()
                }
            }, onDismiss = { showTextBlockInput = false })
    }

    if (showMacroEditor) {
        MacroEditorSheet(steps = macroSteps, onStepsChange = { macroSteps = it },
            onAdd = {
                if (it.isNotEmpty()) { onAddMacro(it); showMacroEditor = false; onDismiss() }
            }, onDismiss = { showMacroEditor = false })
    }

    if (showCustomKeyDialog) {
        CustomKeyDialog(
            onAdd = { label, keyCode, consumerCode, mouseButton ->
                onAddCustomKey(label, keyCode, consumerCode, mouseButton)
                showCustomKeyDialog = false
                onDismiss()
            },
            onDismiss = { showCustomKeyDialog = false }
        )
    }

    if (showTemplatePicker) {
        TemplatePickerDialog(
            mode = mode,
            onSelect = { name ->
                showTemplatePicker = false
                onAddTemplate(name)
                onDismiss()
            },
            onDismiss = { showTemplatePicker = false }
        )
    }
}

@Composable
private fun MousePaletteSection(
    title: String,
    keys: List<PalKey>,
    onAddPaletteKey: (PalKey) -> Unit,
    onDismiss: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(5.dp))
        Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        keys.forEach { pk ->
            val bw = (34 + pk.w.coerceAtLeast(0.5f) * 46).dp
            Box(Modifier.width(bw).height(36.dp).clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF2D2D2D))
                .clickable {
                    onAddPaletteKey(pk)
                    onDismiss()
                }, contentAlignment = Alignment.Center) {
                Text(pk.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RowScope.CategoryCard(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A3A3A))
        .clickable(onClick = onClick).padding(8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardPaletteSheet(onAdd: (PalKey) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keyboard", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            allPalSections.forEach { section ->
                Text(section.title, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    section.keys.forEach { pk ->
                        val bw = (pk.w.coerceAtLeast(0.5f) * 40).dp
                        Box(Modifier.width(bw).height(34.dp).clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF2D2D2D))
                            .clickable { onAdd(pk) }, contentAlignment = Alignment.Center) {
                            Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionsSheet(onAdd: (FnKey) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Functions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            fnSections.forEach { section ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(section.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(section.title, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                section.keys.chunked(3).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chunk.forEach { fk ->
                            val fg = Color(fnKeyToLayoutKey(fk).backgroundColor)
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(fg)
                                .clickable { onAdd(fk) }.padding(6.dp)) {
                                Column {
                                    Text(fk.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(fk.description, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                        repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val mouseActions = listOf(
    PalKey("Left Click", mouseButton = 1, w = 1.2f),
    PalKey("Middle Click", mouseButton = 4, w = 1.2f),
    PalKey("Right Click", mouseButton = 2, w = 1.2f),
    PalKey("Scrl\u2191", mouseScroll = 1),
    PalKey("Scrl\u2193", mouseScroll = -1),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MouseActionsSheet(
    onAddPaletteKey: (PalKey) -> Unit,
    onAddSpecial: (type: String, label: String, bgColor: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mouse Actions", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            // Mouse buttons + scroll
            Text("Buttons & Scroll", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                mouseActions.forEach { pk ->
                    val bw = (pk.w.coerceAtLeast(0.5f) * 40).dp
                    Box(Modifier.width(bw).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAddPaletteKey(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            // Touchpad
            Text("Touchpad", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2D2D2D))
                .clickable { onAddSpecial("touchpad", "Touchpad", 0xFF3A3A3A) }, contentAlignment = Alignment.Center) {
                Text("Touchpad Surface", color = TextPrimary, fontSize = 10.sp)
            }

            // Arrow keys
            Text("Arrows", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                arrowKeys.forEach { pk ->
                    val bw = (pk.w.coerceAtLeast(0.5f) * 40).dp
                    Box(Modifier.width(bw).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAddPaletteKey(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamepadPaletteSheet(
    onAdd: (PalKey) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gamepad Buttons", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            // ABXY
            Text("Buttons", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gamepadKeys.take(4).forEach { pk ->
                    Box(Modifier.width(40.dp).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAdd(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            // Triggers/Bumpers
            Text("Triggers & Bumpers", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gamepadKeys.drop(4).take(4).forEach { pk ->
                    val bw = (pk.w.coerceAtLeast(0.5f) * 40).dp
                    Box(Modifier.width(bw).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAdd(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            // Select / Start
            Text("System", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gamepadKeys.drop(8).take(2).forEach { pk ->
                    Box(Modifier.width(40.dp).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAdd(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            // D-Pad
            Text("D-Pad", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                gamepadKeys.drop(12).take(4).forEach { pk ->
                    Box(Modifier.width(40.dp).height(34.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF2D2D2D))
                        .clickable { onAdd(pk) }, contentAlignment = Alignment.Center) {
                        Text(pk.label, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKeySheet(
    key: LayoutKey,
    onApply: (LayoutKey) -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onEditMacro: (List<MacroStep>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editLabel by remember { mutableStateOf(key.label) }
    var editWidth by remember { mutableStateOf(key.width) }
    var editHeight by remember { mutableStateOf(key.height) }
    var editBg by remember { mutableStateOf(key.backgroundColor) }
    var editTc by remember { mutableStateOf(key.textColor) }
    var editKeyType by remember { mutableStateOf(key.keyType) }
    var editKeyCode by remember { mutableStateOf(key.keyCode) }
    var editConsumerCode by remember { mutableStateOf(key.consumerCode) }
    var editMouseButton by remember { mutableStateOf(key.mouseButton) }
    var editComboMods by remember { mutableStateOf(key.comboMods) }
    var editComboKey by remember { mutableStateOf(key.comboKey) }
    var editMouseScroll by remember { mutableStateOf(key.mouseScroll) }
    var editGamepadButton by remember { mutableStateOf(key.gamepadButton) }
    var editIsGamepad by remember { mutableStateOf(key.isGamepad) }
    var editTextBlock by remember { mutableStateOf(key.textBlock) }
    var showMacroEditor by remember { mutableStateOf(false) }
    var macroSteps by remember { mutableStateOf(key.macroSteps) }
    var showHexInput by remember { mutableStateOf(false) }
    var hexCode by remember { mutableStateOf(if (key.keyCode != 0.toByte()) "0x${key.keyCode.toUByte().toString(16).padStart(2,'0').uppercase()}" else "") }
    var editRepeat by remember { mutableStateOf(key.repeat) }
    var editFullWidth by remember { mutableStateOf(key.fullWidth) }
    var editAlign by remember { mutableStateOf(key.align) }
    var editLevels by remember { mutableStateOf(key.levels) }
    var selectedTab by remember { mutableIntStateOf(0) }

    @Composable
    fun FunctionContent() {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("keyboard", "textBlock", "touchpad", "speech", "keyboard_toggle", "fullscreen_toggle").forEach { t ->
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (editKeyType == t) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                    .clickable { editKeyType = t }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(t.replace("_", " "), color = if (editKeyType == t) Color.White else TextSecondary, fontSize = 9.sp)
                }
            }
        }

        OutlinedTextField(value = editLabel, onValueChange = { editLabel = it.take(16) },
            label = { Text("Label") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))

        if (editKeyType == "textBlock") {
            Text("TextBlock: ${editTextBlock.take(30)}", color = TextSecondary, fontSize = 11.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Width", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(0.5f, 0.8f, 1f, 1.5f, 2f, 3f).forEach { w ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editWidth == w) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                        .clickable { editWidth = w }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${w}x", color = if (editWidth == w) Color.White else TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ht", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(0.8f, 1f, 1.5f, 2f).forEach { h ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editHeight == h) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                        .clickable { editHeight = h }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${h}x", color = if (editHeight == h) Color.White else TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Color", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                palColors.forEach { c ->
                    val isSelected = editBg == c
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Color(c))
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                        .clickable { editBg = c })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Text", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editTc == null) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                    .clickable { editTc = null }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Auto", color = if (editTc == null) Color.White else TextSecondary, fontSize = 10.sp)
                }
                palColors.forEach { c ->
                    val isSelected = editTc == c
                    Box(Modifier.size(22.dp).clip(CircleShape).background(Color(c))
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                        .clickable { editTc = c })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Full W", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editFullWidth) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                .clickable { editFullWidth = !editFullWidth }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(if (editFullWidth) "Yes" else "No", color = if (editFullWidth) Color.White else TextSecondary, fontSize = 10.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Align", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("left", "center", "right").forEach { a ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editAlign == a) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                        .clickable { editAlign = a }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(a, color = if (editAlign == a) Color.White else TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hold", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (editRepeat) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                .clickable { editRepeat = !editRepeat }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(if (editRepeat) "Repeat On Hold" else "Tap Only", color = if (editRepeat) Color.White else TextSecondary, fontSize = 10.sp)
            }
        }

        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A3A))
            .clickable { showHexInput = !showHexInput }.padding(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Key Code: ${"0x%02X".format(editKeyCode)}", color = TextSecondary, fontSize = 10.sp)
                Spacer(Modifier.width(4.dp))
                Text(if (showHexInput) "-" else "+", color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (showHexInput) {
            OutlinedTextField(value = hexCode, onValueChange = {
                hexCode = it
                val v = it.removePrefix("0x").removePrefix("0X").toIntOrNull(16)
                if (v != null && v in 0..255) editKeyCode = v.toByte()
            }, label = { Text("Hex (e.g. 0x2C)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
        }
    }

    @Composable
    fun ShortcutContent() {
        fun comboPreview(): String {
            val names = mutableListOf<String>()
            if (editComboMods and KC.MOD_LEFT_CTRL != 0) names.add("Ctrl")
            if (editComboMods and KC.MOD_LEFT_SHIFT != 0) names.add("Shift")
            if (editComboMods and KC.MOD_LEFT_ALT != 0) names.add("Alt")
            if (editComboMods and KC.MOD_LEFT_META != 0) names.add("Win")
            names.add(shortcutKeyName(editComboKey))
            return names.joinToString("+")
        }
        val mods = listOf(
            "Ctrl" to KC.MOD_LEFT_CTRL,
            "Shift" to KC.MOD_LEFT_SHIFT,
            "Alt" to KC.MOD_LEFT_ALT,
            "Win" to KC.MOD_LEFT_META
        )
        Text("Shortcut: ${comboPreview()}", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            mods.forEach { (label, bit) ->
                val active = editComboMods and bit != 0
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                    .clickable { editComboMods = if (active) editComboMods and bit.inv() else editComboMods or bit }
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(label, color = if (active) Color.White else TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Key", color = TextSecondary, fontSize = 11.sp)
        val pick = (letterKeys + numberKeys + fnKeys + navKeys)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pick.forEach { pk ->
                val active = editComboKey == pk.code
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                    .clickable { editComboKey = pk.code }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(pk.label, color = if (active) Color.White else TextSecondary, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Sends the shortcut (e.g. Ctrl+Shift+A) when tapped.", color = TextSecondary, fontSize = 10.sp)
    }

    @Composable
    fun TextContent() {
        OutlinedTextField(value = editTextBlock, onValueChange = { editTextBlock = it },
            label = { Text("Text to type") }, modifier = Modifier.fillMaxWidth().height(96.dp),
            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
        Spacer(Modifier.height(4.dp))
        Text("The text is typed on the PC when this key is tapped.", color = TextSecondary, fontSize = 10.sp)
        if (editTextBlock.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .clickable {
                    editKeyType = "textBlock"
                    if (editLabel.isBlank() || editLabel == key.label) editLabel = "Text"
                }
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text("Use as Text Key", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    /**
     * Per-key gamepad assignment: picks which of the 16 digital buttons this
     * key sends. Marking it as a gamepad key routes it to the gamepad report
     * instead of the keyboard report.
     */
    @Composable
    fun GamepadContent() {
        Text("Gamepad button", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..15).forEach { bit ->
                val label = gamepadButtonLabels[bit] ?: bit.toString()
                val selected = editIsGamepad && editGamepadButton == bit
                Box(Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                    .clickable {
                        editIsGamepad = true
                        editGamepadButton = bit
                        if (editLabel.isBlank() || editLabel == key.label) editLabel = label
                        editKeyType = "keyboard"
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Send as gamepad button", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = editIsGamepad,
                onCheckedChange = { editIsGamepad = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (editIsGamepad) "Tapping this key presses the ${gamepadButtonLabels[editGamepadButton] ?: editGamepadButton} button on the gamepad."
            else "Tapping this key types on the keyboard instead.",
            color = TextSecondary, fontSize = 10.sp
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Edit Key", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E2E),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Function", fontSize = 11.sp) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Shortcut", fontSize = 11.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("Text", fontSize = 11.sp) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    text = { Text("Gamepad", fontSize = 11.sp) })
            }

            when (selectedTab) {
                0 -> FunctionContent()
                1 -> ShortcutContent()
                2 -> TextContent()
                3 -> GamepadContent()
            }

            // Move / Delete buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A))
                    .clickable { onMoveLeft() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NavigateBefore, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text("Move Left", color = TextSecondary, fontSize = 10.sp)
                    }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A))
                    .clickable { onMoveRight() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Move Right", color = TextSecondary, fontSize = 10.sp)
                        Icon(Icons.Default.NavigateNext, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Edit Macros button (if key has macros)
            if (key.macroSteps.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A4A))
                    .clickable { showMacroEditor = true }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, null, tint = Color(0xFF4A90D9), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit Macros (${key.macroSteps.size} steps)", color = Color(0xFF4A90D9), fontSize = 11.sp)
                    }
                }
            }

            // Apply / Delete
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = {
                    onApply(key.copy(label = editLabel, width = editWidth, height = editHeight,
                        backgroundColor = editBg, textColor = editTc, keyType = editKeyType,
                        keyCode = editKeyCode, consumerCode = editConsumerCode,
                        mouseButton = editMouseButton, comboMods = editComboMods,
                        comboKey = editComboKey, mouseScroll = editMouseScroll,
                        gamepadButton = editGamepadButton, isGamepad = editIsGamepad,
                        textBlock = editTextBlock,
                        repeat = editRepeat, fullWidth = editFullWidth, align = editAlign,
                        levels = editLevels, macroSteps = macroSteps)
)


                }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Apply", color = Color.White, fontSize = 12.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A1A1A))
                    .clickable { onDelete() }.padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showMacroEditor) {
        MacroEditorSheet(steps = macroSteps, onStepsChange = { macroSteps = it },
            onAdd = { showMacroEditor = false },
            onDismiss = { showMacroEditor = false })
    }
}

private fun shortcutKeyName(code: Byte): String {
    val v = code.toInt() and 0xFF
    return when (v) {
        0x28 -> "Enter"
        0x29 -> "Esc"
        0x2C -> "Space"
        0x2A -> "Backspace"
        0x2B -> "Tab"
        0x4C -> "Delete"
        0x49 -> "Insert"
        0x4A -> "Home"
        0x4D -> "End"
        0x4B -> "PageUp"
        0x4E -> "PageDown"
        0x39 -> "CapsLock"
        else -> when {
            v in 0x04..0x1D -> ('A'.code + v - 0x04).toChar().toString()
            v in 0x1E..0x26 -> "${v - 0x1E + 1}"
            v == 0x27 -> "0"
            v in 0x3A..0x45 -> "F${v - 0x3A + 1}"
            else -> "0x%02X".format(v)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MacroEditorSheet(
    steps: List<MacroStep>,
    onStepsChange: (List<MacroStep>) -> Unit,
    onAdd: (List<MacroStep>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStepEditor by remember { mutableStateOf(false) }
    var stepType by remember { mutableStateOf("keystroke") }
    var stepKeyCode by remember { mutableStateOf("") }
    var stepConsumerCode by remember { mutableStateOf("") }
    var stepMouseButton by remember { mutableStateOf("1") }
    var stepText by remember { mutableStateOf("") }
    var stepDelay by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Macro Editor", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (steps.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2A2A3A)), contentAlignment = Alignment.Center) {
                    Text("No steps yet. Tap \"Add Step\" below.", color = Color(0xFF555555), fontSize = 12.sp)
                }
            } else {
                steps.forEachIndexed { si, step ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A3A))
                        .padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val (icon, summary) = when (step.type) {
                            "keystroke" -> Icons.Default.Keyboard to "Key: 0x${step.keyCode.toUByte().toString(16).padStart(2, '0').uppercase()}"
                            "consumer" -> Icons.Default.Audiotrack to "Consumer: 0x${step.consumerCode.toString(16).padStart(4, '0').uppercase()}"
                            "mouse" -> Icons.Default.TouchApp to "Mouse btn: ${step.mouseButton}"
                            "text" -> Icons.Default.TextSnippet to "Text: \"${step.text.take(20)}\""
                            "delay" -> Icons.Default.Schedule to "Delay: ${step.delayMs}ms"
                            else -> Icons.Default.Keyboard to step.type
                        }
                        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${si + 1}. $summary", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF3A1A1A))
                            .clickable { onStepsChange(steps.toMutableList().also { it.removeAt(si) }) },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A3A3A))
                .clickable {
                    stepType = "keystroke"; stepKeyCode = ""; stepConsumerCode = ""
                    stepMouseButton = "1"; stepText = ""; stepDelay = ""; showStepEditor = true
                }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Step", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onAdd(steps) }, modifier = Modifier.weight(1f), enabled = steps.isNotEmpty()) {
                    Text("Done")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showStepEditor) {
        StepEditorDialog(
            type = stepType, onTypeChange = { stepType = it },
            keyCode = stepKeyCode, onKeyCodeChange = { stepKeyCode = it },
            consumerCode = stepConsumerCode, onConsumerCodeChange = { stepConsumerCode = it },
            mouseButton = stepMouseButton, onMouseButtonChange = { stepMouseButton = it },
            text = stepText, onTextChange = { stepText = it },
            delay = stepDelay, onDelayChange = { stepDelay = it },
            onAdd = {
                val step = when (stepType) {
                    "keystroke" -> MacroStep(type = "keystroke", keyCode = (stepKeyCode.toIntOrNull() ?: 0).toByte())
                    "consumer" -> MacroStep(type = "consumer", consumerCode = stepConsumerCode.toIntOrNull(16) ?: 0)
                    "mouse" -> MacroStep(type = "mouse", mouseButton = stepMouseButton.toIntOrNull() ?: 1)
                    "text" -> MacroStep(type = "text", text = stepText)
                    "delay" -> MacroStep(type = "delay", delayMs = stepDelay.toIntOrNull() ?: 0)
                    else -> return@StepEditorDialog
                }
                onStepsChange(steps + step)
                showStepEditor = false
            },
            onDismiss = { showStepEditor = false }
        )
    }
}

@Composable
private fun StepEditorDialog(
    type: String, onTypeChange: (String) -> Unit,
    keyCode: String, onKeyCodeChange: (String) -> Unit,
    consumerCode: String, onConsumerCodeChange: (String) -> Unit,
    mouseButton: String, onMouseButtonChange: (String) -> Unit,
    text: String, onTextChange: (String) -> Unit,
    delay: String, onDelayChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { focusManager.clearFocus(); onAdd() }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add Step", color = TextPrimary) },
        containerColor = CardSurface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("keystroke", "consumer", "mouse", "text", "delay").forEach { t ->
                        Box(Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (type == t) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                            .clickable { onTypeChange(t) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(t, color = if (type == t) Color.White else TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                when (type) {
                    "keystroke" -> {
                        OutlinedTextField(value = keyCode, onValueChange = onKeyCodeChange,
                            label = { Text("HID Key Code (0-255)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
                        Text("Tip: 0x2C = Space, 0x28 = Enter", color = TextSecondary, fontSize = 9.sp)
                    }
                    "consumer" -> {
                        OutlinedTextField(value = consumerCode, onValueChange = onConsumerCodeChange,
                            label = { Text("Consumer Code (hex)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
                        Text("Tip: 0xCD = PlayPause, 0xE9 = Vol+", color = TextSecondary, fontSize = 9.sp)
                    }
                    "mouse" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1" to "Left", "2" to "Right", "4" to "Middle").forEach { (v, l) ->
                                val bg = if (mouseButton == v) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
                                    .clickable { onMouseButtonChange(v) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(l, color = if (mouseButton == v) Color.White else TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1" to "Scroll Up", "-1" to "Scroll Down").forEach { (v, l) ->
                                val bg = if (mouseButton == v) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(bg)
                                    .clickable { onMouseButtonChange(v) }.padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center) {
                                    Text(l, color = if (mouseButton == v) Color.White else TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    "text" -> {
                        OutlinedTextField(value = text, onValueChange = onTextChange,
                            label = { Text("Text to send") }, singleLine = false, modifier = Modifier.fillMaxWidth().height(80.dp),
                            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
                    }
                    "delay" -> {
                        OutlinedTextField(value = delay, onValueChange = onDelayChange,
                            label = { Text("Delay in milliseconds") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
                    }
                }
            }
        })
}

@Composable
private fun TextBlockDialog(
    text: String, onTextChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { focusManager.clearFocus(); onAdd(text) }, enabled = text.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Text Block", color = TextPrimary) },
        containerColor = CardSurface,
        text = {
            Column {
                Text("Enter text to send when tapped:", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = text, onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    textStyle = TextStyle(color = Color(0xFFEEEEEE), fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF666666), cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = TextSecondary))
            }
        })
}

/**
 * Free-form key creator: label + USB HID usage ID + which HID report it rides on.
 * This is the escape hatch for keys the built-in palettes don't ship.
 */
@Composable
fun CustomKeyDialog(
    onAdd: (label: String, keyCode: Byte, consumerCode: Int, mouseButton: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var codeText by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("keyboard") }
    val focusManager = LocalFocusManager.current

    val types = listOf("keyboard" to "Keyboard", "consumer" to "Consumer", "mouse" to "Mouse")
    val parsedCode = codeText.toIntOrNull(16)
    val codeValid = parsedCode != null && parsedCode in 0..255
    val valid = label.isNotBlank() && codeValid

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = { Text("Custom Key", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create a key the palettes don't include.", color = TextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(12) },
                    label = { Text("Label") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF666666),
                        cursorColor = MaterialTheme.colorScheme.primary)
                )
                OutlinedTextField(
                    value = codeText, onValueChange = { codeText = it.take(2) },
                    label = { Text("HID code (hex, e.g. 4C)") }, singleLine = true,
                    isError = codeText.isNotEmpty() && !codeValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF666666),
                        cursorColor = MaterialTheme.colorScheme.primary)
                )
                if (codeText.isNotEmpty() && !codeValid) {
                    Text("Enter one byte in hex: 00 - FF", color = Color(0xFFEF5350), fontSize = 10.sp)
                }
                Text("Report type", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { (id, title) ->
                        val sel = reportType == id
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                            .clickable { reportType = id }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center) {
                            Text(title, color = if (sel) Color.White else TextSecondary,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = {
                focusManager.clearFocus()
                val c = parsedCode!!.toInt() and 0xFF
                // Route the byte to the field that matches the chosen report type.
                when (reportType) {
                    "consumer" -> onAdd(label.trim(), 0, c, 0)
                    "mouse"    -> onAdd(label.trim(), 0, 0, c)
                    else       -> onAdd(label.trim(), c.toByte(), 0, 0)
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PositionPickerDialog(
    rows: List<MutableList<LayoutKey>>,
    onConfirm: (rowIdx: Int, pos: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRow by remember { mutableIntStateOf((rows.size - 1).coerceAtLeast(0)) }
    var selectedPos by remember { mutableIntStateOf(rows.lastOrNull()?.size ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = { Text("Place Key", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose where to insert the key:", color = TextSecondary, fontSize = 12.sp)

                // Row selector
                Column {
                    Text("Row", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rows.forEachIndexed { ri, _ ->
                            val bg = if (selectedRow == ri) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
                                .clickable { selectedRow = ri; selectedPos = selectedPos.coerceAtMost(rows[ri].size) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("Row ${ri + 1}", color = if (selectedRow == ri) Color.White else TextSecondary, fontSize = 11.sp)
                            }
                        }
                        // New row option
                        val newBg = if (selectedRow >= rows.size) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(newBg)
                            .clickable { selectedRow = rows.size; selectedPos = 0 }
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("+ New", color = if (selectedRow >= rows.size) Color.White else Color(0xFF4CAF50), fontSize = 11.sp)
                        }
                    }
                }

                // Position selector (within the selected row)
                if (selectedRow < rows.size) {
                    Column {
                        Text("Position", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Beginning
                            val bBg = if (selectedPos == 0) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(bBg)
                                .clickable { selectedPos = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Start", color = if (selectedPos == 0) Color.White else TextSecondary, fontSize = 10.sp)
                            }
                            // After each key
                            rows[selectedRow].forEachIndexed { ki, k ->
                                val kBg = if (selectedPos == ki + 1) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(kBg)
                                    .clickable { selectedPos = ki + 1 }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text("After \"${k.label.take(3)}\"", color = if (selectedPos == ki + 1) Color.White else TextSecondary, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                            // End
                            val eBg = if (selectedPos >= rows[selectedRow].size) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A)
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(eBg)
                                .clickable { selectedPos = rows[selectedRow].size }
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("End", color = if (selectedPos >= rows[selectedRow].size) Color.White else TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedRow, selectedPos) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Place Here", color = Color.White, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontSize = 12.sp) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerDialog(
    mode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = com.example.bluemice.model.PredefinedLayouts.all.filter { it.mode == mode }
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Create Layout", color = TextPrimary) },
        containerColor = CardSurface,
        text = {
            Column {
                Text("Choose a template to start with:", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                templates.forEach { t ->
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A3A))
                        .clickable { onSelect(t.name) }.padding(10.dp)) {
                        Text(t.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A1A2A))
                    .clickable { onSelect("") }.padding(10.dp)) {
                    Text("Start Blank", color = TextSecondary, fontSize = 13.sp)
                }
            }
        })
}
