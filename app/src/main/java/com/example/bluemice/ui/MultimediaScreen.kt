package com.example.bluemice.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.LayoutManager
import com.example.bluemice.SettingsManager
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.model.LayoutRow
import com.example.bluemice.ui.theme.*

private val KC = HidReportBuilder.KeyCodes

object ConsumerCodes {
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
    const val MUTE = 0x00E2
    const val BRIGHTNESS_UP = 0x006F
    const val BRIGHTNESS_DOWN = 0x0070
    const val PLAY = 0x00B0
    const val PAUSE = 0x00B1
    const val REWIND = 0x00B4
    const val FAST_FORWARD = 0x00B3
    const val NEXT_TRACK = 0x00B5
    const val PREV_TRACK = 0x00B6
    const val STOP = 0x00B7
    const val EJECT = 0x00B8
    const val PLAY_PAUSE = 0x00CD
}

data class Mk(val lbl: String, val code: Byte, val bg: Color = Color(0xFF2D2D2D), val col: Color = Color(0xFFEEEEEE), val consumerCode: Int = 0)

@Composable
fun MultimediaScreen(bleManager: BleHidManager, onEdit: () -> Unit = {}, layoutManager: LayoutManager? = null, settingsManager: SettingsManager? = null) {
    // LayoutManager is backed by SharedPreferences, which Compose cannot observe,
    // so switching or saving a layout only shows up once this counter changes.
    var layoutVersion by remember { mutableIntStateOf(0) }
    var currentLayoutName by remember {
        mutableStateOf(layoutManager?.getCurrentLayoutName("media") ?: "")
    }
    val layoutNames = remember(layoutVersion) {
        layoutManager?.getAllLayouts("media")?.map { it.name } ?: emptyList()
    }
    val currentLayout = remember(layoutVersion, layoutManager) {
        layoutManager?.getCurrentLayout("media")
    }
    val hasCustomKeys = currentLayout != null && currentLayout.rows.any { it.keys.isNotEmpty() }

    var isEditing by remember { mutableStateOf(false) }
    var editRows: List<MutableList<LayoutKey>> by remember { mutableStateOf(currentLayout?.rows?.map { it.keys.toMutableList() } ?: listOf(mutableListOf())) }
    var showAddKeySheet by remember { mutableStateOf(false) }
    var showEditKeySheet by remember { mutableStateOf(false) }
    var editRowIdx by remember { mutableIntStateOf(-1) }
    var editKeyIdx by remember { mutableIntStateOf(-1) }
    var editingKey by remember { mutableStateOf(LayoutKey("")) }

    var pendingKey by remember { mutableStateOf<LayoutKey?>(null) }
    var showPositionPicker by remember { mutableStateOf(false) }
    // Set when the user tapped a row's + slot - the key then lands at that row's end.
    var addTargetRow by remember { mutableIntStateOf(-1) }
    // Batch-edit mode: taps gather keys instead of opening the per-key sheet.
    var selectMode by remember { mutableStateOf(false) }
    // Test mode: render the live pad from the working rows instead of the canvas.
    var testMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val layoutHistory = remember { LayoutHistory() }
    fun pushUndoSnapshot() { layoutHistory.push(editRows.map { it.toList() }) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) layoutManager?.let { lm -> runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(lm.exportLayout("media", lm.getCurrentLayoutName("media"))?.toByteArray()) }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@runCatching
            if (layoutManager?.importLayout(json) == true) {
                val name = try { org.json.JSONObject(json).optString("name") } catch (_: Exception) { "" }
                layoutManager.getAllLayouts("media").find { l -> l.name == name }?.let { imported ->
                    pushUndoSnapshot()
                    editRows = imported.rows.map { it.keys.toMutableList() }
                    layoutManager.setCurrentLayoutName("media", name)
                }
            }
        }
    }

    fun insertKeyAt(key: LayoutKey, rowIdx: Int, pos: Int) {
        pushUndoSnapshot()
        val newRows = editRows.toMutableList()
        if (rowIdx >= newRows.size) {
            newRows.add(mutableListOf(key))
        } else {
            val nr = newRows[rowIdx].toMutableList()
            val insertPos = pos.coerceIn(0, nr.size)
            nr.add(insertPos, key)
            newRows[rowIdx] = nr
        }
        editRows = newRows
    }

    fun saveLayout(rows: List<MutableList<LayoutKey>>) {
        layoutManager?.let { lm ->
            val name = lm.getCurrentLayoutName("media")
            // Always keep the current name. Renaming an empty layout to a fixed
            // "Custom X" would silently overwrite any existing layout with that name.
            val layoutName = name
            lm.saveCustomLayout(LayoutDefinition(layoutName, rows.map { LayoutRow(it.toList()) }, "media"))
            lm.setCurrentLayoutName("media", layoutName)
        }
        // Repaint the live deck from what was just written.
        currentLayoutName = layoutManager?.getCurrentLayoutName("media") ?: currentLayoutName
        layoutVersion++
    }

    Box(Modifier.fillMaxSize().screenBackground()) {
        Column(Modifier.fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp,
                // Clear the floating EditModeBar so the last row stays tappable.
                bottom = if (isEditing) 64.dp else 12.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

            ScreenHeader(title = "Media", subtitle = "Playback · volume · navigation") {
                IconButton(onClick = { isEditing = !isEditing; if (!isEditing) saveLayout(editRows) }) {
                    Icon(Icons.Default.Edit, null, tint = if (isEditing) Color(0xFF4CAF50) else TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            // Which layout is live, and the one-tap way to make another.
            LayoutBar(
                mode = "media",
                currentName = currentLayoutName,
                names = layoutNames,
                onSelect = { name ->
                    layoutManager?.setCurrentLayoutName("media", name)
                    currentLayoutName = name
                    layoutManager?.getCurrentLayout("media")?.rows?.let { rows ->
                        editRows = rows.map { it.keys.toMutableList() }
                    }
                    layoutVersion++
                },
                onCreate = { template ->
                    layoutManager?.createLayout("media", template)?.let { created ->
                        currentLayoutName = created.name
                        editRows = created.rows.map { it.keys.toMutableList() }
                        layoutVersion++
                        isEditing = true
                    }
                }
            )

            // Framed surface - the same one the gamepad screen uses.
            ScreenPanel(Modifier.padding(bottom = 8.dp)) {
                if (isEditing && testMode) {
                    TestModePreview("media",
                        layoutManager?.getCurrentLayoutName("media") ?: "Custom Media",
                        editRows, bleManager)
                } else if (isEditing) {
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
                    onQuickColor = { ri, ki, c ->
                        val newRows = editRows.toMutableList()
                        val nr = newRows[ri].toMutableList()
                        nr[ki] = nr[ki].copy(backgroundColor = c)
                        newRows[ri] = nr
                        editRows = newRows
                    },
                    onRequestFullPicker = { ri, ki ->
                        editRowIdx = ri; editKeyIdx = ki
                        editingKey = editRows[ri][ki]
                        showEditKeySheet = true
                    },
                    selectMode = selectMode,
                    onBatchTransform = { coords, fn ->
                        pushUndoSnapshot()
                        val newRows = editRows.map { it.toMutableList() }
                        coords.forEach { (r, k) ->
                            if (r in newRows.indices && k in newRows[r].indices) {
                                newRows[r][k] = fn(newRows[r][k])
                            }
                        }
                        editRows = newRows
                    },
                    onBatchDelete = { coords ->
                        pushUndoSnapshot()
                        val newRows = editRows.map { it.toMutableList() }
                        coords.sortedByDescending { it.first }.groupBy { it.first }
                            .forEach { (r, keys) ->
                                if (r in newRows.indices) {
                                    keys.map { it.second }.sortedDescending()
                                        .forEach { k -> if (k in newRows[r].indices) newRows[r].removeAt(k) }
                                }
                            }
                        editRows = newRows
                    },
                    history = layoutHistory,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (!isEditing) {
                val layoutEmpty = currentLayout == null || currentLayout.rows.none { it.keys.isNotEmpty() }
                if (layoutEmpty) {
                    EmptyLayoutState(
                        message = "Your media layout has no keys",
                        onRestoreDefault = { layoutManager?.resetToDefault("media") },
                        onAddKeys = { isEditing = true }
                    )
                } else {
                    CustomLayoutView(currentLayout!!, bleManager)
                }
            }
            }
        }

        if (isEditing) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                EditModeBar(
                    layoutName = layoutManager?.getCurrentLayoutName("media") ?: "Custom Media",
                    layoutNames = layoutManager?.let { lm -> lm.getAllLayouts("media").map { it.name } } ?: emptyList(),
                    hasKeys = editRows.any { it.isNotEmpty() },
                    onLoadLayout = { name ->
                        val layout = layoutManager?.let { lm -> lm.getAllLayouts("media").find { l -> l.name == name } }
                        if (layout != null) {
                            pushUndoSnapshot()
                            editRows = layout.rows.map { it.keys.toMutableList() }
                            layoutManager?.setCurrentLayoutName("media", name)
                        }
                    },
                    onSave = { saveLayout(editRows) },
                    onAddKey = { showAddKeySheet = true },
                    onAddRow = { pushUndoSnapshot(); editRows = editRows + listOf(mutableListOf<LayoutKey>()) },
                                        onNewBlank = { pushUndoSnapshot(); editRows = listOf(mutableListOf<LayoutKey>(), mutableListOf<LayoutKey>()) },
                selectMode = selectMode,
                onToggleSelectMode = { selectMode = !selectMode },
                testMode = testMode,
                onToggleTestMode = { testMode = !testMode; if (testMode) selectMode = false },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onExport = { exportLauncher.launch("bluemice-media-layout.json") },
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
            onAddPaletteKey = { pk ->
                pendingKey = palKeyToLayoutKey(pk)
                showAddKeySheet = false
                showPositionPicker = true
            },
            onAddFnKey = { fk ->
                pendingKey = LayoutKey(label = fk.label, keyCode = fk.code, consumerCode = fk.consumerCode,
                    width = 1.2f, backgroundColor = when { fk.label.startsWith("F") -> 0xFF4A3A3A; fk.consumerCode in 0x00B0..0x00CD -> 0xFF3A4A3A; fk.consumerCode in 0x0060..0x00EA -> 0xFF3A3A4A; else -> 0xFF3A3A3A })
                showAddKeySheet = false
                showPositionPicker = true
            },
            onAddTextBlock = { text ->
                pendingKey = LayoutKey("Text", keyType = "textBlock", textBlock = text)
                showAddKeySheet = false
                showPositionPicker = true
            },
            onAddSpecial = { type, label, bg ->
                pendingKey = LayoutKey(label, keyType = type, width = if (type == "touchpad") 2f else 1f, height = if (type == "touchpad") 2f else 1f, backgroundColor = bg)
                showAddKeySheet = false
                showPositionPicker = true
            },
            onAddMacro = { steps ->
                pendingKey = LayoutKey("Macro (${steps.size} steps)", keyType = "keyboard", width = 1.5f, backgroundColor = 0xFF4A4A6F, macroSteps = steps)
                showAddKeySheet = false
                showPositionPicker = true
            },
            onAddCustomKey = { label, keyCode, consumerCode, mouseButton ->
                pendingKey = LayoutKey(label, keyCode = keyCode, consumerCode = consumerCode, mouseButton = mouseButton)
                showAddKeySheet = false
                showPositionPicker = true
            },
                        onAddTemplate = { tpl ->
                val lm = layoutManager ?: return@AddKeySheet
                val src = if (tpl.isEmpty()) null else lm.getAllLayouts("media").find { it.name == tpl }
                if (src != null) {
                    pushUndoSnapshot()
                    editRows = src.rows.map { r -> r.keys.toMutableList() }
                    lm.setCurrentLayoutName("media", src.name)
                } else {
                    pushUndoSnapshot()
                    editRows = listOf(mutableListOf())
                }
                showAddKeySheet = false
            },
            onDismiss = { showAddKeySheet = false }
        )
    }

    // A row's + slot appends straight to that row; otherwise ask where the key goes.
    LaunchedEffect(pendingKey, addTargetRow) {
        if (pendingKey != null && addTargetRow >= 0) {
            val target = addTargetRow
            val key = pendingKey!!
            addTargetRow = -1
            pendingKey = null
            showPositionPicker = false
            insertKeyAt(key, target, editRows.getOrNull(target)?.size ?: 0)
        }
    }

    if (showPositionPicker && pendingKey != null && addTargetRow < 0) {
        PositionPickerDialog(
            rows = editRows,
            onConfirm = { rowIdx, pos ->
                insertKeyAt(pendingKey!!, rowIdx, pos)
                pendingKey = null
                showPositionPicker = false
            },
            onDismiss = {
                pendingKey = null
                showPositionPicker = false
            }
        )
    }

    if (showEditKeySheet && editRowIdx >= 0 && editKeyIdx >= 0) {
        EditKeySheet(
            key = editingKey,
            onApply = { newKey, scope -> pushUndoSnapshot(); editRows = applyKeyEdit(editRows, editRowIdx, editKeyIdx, editingKey, newKey, scope); showEditKeySheet = false },
            onDelete = { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr.removeAt(editKeyIdx); er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onMoveLeft = { if (editKeyIdx > 0) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx-1]; nr[editKeyIdx-1] = t; er[editRowIdx] = nr; editRows = er } },
            onMoveRight = { if (editKeyIdx < editRows[editRowIdx].size - 1) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx+1]; nr[editKeyIdx+1] = t; er[editRowIdx] = nr; editRows = er } },
            onEditMacro = {},
            onDismiss = { showEditKeySheet = false }
        )
    }
}

