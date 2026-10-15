package com.example.bluemice.ui

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.bluemice.ui.theme.CardSurface
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.BleHidManager
import com.example.bluemice.LayoutManager
import com.example.bluemice.SettingsManager
import com.example.bluemice.model.GamepadAction
import com.example.bluemice.model.GamepadConfig
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.model.LayoutRow
import com.example.bluemice.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

private val CyanNeon = Color(0xFF00E5FF)
private val CyanDim = Color(0xFF0088AA)

@Composable
fun GamepadScreen(
    bleManager: BleHidManager,
    onEdit: () -> Unit = {},
    layoutManager: LayoutManager? = null,
    settingsManager: SettingsManager? = null,
    /**
     * The pad should own the window: landscape, Fullscreen on, this tab open.
     * Header and layout bar step aside (the activity's top bar does the same)
     * so the only thing on screen is the controller.
     *
     * It still yields while the edit canvas is open - the edit bar is chrome you
     * need in order to get back out.
     */
    playMode: Boolean = false,
    /**
     * Play mode is *available* but currently dismissed. The header then offers a
     * way back in, so entering and leaving play mode is symmetric.
     */
    playAvailable: Boolean = false,
    onTogglePlayMode: () -> Unit = {}
) {
    var leftX by remember { mutableStateOf(0f) }
    var leftY by remember { mutableStateOf(0f) }
    var rightX by remember { mutableStateOf(0f) }
    var rightY by remember { mutableStateOf(0f) }
    var isLeftTouched by remember { mutableStateOf(false) }
    var isRightTouched by remember { mutableStateOf(false) }

    var buttons by remember { mutableStateOf(0) }

    var padConfig by remember { mutableStateOf(settingsManager?.gamepadConfig ?: GamepadConfig()) }
    var showPadSettings by remember { mutableStateOf(false) }

    /**
     * Applies deadzone, sensitivity and axis inversion, then scales to the
     * -127..127 range the HID descriptor expects.
     */
    fun toAxis(v: Float, invert: Boolean): Int {
        val inverted = if (invert) -v else v
        // Deadzone rescaled so output starts at 0, not at the deadzone edge —
        // otherwise there's a jump the moment a stick leaves the centre.
        val dz = padConfig.stickDeadzone
        val shaped = if (abs(inverted) < dz) 0f
        else (abs(inverted) - dz) / (1f - dz) * kotlin.math.sign(inverted)
        return (shaped * padConfig.stickSensitivity * 127f).toInt().coerceIn(-127, 127)
    }

    LaunchedEffect(isLeftTouched, isRightTouched, leftX, leftY, rightX, rightY, buttons, padConfig) {
        while (isLeftTouched || isRightTouched || buttons != 0) {
            bleManager.sendGamepadReport(
                toAxis(leftX, padConfig.invertLeftX), toAxis(leftY, padConfig.invertLeftY),
                toAxis(rightX, padConfig.invertRightX), toAxis(rightY, padConfig.invertRightY),
                buttons
            )
            delay(16)
        }
        bleManager.sendEmptyGamepadReport()
    }

    /**
     * Remap-aware button press.
     *
     * Gamepad targets go through the binding table into the 16-bit mask. Any
     * button remapped to a keyboard/consumer/mouse target is diverted to that
     * report instead and leaves the mask entirely.
     *
     * Press and release resolve the same target, so a non-identity binding
     * can't leave a key stuck down.
     */
    fun setButton(bit: Int, pressed: Boolean) {
        val target = padConfig.overrides[bit]
        if (target != null && target.kind != GamepadAction.Kind.GAMEPAD) {
            when (target.kind) {
                GamepadAction.Kind.KEYBOARD -> {
                    // L/R shift and ctrl are e0/e1; a bare press needs the
                    // left-hand modifier so the PC sees a real modifier.
                    val mod = if (target.code == 0xE1 || target.code == 0xE0) 0xE0 else 0
                    bleManager.sendKeyPress(mod, target.code.toByte())
                }
                GamepadAction.Kind.CONSUMER -> bleManager.sendConsumerPress(target.code)
                GamepadAction.Kind.MOUSE -> bleManager.sendMouseClick(target.code)
                else -> {}
            }
            return
        }
        val logical = padConfig.bindings[bit] ?: bit
        if (logical !in 0..15) return
        val contribution = 1 shl logical
        buttons = if (pressed) buttons or contribution else buttons and contribution.inv()
    }

    // LayoutManager is backed by SharedPreferences, which Compose cannot observe,
    // so switching or saving a layout only shows up once this counter changes.
    var layoutVersion by remember { mutableIntStateOf(0) }
    var currentLayoutName by remember {
        mutableStateOf(layoutManager?.getCurrentLayoutName("gamepad") ?: "")
    }
    val layoutNames = remember(layoutVersion) {
        layoutManager?.getAllLayouts("gamepad")?.map { it.name } ?: emptyList()
    }
    val currentLayout = remember(layoutVersion, layoutManager) {
        layoutManager?.getCurrentLayout("gamepad")
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
            context.contentResolver.openOutputStream(uri)?.use { it.write(lm.exportLayout("gamepad", lm.getCurrentLayoutName("gamepad"))?.toByteArray()) }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@runCatching
            if (layoutManager?.importLayout(json) == true) {
                val name = try { org.json.JSONObject(json).optString("name") } catch (_: Exception) { "" }
                layoutManager.getAllLayouts("gamepad").find { l -> l.name == name }?.let { imported ->
                    pushUndoSnapshot()
                    editRows = imported.rows.map { it.keys.toMutableList() }
                    layoutManager.setCurrentLayoutName("gamepad", name)
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
            val name = lm.getCurrentLayoutName("gamepad")
            // Always keep the current name. Renaming an empty layout to a fixed
            // "Custom X" would silently overwrite any existing layout with that name.
            val layoutName = name
            lm.saveCustomLayout(LayoutDefinition(layoutName, rows.map { LayoutRow(it.toList()) }, "gamepad"))
            lm.setCurrentLayoutName("gamepad", layoutName)
        }
        // Repaint the live pad from what was just written.
        currentLayoutName = layoutManager?.getCurrentLayoutName("gamepad") ?: currentLayoutName
        layoutVersion++
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Play mode is the strict form of playMode: it steps aside the instant the
    // editor opens. Everything keyed on this - padding, scrolling, chrome, the
    // exit affordance - is derived from one place so the modes cannot drift.
    val playing = playMode && !isEditing
    // Pad opacity, honoured only while playing: a solid pad over a game is an
    // obstruction, and outside play mode there is nothing to see through it.
    // State rather than a plain read, so the slider in the settings sheet and
    // the pad itself move together.
    var padOpacity by remember { mutableStateOf(settingsManager?.gamepadPadOpacity ?: 1f) }

    // The exit affordance fades a few seconds after the last touch and returns
    // on the next one, so it never sits over a control mid-game. Resetting on a
    // *new* press rather than any move means dragging a stick keeps it hidden.
    var exitVisible by remember { mutableStateOf(true) }
    var exitTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(exitTick, playing) {
        if (playing) {
            delay(3000)
            exitVisible = false
        } else {
            exitVisible = true
        }
    }

    Box(
        Modifier.fillMaxSize().screenBackground()
            // Observed at the Initial pass and never consumed, so the keys
            // underneath still get every event - this only watches for a down.
            .then(
                if (playing) Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) exitTick++
                        }
                    }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(
                    // Play mode gives the pad every pixel - no gutters, because
                    // there is no chrome left for them to frame.
                    start = if (playing) 0.dp else 8.dp,
                    end = if (playing) 0.dp else 8.dp,
                    top = if (playing) 0.dp else if (isLandscape) 4.dp else 8.dp,
                    // Reserve the strip the floating EditModeBar occupies, so the
                    // last row (and its Save/Done controls) are never covered.
                    bottom = if (isEditing) 64.dp else if (playing) 0.dp
                    else if (isLandscape) 4.dp else 8.dp
                )
        ) {
            if (!playing) {
            ScreenHeader(
                title = "Gamepad",
                subtitle = if (padConfig.preset == GamepadConfig.PRESET_XBOX) "Xbox layout" else padConfig.preset,
                accent = CyanNeon,
                scrollActions = true
            ) {
                // Play mode is on but dismissed - give a way back without a
                // detour through the drawer.
                if (playAvailable && !playMode && !isEditing) {
                    IconButton(onClick = onTogglePlayMode) {
                        Icon(Icons.Default.Fullscreen, "Enter play mode",
                            tint = CyanNeon, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = { showPadSettings = true }) {
                    Icon(Icons.Default.Settings, "Gamepad settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                Text("BT", color = CyanDim, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                IconButton(onClick = {
                    isEditing = !isEditing
                    if (isEditing) {
                        if (editRows.all { it.isEmpty() }) {
                            val defaultLayout = layoutManager?.getCurrentLayout("gamepad")
                            if (defaultLayout != null && defaultLayout.rows.any { it.keys.isNotEmpty() }) {
                                editRows = defaultLayout.rows.map { it.keys.toMutableList() }
                            } else {
                                // Last-resort seed: keep it in step with the shipped
                                // default rather than an old hardcoded copy.
                                editRows = com.example.bluemice.model.PredefinedLayouts.GAMEPAD_DEFAULT
                                    .rows.map { it.keys.toMutableList() }.toMutableList()
                            }
                        }
                    } else {
                        saveLayout(editRows)
                    }
                }) {
                    Icon(Icons.Default.Edit, null, tint = if (isEditing) CyanNeon else TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            // Which layout is live, and the one-tap way to make another.
            LayoutBar(
                mode = "gamepad",
                currentName = currentLayoutName,
                names = layoutNames,
                onSelect = { name ->
                    layoutManager?.setCurrentLayoutName("gamepad", name)
                    currentLayoutName = name
                    layoutManager?.getCurrentLayout("gamepad")?.rows?.let { rows ->
                        editRows = rows.map { it.keys.toMutableList() }
                    }
                    layoutVersion++
                },
                onCreate = { template ->
                    layoutManager?.createLayout("gamepad", template)?.let { created ->
                        currentLayoutName = created.name
                        editRows = created.rows.map { it.keys.toMutableList() }
                        layoutVersion++
                        isEditing = true
                    }
                }
            )
            }

            // One framed panel holds the pad in both orientations - it keeps the grid
            // aligned and gives portrait and landscape the same surface to sit in.
            //
            // Scrolling is the fallback, never the plan: a controller you have to
            // scroll is unusable, so play mode turns scrolling off and scales the
            // pad to the viewport instead. The edit canvas keeps it, because an
            // editor is the one place scrolling is the right answer.
            ScreenPanel(
                Modifier.weight(1f)
                    .then(if (playing) Modifier.graphicsLayer { alpha = padOpacity } else Modifier),
                scroll = !playing,
                contentAlignment = if (isLandscape) Alignment.Center else Alignment.TopCenter
            ) {
                if (isEditing && testMode) {
                    TestModePreview("gamepad",
                        layoutManager?.getCurrentLayoutName("gamepad") ?: "Custom Gamepad",
                        editRows, bleManager,
                        onStickTouch = { id, x, y, t ->
                            if (id == "L") { leftX = x; leftY = y; isLeftTouched = t } else { rightX = x; rightY = y; isRightTouched = t }
                        })
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
                    onStickTouch = { id, x, y, t ->
                        if (id == "L") { leftX = x; leftY = y; isLeftTouched = t } else { rightX = x; rightY = y; isRightTouched = t }
                    },
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
                    modifier = Modifier.fillMaxWidth()
                )
                } else {
                val layoutEmpty = currentLayout == null || currentLayout.rows.none { it.keys.isNotEmpty() }
                if (layoutEmpty) {
                    EmptyLayoutState(
                        message = "Your gamepad layout has no keys",
                        onRestoreDefault = { layoutManager?.resetToDefault("gamepad") },
                        onAddKeys = { isEditing = true }
                    )
                } else if (playing) {
                    // Size the pad to the viewport rather than to a fixed dp table.
                    //
                    // Width is already fluid - every row splits itself by weight -
                    // so height is the only axis that can overflow, and it is the
                    // only one we scale. Solving `natural * scale == maxHeight` is
                    // exact rather than measured, so the pad lands on the first
                    // frame instead of drawing at one size and jumping to another.
                    // The clamp caps how far it may *grow* (a phone pad does not
                    // want to be 3x) and refuses to shrink past the point where the
                    // keys stop being tappable - that is the honest signal that the
                    // layout is simply too tall for landscape.
                    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val natural = padNaturalHeight(currentLayout!!)
                        val scale = if (natural > 0.dp)
                            (maxHeight / natural).coerceIn(0.55f, 1.6f) else 1f
                        CustomLayoutView(currentLayout!!, bleManager,
                            heightScale = scale,
                            onStickTouch = { id, x, y, t ->
                                if (id == "L") { leftX = x; leftY = y; isLeftTouched = t } else { rightX = x; rightY = y; isRightTouched = t }
                            })
                    }
                } else {
                    CustomLayoutView(currentLayout!!, bleManager,
                        onStickTouch = { id, x, y, t ->
                            if (id == "L") { leftX = x; leftY = y; isLeftTouched = t } else { rightX = x; rightY = y; isRightTouched = t }
                        })
                }
            }
            }
        }

        if (isEditing) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                EditModeBar(
                    layoutName = layoutManager?.getCurrentLayoutName("gamepad") ?: "Custom Gamepad",
                    layoutNames = layoutManager?.let { lm -> lm.getAllLayouts("gamepad").map { it.name } } ?: emptyList(),
                    hasKeys = editRows.any { it.isNotEmpty() },
                    onLoadLayout = { name ->
                        val layout = layoutManager?.let { lm -> lm.getAllLayouts("gamepad").find { l -> l.name == name } }
                        if (layout != null) {
                            pushUndoSnapshot()
                            editRows = layout.rows.map { it.keys.toMutableList() }
                            layoutManager?.setCurrentLayoutName("gamepad", name)
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
                    onExport = { exportLauncher.launch("bluemice-gamepad-layout.json") },
                    canUndo = layoutHistory.undoStack.isNotEmpty(),
                    canRedo = layoutHistory.redoStack.isNotEmpty(),
                    onUndo = { layoutHistory.undo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onRedo = { layoutHistory.redo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onExit = { isEditing = false; saveLayout(editRows) }
                )
            }
        }

        // Play mode's only control. It lands in the pad's centre column - the
        // aligned gap between the left and right clusters - which is dead space
        // by construction, so it never covers a key. Fades while you play.
        if (playing) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .graphicsLayer { alpha = if (exitVisible) 1f else 0f }
                    .clip(CircleShape)
                    .background(Color(0xE6101828))
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    // Disabled while invisible so an invisible target cannot
                    // swallow a press meant for the pad underneath.
                    .clickable(enabled = exitVisible) { onTogglePlayMode() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FullscreenExit, "Leave play mode",
                        tint = CyanNeon, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Exit", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showAddKeySheet) {
        AddKeySheet(
            mode = "gamepad",
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
                val src = if (tpl.isEmpty()) null else lm.getAllLayouts("gamepad").find { it.name == tpl }
                if (src != null) {
                    pushUndoSnapshot()
                    editRows = src.rows.map { r -> r.keys.toMutableList() }
                    lm.setCurrentLayoutName("gamepad", src.name)
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

    if (showPadSettings) {
        GamepadSettingsSheet(
            config = padConfig,
            onConfigChange = { next ->
                padConfig = next
                settingsManager?.gamepadConfig = next
            },
            padOpacity = padOpacity,
            onPadOpacityChange = { v ->
                padOpacity = v
                settingsManager?.gamepadPadOpacity = v
            },
            onDismiss = { showPadSettings = false }
        )
    }
}

@Composable
private fun ColumnScope.PortraitGamepad(
    leftX: Float, leftY: Float, rightX: Float, rightY: Float,
    isLeftTouched: Boolean, isRightTouched: Boolean,
    onLeftTouch: (Float, Float, Boolean) -> Unit,
    onRightTouch: (Float, Float, Boolean) -> Unit,
    setButton: (Int, Boolean) -> Unit,
    triggerThreshold: Float = 0f,
    onCapture: ((LayoutKey) -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().weight(0.38f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        NeonStick("L", onTouch = onLeftTouch, modifier = Modifier.size(110.dp))
        NeonStick("R", onTouch = onRightTouch, modifier = Modifier.size(110.dp))
    }

    Spacer(Modifier.height(4.dp))

    Box(Modifier.fillMaxWidth().weight(0.37f)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NeonDPad(onPress = setButton, onCapture = onCapture)
            NeonABXY(onPress = setButton, onCapture = onCapture)
        }
    }

    Spacer(Modifier.height(4.dp))

    Box(Modifier.fillMaxWidth().weight(0.25f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0E2035)).padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NeonButton("LB", accentColor = CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 30.dp, onPress = { setButton(4, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "LB", gamepadButton = 4, isGamepad = true)) } })
            NeonButton("RB", accentColor = CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 30.dp, onPress = { setButton(5, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "RB", gamepadButton = 5, isGamepad = true)) } })
            NeonTrigger("LT", accentColor = Color(0xFFE91E63), threshold = triggerThreshold, onPress = { setButton(6, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "LT", gamepadButton = 6, isGamepad = true)) } })
            NeonTrigger("RT", accentColor = Color(0xFFE91E63), threshold = triggerThreshold, onPress = { setButton(7, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "RT", gamepadButton = 7, isGamepad = true)) } })
            NeonButton("Sel", accentColor = CyanNeon.copy(alpha = 0.6f), width = 36.dp, height = 28.dp, onPress = { setButton(8, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "Sel", gamepadButton = 8, isGamepad = true)) } })
            NeonButton("St", accentColor = CyanNeon.copy(alpha = 0.6f), width = 36.dp, height = 28.dp, onPress = { setButton(9, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "St", gamepadButton = 9, isGamepad = true)) } })
        }
    }
}

