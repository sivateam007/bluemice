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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.LayoutManager
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.model.LayoutRow
import com.example.bluemice.ui.theme.*

private val KC = HidReportBuilder.KeyCodes
private val numBg = Color(0xFF2D2D2D)
private val opBg = Color(0xFF3A3A50)
private val actionBg = Color(0xFF4A2A2A)
private val enterBg = Color(0xFF2A4A2A)
private val clearBg = Color(0xFF6A1A1A)
private val fnBg = Color(0xFF3A3A2A)
private val bg = SurfaceDark

data class Nk(val lbl: String, val code: Byte, val bg: Color = numBg, val w: Float = 1f)

@Composable
fun NumpadScreen(bleManager: BleHidManager, onEdit: () -> Unit = {}, layoutManager: LayoutManager? = null) {
    var disp by remember { mutableStateOf("") }
    fun send(k: Byte) { bleManager.sendKeyPress(0, k) }

    // The layout keys send the HID reports; this mirrors them into the on-screen
    // readout so the calculator display keeps working now the grid is editable.
    val displayChars = setOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        ".", "(", ")", "%", "/", "\u00D7", "\u2212", "+", "00", "000")
    fun onNumpadKeySent(key: LayoutKey) {
        when {
            key.label == "C" || key.label == "CE" -> disp = ""
            key.label == "\u00B1" -> if (disp.startsWith("-")) disp = disp.drop(1) else disp = "-$disp"
            key.keyCode == KC.KEY_BACKSPACE -> if (disp.isNotEmpty()) disp = disp.dropLast(1)
            key.keyCode == KC.KEY_KP_ENTER -> disp += "\n="
            key.label in displayChars -> disp += key.label
        }
    }

    val currentLayout = layoutManager?.getCurrentLayout("numpad")
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

    val context = LocalContext.current
    val layoutHistory = remember { LayoutHistory() }
    fun pushUndoSnapshot() { layoutHistory.push(editRows.map { it.toList() }) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) layoutManager?.let { lm -> runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(lm.exportLayout("numpad", lm.getCurrentLayoutName("numpad"))?.toByteArray()) }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@runCatching
            if (layoutManager?.importLayout(json) == true) {
                val name = try { org.json.JSONObject(json).optString("name") } catch (_: Exception) { "" }
                layoutManager.getAllLayouts("numpad").find { l -> l.name == name }?.let { imported ->
                    pushUndoSnapshot()
                    editRows = imported.rows.map { it.keys.toMutableList() }
                    layoutManager.setCurrentLayoutName("numpad", name)
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
            val name = lm.getCurrentLayoutName("numpad")
            // Always keep the current name. Renaming an empty layout to a fixed
            // "Custom X" would silently overwrite any existing layout with that name.
            val layoutName = name
            lm.saveCustomLayout(LayoutDefinition(layoutName, rows.map { LayoutRow(it.toList()) }, "numpad"))
            lm.setCurrentLayoutName("numpad", layoutName)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(bg).padding(8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
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
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else if (!isEditing) {
                val layoutEmpty = currentLayout == null || currentLayout.rows.none { it.keys.isNotEmpty() }
                if (layoutEmpty) {
                    EmptyLayoutState(
                        message = "Your numpad layout has no keys",
                        onRestoreDefault = { layoutManager?.resetToDefault("numpad") },
                        onAddKeys = { isEditing = true }
                    )
                } else {
                    CustomLayoutView(
                        currentLayout!!, bleManager,
                        modifier = Modifier.padding(bottom = 4.dp),
                        onKeySent = { onNumpadKeySent(it) }
                    )
                }
            }

            // Display - mirrors whatever the layout keys just sent.
            Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0D0D0D)).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd) {
                Text(disp.ifEmpty { "0" }, color = Color(0xFF4CAF50), fontSize = if (disp.length > 12) 18.sp else 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        if (isEditing) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                EditModeBar(
                    layoutName = layoutManager?.getCurrentLayoutName("numpad") ?: "Custom Numpad",
                    layoutNames = layoutManager?.let { lm -> lm.getAllLayouts("numpad").map { it.name } } ?: emptyList(),
                    hasKeys = editRows.any { it.isNotEmpty() },
                    onLoadLayout = { name ->
                        val layout = layoutManager?.let { lm -> lm.getAllLayouts("numpad").find { l -> l.name == name } }
                        if (layout != null) {
                            pushUndoSnapshot()
                            editRows = layout.rows.map { it.keys.toMutableList() }
                            layoutManager?.setCurrentLayoutName("numpad", name)
                        }
                    },
                    onSave = { saveLayout(editRows) },
                    onAddKey = { showAddKeySheet = true },
                    onAddRow = { pushUndoSnapshot(); editRows = editRows + listOf(mutableListOf<LayoutKey>()) },
                                        onNewBlank = { pushUndoSnapshot(); editRows = listOf(mutableListOf<LayoutKey>()) },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onExport = { exportLauncher.launch("bluemice-numpad-layout.json") },
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
                val src = if (tpl.isEmpty()) null else lm.getAllLayouts("numpad").find { it.name == tpl }
                if (src != null) {
                    pushUndoSnapshot()
                    editRows = src.rows.map { r -> r.keys.toMutableList() }
                    lm.setCurrentLayoutName("numpad", src.name)
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
            onApply = { newKey -> val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr[editKeyIdx] = newKey; er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onDelete = { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr.removeAt(editKeyIdx); er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onMoveLeft = { if (editKeyIdx > 0) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx-1]; nr[editKeyIdx-1] = t; er[editRowIdx] = nr; editRows = er } },
            onMoveRight = { if (editKeyIdx < editRows[editRowIdx].size - 1) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx+1]; nr[editKeyIdx+1] = t; er[editRowIdx] = nr; editRows = er } },
            onEditMacro = {},
            onDismiss = { showEditKeySheet = false }
        )
    }
}