@Composable
private fun MediaKey(k: Mk, mod: Modifier, bm: BleHidManager, onCapture: ((LayoutKey) -> Unit)? = null) {
    val capturedKey = remember(k.lbl, k.code, k.consumerCode) {
        LayoutKey(label = k.lbl, keyCode = k.code, consumerCode = k.consumerCode)
    }
    Box(mod.height(56.dp).clip(RoundedCornerShape(10.dp)).background(k.bg).then(
        if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
        else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            if (k.consumerCode != 0) bm.sendConsumerPress(k.consumerCode)
            else bm.sendKeyPress(0, k.code)
        }
    ), contentAlignment = Alignment.Center) {
        Text(k.lbl, color = k.col, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

@Composable
private fun ConsumerKey(k: Mk, mod: Modifier, bm: BleHidManager, onCapture: ((LayoutKey) -> Unit)? = null) {
    val capturedKey = remember(k.lbl, k.consumerCode) {
        LayoutKey(label = k.lbl, consumerCode = k.consumerCode)
    }
    Box(mod.height(56.dp).clip(RoundedCornerShape(10.dp)).background(k.bg).then(
        if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
        else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { bm.sendConsumerPress(k.consumerCode) }
    ), contentAlignment = Alignment.Center) {
        Text(k.lbl, color = k.col, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

@Composable
private fun ShortcutKey(lbl: String, mods: Int, key: Byte, mod: Modifier, bm: BleHidManager, onCapture: ((LayoutKey) -> Unit)? = null) {
    val capturedKey = remember(lbl, mods, key) {
        LayoutKey(label = lbl, comboMods = mods, comboKey = key)
    }
    Box(mod.clip(RoundedCornerShape(5.dp)).background(Color(0xFF2A2A4A)).then(
        if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
        else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { bm.sendKeyPress(mods, key) }
    ).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(lbl, color = Color(0xFFCCCCCC), fontSize = 9.sp, textAlign = TextAlign.Center)
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(5.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp).padding(1.dp))
            }
        }
    }
}

@Composable
private fun MediaDpad(bm: BleHidManager, onCaptureKey: ((LayoutKey) -> Unit)? = null) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DpadBtn("\u25B2", KC.KEY_ARROW_UP, Color(0xFF3A3A3A), bm, onCaptureKey)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                DpadBtn("\u25C4", KC.KEY_ARROW_LEFT, Color(0xFF3A3A3A), bm, onCaptureKey)
                DpadBtn("OK", KC.KEY_ENTER, Color(0xFF2A4A2A), bm, onCaptureKey)
                DpadBtn("\u25BA", KC.KEY_ARROW_RIGHT, Color(0xFF3A3A3A), bm, onCaptureKey)
            }
            DpadBtn("\u25BC", KC.KEY_ARROW_DOWN, Color(0xFF3A3A3A), bm, onCaptureKey)
        }
    }
}

@Composable
private fun DpadBtn(lbl: String, code: Byte, bg: Color, bm: BleHidManager, onCaptureKey: ((LayoutKey) -> Unit)? = null) {
    val capturedKey = remember(lbl, code) { LayoutKey(label = lbl, keyCode = code, backgroundColor = 0xFF3A3A3A) }
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(bg).then(
        if (onCaptureKey != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCaptureKey(capturedKey) }
        else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { bm.sendKeyPress(0, code) }
    ), contentAlignment = Alignment.Center) {
        Text(lbl, color = if (lbl == "OK") Color(0xFF4CAF50) else Color(0xFFDDDDDD), fontSize = if (lbl == "OK") 12.sp else 18.sp, fontWeight = if (lbl == "OK") FontWeight.Bold else FontWeight.Normal)
        if (onCaptureKey != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}