@Composable
private fun ColumnScope.LandscapeGamepad(
    leftX: Float, leftY: Float, rightX: Float, rightY: Float,
    isLeftTouched: Boolean, isRightTouched: Boolean,
    onLeftTouch: (Float, Float, Boolean) -> Unit,
    onRightTouch: (Float, Float, Boolean) -> Unit,
    setButton: (Int, Boolean) -> Unit,
    triggerThreshold: Float = 0f,
    onCapture: ((LayoutKey) -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            NeonTrigger("LT", accentColor = Color(0xFFE91E63), width = 64.dp, height = 26.dp, threshold = triggerThreshold, onPress = { setButton(6, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "LT", gamepadButton = 6, isGamepad = true)) } })
            NeonButton("LB", accentColor = CyanNeon.copy(alpha = 0.8f), width = 40.dp, height = 26.dp, onPress = { setButton(4, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "LB", gamepadButton = 4, isGamepad = true)) } })
            NeonButton("RB", accentColor = CyanNeon.copy(alpha = 0.8f), width = 40.dp, height = 26.dp, onPress = { setButton(5, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "RB", gamepadButton = 5, isGamepad = true)) } })
            NeonTrigger("RT", accentColor = Color(0xFFE91E63), width = 64.dp, height = 26.dp, threshold = triggerThreshold, onPress = { setButton(7, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "RT", gamepadButton = 7, isGamepad = true)) } })
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.weight(0.4f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                NeonStick("L", onTouch = onLeftTouch, modifier = Modifier.size(100.dp))
                Spacer(Modifier.height(8.dp))
                NeonDPad(onPress = setButton, dpadSize = 120.dp, onCapture = onCapture)
            }

            Column(Modifier.weight(0.2f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                NeonButton("Sel", accentColor = CyanNeon.copy(alpha = 0.6f), width = 40.dp, height = 26.dp, onPress = { setButton(8, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "Sel", gamepadButton = 8, isGamepad = true)) } })
                Spacer(Modifier.height(8.dp))
                NeonButton("St", accentColor = CyanNeon.copy(alpha = 0.6f), width = 40.dp, height = 26.dp, onPress = { setButton(9, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "St", gamepadButton = 9, isGamepad = true)) } })
            }

            Column(Modifier.weight(0.4f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                NeonStick("R", onTouch = onRightTouch, modifier = Modifier.size(100.dp))
                Spacer(Modifier.height(8.dp))
                NeonABXY(onPress = setButton, onCapture = onCapture)
            }
        }
    }
}

