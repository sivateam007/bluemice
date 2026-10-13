package com.example.bluemice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.LayoutManager
import com.example.bluemice.SettingsManager
import com.example.bluemice.model.KeyboardLanguageCatalog
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.model.LayoutRow
import com.example.bluemice.ui.theme.*

data class KDef(val label: String, val onClick: () -> Unit, val w: Float = 1f, val bg: Color = Color(0xFF2D2D2D), val sz: Float = 12f)

@Composable
fun KRow(keys: List<KDef>, h: Dp = 44.dp) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        keys.forEach { k ->
            Box(
                modifier = Modifier.weight(k.w.coerceAtLeast(0.5f)).height(h)
                    .clip(RoundedCornerShape(6.dp)).background(k.bg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = k.onClick),
                contentAlignment = Alignment.Center
            ) { Text(k.label, fontSize = k.sz.sp, fontWeight = FontWeight.Medium, color = TextPrimary, textAlign = TextAlign.Center, maxLines = 1) }
        }
    }
}

@Composable
fun KeyboardScreen(bleManager: BleHidManager, onEdit: () -> Unit = {}, layoutManager: LayoutManager? = null, settingsManager: SettingsManager? = null) {
    var textInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val KC = HidReportBuilder.KeyCodes

    var keyboardMode by remember { mutableStateOf(settingsManager?.keyboardMode ?: "layout") }
    var language by remember { mutableStateOf(KeyboardLanguageCatalog.byLocale(settingsManager?.selectedLanguage ?: "en")) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val currentLayout = layoutManager?.getCurrentLayout("keyboard")
    val hasCustomKeys = currentLayout != null && currentLayout.rows.any { it.keys.isNotEmpty() }

    // Inline editing state
    var isEditing by remember { mutableStateOf(false) }
    var editRows: List<MutableList<LayoutKey>> by remember { mutableStateOf(currentLayout?.rows?.map { it.keys.toMutableList() } ?: listOf(mutableListOf())) }
    var showAddKeySheet by remember { mutableStateOf(false) }
    var showEditKeySheet by remember { mutableStateOf(false) }
    var editRowIdx by remember { mutableIntStateOf(-1) }
    var editKeyIdx by remember { mutableIntStateOf(-1) }
    var editingKey by remember { mutableStateOf(LayoutKey("")) }

    fun saveLayout(rows: List<MutableList<LayoutKey>>) {
        layoutManager?.let { lm ->
            val name = lm.getCurrentLayoutName("keyboard")
            // Always keep the current name. Renaming an empty layout to a fixed
            // "Custom X" would silently overwrite any existing layout with that name.
            val layoutName = name
            lm.saveCustomLayout(LayoutDefinition(layoutName, rows.map { LayoutRow(it.toList()) }, "keyboard"))
            lm.setCurrentLayoutName("keyboard", layoutName)
        }
    }

    val context = LocalContext.current
    val layoutHistory = remember { LayoutHistory() }
    fun pushUndoSnapshot() { layoutHistory.push(editRows.map { it.toList() }) }

    // Set when the user tapped a row's + slot - the key then lands at that row's end.
    var addTargetRow by remember { mutableIntStateOf(-1) }

    /** Appends [key] to the tapped row, falling back to the last row. */
    fun appendKey(rowIdx: Int, key: LayoutKey) {
        addTargetRow = -1
        pushUndoSnapshot()
        if (editRows.isEmpty()) { editRows = listOf(mutableListOf(key)); return }
        val ri = if (rowIdx in 0 until editRows.size) rowIdx else editRows.size - 1
        val newRows = editRows.toMutableList()
        val nr = newRows[ri].toMutableList()
        nr.add(key)
        newRows[ri] = nr
        editRows = newRows
    }
    // Export/import through LayoutManager's canonical org.json schema, the same
    // path every other screen uses. The old local kotlinx.serialized pair wrote
    // a different field layout, so keyboard files didn't round-trip with the
    // other five screens.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) layoutManager?.let { lm -> runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(lm.exportLayout("keyboard", lm.getCurrentLayoutName("keyboard"))?.toByteArray())
            }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: return@runCatching
            if (layoutManager?.importLayout(json) == true) {
                val name = try { org.json.JSONObject(json).optString("name") } catch (_: Exception) { "" }
                layoutManager.getAllLayouts("keyboard").find { l -> l.name == name }?.let { imported ->
                    pushUndoSnapshot()
                    editRows = imported.rows.map { it.keys.toMutableList() }
                    layoutManager.setCurrentLayoutName("keyboard", name)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(Modifier.fillMaxSize().screenBackground()) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 6.dp, end = 6.dp, top = 6.dp,
                    // Clear the floating EditModeBar so the last row stays tappable.
                    bottom = if (isEditing) 64.dp else 6.dp
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Same header treatment as the other screens so the app reads as one product.
            ScreenHeader(
                title = "Keyboard",
                subtitle = if (keyboardMode == "language") "Language board" else "Layout keys"
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(CardSurface).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    var prevLen by remember { mutableStateOf(0) }
                    BasicTextField(
                        value = textInput,
                        onValueChange = { newText ->
                            if (newText.length < prevLen) {
                                val deleted = prevLen - newText.length
                                repeat(deleted) { bleManager.sendKeyPress(0, KC.KEY_BACKSPACE) }
                            } else {
                                newText.drop(prevLen).forEach { c ->
                                    sendChar(bleManager, c, if (keyboardMode == "language") language else null)
                                }
                            }
                            prevLen = newText.length
                            textInput = newText
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                        singleLine = true
                    )
                    if (textInput.isEmpty()) {
                        Text("Type with Gboard...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(CardSurface)
                    .clickable { textInput = ""; focusRequester.requestFocus() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)) { Text("Clear", color = Color(0xFF888888), fontSize = 12.sp) }
                IconButton(onClick = {
                    keyboardMode = if (keyboardMode == "layout") "language" else "layout"
                    settingsManager?.keyboardMode = keyboardMode
                }) {
                    Icon(Icons.Default.Language, null, tint = if (keyboardMode == "language") Color(0xFF4CAF50) else TextSecondary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { isEditing = !isEditing; if (!isEditing) saveLayout(editRows) }) {
                    Icon(Icons.Default.Edit, null, tint = if (isEditing) Color(0xFF4CAF50) else TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            if (isEditing) {
                EditableLayoutView(
                    rows = editRows,
                    onEditKey = { ri, ki -> editRowIdx = ri; editKeyIdx = ki; editingKey = editRows[ri][ki]; showEditKeySheet = true },
                    onMoveLeft = { ri, ki -> if (ki > 0) { val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); val t = nr[ki]; nr[ki] = nr[ki-1]; nr[ki-1] = t; newRows[ri] = nr; editRows = newRows } },
                    onMoveRight = { ri, ki -> if (ki < editRows[ri].size - 1) { val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); val t = nr[ki]; nr[ki] = nr[ki+1]; nr[ki+1] = t; newRows[ri] = nr; editRows = newRows } },
                    onMoveUp = { ri, ki -> if (ri > 0) { val newRows = editRows.toMutableList(); val prevRow = newRows[ri - 1].toMutableList(); val curRow = newRows[ri].toMutableList(); val key = curRow.removeAt(ki); prevRow.add(key); newRows[ri - 1] = prevRow; newRows[ri] = curRow; editRows = newRows } },
                    onMoveDown = { ri, ki -> if (ri < editRows.size - 1) { val newRows = editRows.toMutableList(); val nextRow = newRows[ri + 1].toMutableList(); val curRow = newRows[ri].toMutableList(); val key = curRow.removeAt(ki); nextRow.add(0, key); newRows[ri + 1] = nextRow; newRows[ri] = curRow; editRows = newRows } },
                    onDeleteKey = { ri, ki -> val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); nr.removeAt(ki); newRows[ri] = nr; editRows = newRows },
                    onMoveRowUp = { ri -> if (ri > 0) { val newRows = editRows.toMutableList(); val t = newRows[ri]; newRows[ri] = newRows[ri-1]; newRows[ri-1] = t; editRows = newRows } },
                    onMoveRowDown = { ri -> if (ri < editRows.size - 1) { val newRows = editRows.toMutableList(); val t = newRows[ri]; newRows[ri] = newRows[ri+1]; newRows[ri+1] = t; editRows = newRows } },
                    onDeleteRow = { ri -> val newRows = editRows.toMutableList(); newRows.removeAt(ri); editRows = newRows },
                    onSwapKeys = { ri, ki1, ki2 -> val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); val t = nr[ki1]; nr[ki1] = nr[ki2]; nr[ki2] = t; newRows[ri] = nr; editRows = newRows },
                    onResizeKey = { ri, ki, w -> val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); nr[ki] = nr[ki].copy(width = w); newRows[ri] = nr; editRows = newRows },
                    onResizeHeight = { ri, ki, h -> val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); nr[ki] = nr[ki].copy(height = h); newRows[ri] = nr; editRows = newRows },
                    onEqualizeRow = { ri -> val newRows = editRows.toMutableList(); newRows[ri] = editRows[ri].map { it.copy(width = 1f) }.toMutableList(); editRows = newRows },
                    onAlignRow = { ri -> val mx = editRows[ri].maxOf { it.width }; val newRows = editRows.toMutableList(); newRows[ri] = editRows[ri].map { it.copy(width = mx) }.toMutableList(); editRows = newRows },
                    onDuplicateKey = { ri, ki -> val newRows = editRows.toMutableList(); val nr = newRows[ri].toMutableList(); nr.add(ki + 1, nr[ki]); newRows[ri] = nr; editRows = newRows },
                    onAddToRow = { ri -> addTargetRow = ri; showAddKeySheet = true },
                    history = layoutHistory,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            } else if (!isEditing) {
                val layoutEmpty = currentLayout == null || currentLayout.rows.none { it.keys.isNotEmpty() }
                if (layoutEmpty) {
                    EmptyLayoutState(
                        message = "Your keyboard layout has no keys",
                        onRestoreDefault = { layoutManager?.resetToDefault("keyboard") },
                        onAddKeys = { isEditing = true }
                    )
                } else {
                    CustomLayoutView(currentLayout!!, bleManager, modifier = Modifier.padding(bottom = 6.dp),
                        hapticsEnabled = settingsManager?.hapticsEnabled ?: false)
                }
            }

            if (!isEditing) {
                if (keyboardMode == "language") {
                    Spacer(Modifier.height(2.dp))
                    LanguageKeyboard(
                        bleManager = bleManager,
                        language = language,
                        onPickLanguage = { showLanguagePicker = true }
                    )
            }
            }
        }

        if (isEditing) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                EditModeBar(
                    layoutName = layoutManager?.getCurrentLayoutName("keyboard") ?: "Custom Keyboard",
                    layoutNames = layoutManager?.let { lm -> lm.getAllLayouts("keyboard").map { it.name } } ?: emptyList(),
                    hasKeys = editRows.any { it.isNotEmpty() },
                    onLoadLayout = { name ->
                        val layout = layoutManager?.let { lm -> lm.getAllLayouts("keyboard").find { l -> l.name == name } }
                        if (layout != null) {
                            pushUndoSnapshot()
                            editRows = layout.rows.map { it.keys.toMutableList() }
                            layoutManager?.setCurrentLayoutName("keyboard", name)
                        }
                    },
                    onSave = { saveLayout(editRows) },
                    onAddKey = { showAddKeySheet = true },
                    onAddRow = { pushUndoSnapshot(); editRows = editRows + listOf(mutableListOf<LayoutKey>()) },
                                        onNewBlank = { pushUndoSnapshot(); editRows = listOf(mutableListOf<LayoutKey>()) },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onExport = { exportLauncher.launch("layout.json") },
                    canUndo = layoutHistory.undoStack.isNotEmpty(),
                    canRedo = layoutHistory.redoStack.isNotEmpty(),
                    onUndo = { layoutHistory.undo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onRedo = { layoutHistory.redo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onExit = { isEditing = false; saveLayout(editRows) }
                )
            }
        }
    }

    if (showAddKeySheet) {
        AddKeySheet(
            onAddPaletteKey = { pk -> appendKey(addTargetRow, palKeyToLayoutKey(pk)); showAddKeySheet = false },
            onAddFnKey = { fk -> appendKey(addTargetRow, LayoutKey(label = fk.label, keyCode = fk.code, consumerCode = fk.consumerCode, width = 1.2f, backgroundColor = when { fk.label.startsWith("F") -> 0xFF4A3A3A; fk.consumerCode in 0x00B0..0x00CD -> 0xFF3A4A3A; fk.consumerCode in 0x0060..0x00EA -> 0xFF3A3A4A; else -> 0xFF3A3A3A })); showAddKeySheet = false },
            onAddTextBlock = { text -> appendKey(addTargetRow, LayoutKey("Text", keyType = "textBlock", textBlock = text)); showAddKeySheet = false },
            onAddSpecial = { type, label, bg -> appendKey(addTargetRow, LayoutKey(label, keyType = type, width = if (type == "touchpad") 2f else 1f, height = if (type == "touchpad") 2f else 1f, backgroundColor = bg)); showAddKeySheet = false },
            onAddMacro = { steps -> appendKey(addTargetRow, LayoutKey("Macro (${steps.size} steps)", keyType = "keyboard", width = 1.5f, backgroundColor = 0xFF4A4A6F, macroSteps = steps)); showAddKeySheet = false },
            onAddCustomKey = { label, keyCode, consumerCode, mouseButton ->
                appendKey(addTargetRow, LayoutKey(label, keyCode = keyCode, consumerCode = consumerCode, mouseButton = mouseButton))
                showAddKeySheet = false
            },
                        onAddTemplate = { tpl ->
                val lm = layoutManager ?: return@AddKeySheet
                val src = if (tpl.isEmpty()) null else lm.getAllLayouts("keyboard").find { it.name == tpl }
                if (src != null) {
                    pushUndoSnapshot()
                    editRows = src.rows.map { r -> r.keys.toMutableList() }
                    lm.setCurrentLayoutName("keyboard", src.name)
                } else {
                    pushUndoSnapshot()
                    editRows = listOf(mutableListOf())
                }
                showAddKeySheet = false
            },
            onDismiss = { showAddKeySheet = false }
        )
    }

    if (showEditKeySheet && editRowIdx >= 0 && editKeyIdx >= 0) {
        EditKeySheet(
            key = editingKey,
            onApply = { newKey -> val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr[editKeyIdx] = newKey; er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onDelete = { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr.removeAt(editKeyIdx); er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onMoveLeft = { if (editKeyIdx > 0) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx-1]; nr[editKeyIdx-1] = t; er[editRowIdx] = nr; editRows = er } },
            onMoveRight = { if (editKeyIdx < editRows[editRowIdx].size - 1) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx+1]; nr[editKeyIdx+1] = t; er[editRowIdx] = nr; editRows = er } },
            onEditMacro = {},
            onDismiss = { showEditKeySheet = false }
        )
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            currentLocale = language.locale,
            onPick = { loc ->
                language = KeyboardLanguageCatalog.byLocale(loc)
                settingsManager?.selectedLanguage = loc
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }
}