@Composable
private fun NeonButton(label: String, accentColor: Color = CyanNeon, width: androidx.compose.ui.unit.Dp = 44.dp, height: androidx.compose.ui.unit.Dp = 36.dp, onPress: (Boolean) -> Unit, onCapture: ((LayoutKey) -> Unit)? = null) {
    var pressed by remember { mutableStateOf(false) }
    val capturedKey = remember(label) { LayoutKey(label = label, gamepadButton = 0) }
    Box(
        modifier = Modifier.size(width = width, height = height).clip(RoundedCornerShape(10.dp))
            .background(if (pressed) accentColor.copy(alpha = 0.25f) else Color(0x0800E5FF))
            .border(1.5.dp, if (pressed) accentColor else accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .then(
                if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
                else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) { pressed = true; onPress(true); change.consume() }
                            else { pressed = false; onPress(false) }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (pressed) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(accentColor.copy(alpha = 0.3f), cornerRadius = CornerRadius(8f))
            }
        }
        Text(label, fontSize = if (label.length <= 2) 11.sp else 9.sp, fontWeight = FontWeight.Bold, color = if (pressed) accentColor else accentColor.copy(alpha = 0.7f))
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

@Composable
private fun NeonTrigger(
    label: String, accentColor: Color = CyanNeon,
    width: androidx.compose.ui.unit.Dp = 52.dp, height: androidx.compose.ui.unit.Dp = 30.dp,
    onPress: (Boolean) -> Unit,
    onCapture: ((LayoutKey) -> Unit)? = null,
    /** Fraction of the trigger's height that must be dragged before it fires. */
    threshold: Float = 0f
) {
    var pressed by remember { mutableStateOf(false) }
    val capturedKey = remember(label) { LayoutKey(label = label, gamepadButton = 0) }

    // A plain tap is always a full pull. A downward drag is a partial pull and
    // only fires once it passes [threshold] — so the setting behaves like a
    // hair-trigger vs heavy-trigger adjustment without needing analog HID.
    Box(
        modifier = Modifier.size(width = width, height = height).clip(RoundedCornerShape(14.dp))
            .background(if (pressed) accentColor.copy(alpha = 0.2f) else Color(0x0400E5FF))
            .border(1.5.dp, if (pressed) accentColor else accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .then(
                if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
                else Modifier.pointerInput(threshold) {
                    awaitPointerEventScope {
                        val travelPx = size.height.toFloat()
                        val engagePx = (travelPx * threshold.coerceIn(0f, 1f))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val dragged = change.position.y
                                // Drag case: engage only past the threshold.
                                // Tap case (no meaningful drag): fire immediately.
                                val isDrag = dragged > 6f
                                val nowPressed = if (isDrag) dragged >= engagePx else true
                                if (nowPressed != pressed) { pressed = nowPressed; onPress(nowPressed) }
                                change.consume()
                            } else if (pressed) {
                                pressed = false; onPress(false)
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (pressed) {
                val fillW = size.width * 0.8f
                drawRoundRect(accentColor.copy(alpha = 0.25f),
                    topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
                    size = Size(fillW, size.height * 0.7f),
                    cornerRadius = CornerRadius(8f))
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (pressed) accentColor else accentColor.copy(alpha = 0.6f))
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

@Composable
private fun NeonDPad(onPress: (Int, Boolean) -> Unit, dpadSize: androidx.compose.ui.unit.Dp = 130.dp, onCapture: ((LayoutKey) -> Unit)? = null) {
    Column(Modifier.size(dpadSize), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        NeonButton("\u25B2", CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 32.dp, onPress = { onPress(12, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "DPad Up", gamepadButton = 12, isGamepad = true)) } })
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonButton("\u25C0", CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 32.dp, onPress = { onPress(13, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "DPad Left", gamepadButton = 13, isGamepad = true)) } })
            Box(Modifier.size(width = 38.dp, height = 32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x0800E5FF)).border(1.dp, CyanNeon.copy(alpha = 0.15f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(12.dp)) { drawCircle(CyanNeon.copy(alpha = 0.3f), 4f, Offset(size.width / 2, size.height / 2)) }
            }
            NeonButton("\u25B6", CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 32.dp, onPress = { onPress(15, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "DPad Right", gamepadButton = 15, isGamepad = true)) } })
        }
        Spacer(Modifier.height(2.dp))
        NeonButton("\u25BC", CyanNeon.copy(alpha = 0.8f), width = 38.dp, height = 32.dp, onPress = { onPress(14, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "DPad Down", gamepadButton = 14, isGamepad = true)) } })
    }
}

@Composable
private fun NeonABXY(onPress: (Int, Boolean) -> Unit, onCapture: ((LayoutKey) -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NeonButton("Y", Color(0xFFFFEB3B), width = 40.dp, height = 34.dp, onPress = { onPress(3, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "Y", gamepadButton = 3, isGamepad = true)) } })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NeonButton("X", Color(0xFF2196F3), width = 40.dp, height = 34.dp, onPress = { onPress(2, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "X", gamepadButton = 2, isGamepad = true)) } })
            NeonButton("B", Color(0xFFF44336), width = 40.dp, height = 34.dp, onPress = { onPress(1, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "B", gamepadButton = 1, isGamepad = true)) } })
        }
        NeonButton("A", Color(0xFF4CAF50), width = 40.dp, height = 34.dp, onPress = { onPress(0, it) }, onCapture = onCapture?.let { cap -> { cap(LayoutKey(label = "A", gamepadButton = 0, isGamepad = true)) } })
    }
}