@Composable
private fun ArrowCluster(onKey: (Byte) -> Unit) {
    val KC = HidReportBuilder.KeyCodes
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            KDef("Ins", { onKey(KC.KEY_INSERT) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=48.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
            KDef("Del", { onKey(KC.KEY_DELETE) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=48.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            KDef("Home", { onKey(KC.KEY_HOME) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=56.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
            KDef("PgUp", { onKey(KC.KEY_PAGE_UP) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=56.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            KDef("\u25B2", { onKey(KC.KEY_ARROW_UP) }, 1f, sz=12f).let { k -> Box(Modifier.size(width=48.dp, height=34.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                KDef("\u25C0", { onKey(KC.KEY_ARROW_LEFT) }, 1f, sz=12f).let { k -> Box(Modifier.size(width=48.dp, height=34.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
                KDef("\u25BC", { onKey(KC.KEY_ARROW_DOWN) }, 1f, sz=12f).let { k -> Box(Modifier.size(width=48.dp, height=34.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
                KDef("\u25B6", { onKey(KC.KEY_ARROW_RIGHT) }, 1f, sz=12f).let { k -> Box(Modifier.size(width=48.dp, height=34.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            KDef("End", { onKey(KC.KEY_END) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=56.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
            KDef("PgDn", { onKey(KC.KEY_PAGE_DOWN) }, 1f, Color(0xFF3A3A3A), 10f).let { k -> Box(Modifier.size(width=56.dp, height=36.dp).clip(RoundedCornerShape(6.dp)).background(k.bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null){k.onClick()}, contentAlignment=Alignment.Center){ Text(k.label, fontSize=k.sz.sp, color=TextPrimary, textAlign=TextAlign.Center) } }
        }
    }
}
