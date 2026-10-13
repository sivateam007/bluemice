package com.example.bluemice.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

private val KC = HidReportBuilder.KeyCodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MouseScreen(
    bleManager: BleHidManager,
    pointerSpeed: Float = 1f,
    scrollSpeed: Float = 1f,
    onScrollSpeedChange: (Float) -> Unit = {},
    onPointerSpeedChange: (Float) -> Unit = {},
    onEditModeToggle: (Boolean) -> Unit = {},
    layoutManager: LayoutManager? = null,
    settingsManager: SettingsManager? = null
) {
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }
    var isMiddlePressed by remember { mutableStateOf(false) }
    var mouseX by remember { mutableFloatStateOf(0f) }
    var mouseY by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }
    var localScrollSpeed by remember { mutableStateOf(scrollSpeed) }
    var localPointerSpeed by remember { mutableStateOf(pointerSpeed) }
    var showKeyboard by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var textInput by remember { mutableStateOf("") }
    var remainderX by remember { mutableFloatStateOf(0f) }
    var remainderY by remember { mutableFloatStateOf(0f) }
    val currentSpeed by rememberUpdatedState(localPointerSpeed)

    val context = LocalContext.current

    // M3 mouse settings
    var tapToClick by remember { mutableStateOf(settingsManager?.tapToClick ?: true) }
    var invertScroll by remember { mutableStateOf(settingsManager?.invertScroll ?: false) }
    var showScrollBar by remember { mutableStateOf(settingsManager?.showScrollBar ?: true) }
    var scrollBarLeft by remember { mutableStateOf(settingsManager?.scrollBarLeft ?: false) }
    var mouseJiggleMode by remember { mutableStateOf(settingsManager?.mouseJiggleMode ?: "disabled") }
    var penMode by remember { mutableStateOf(settingsManager?.penMode ?: false) }
    var airMouseEnabled by remember { mutableStateOf(settingsManager?.airMouseEnabled ?: false) }
    val gestureScope = rememberCoroutineScope()

    val tapToClickState by rememberUpdatedState(tapToClick)
    val invertScrollState by rememberUpdatedState(invertScroll)
    val pointerSpeedState by rememberUpdatedState(localPointerSpeed)
    val scrollSpeedState by rememberUpdatedState(localScrollSpeed)
    val penModeState by rememberUpdatedState(penMode)
    val airEnabledState by rememberUpdatedState(airMouseEnabled)
    val airSensState by rememberUpdatedState(settingsManager?.airMouseSensitivity ?: 1f)
    val jiggleModeState by rememberUpdatedState(mouseJiggleMode)
    val jigglerIntervalState by rememberUpdatedState(settingsManager?.jigglerIntervalSeconds ?: 30)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentLayout = layoutManager?.getCurrentLayout("mouse")
    val hasCustomKeys = currentLayout != null && currentLayout.rows.any { it.keys.isNotEmpty() }

    // Inline editing state
    var isEditing by remember { mutableStateOf(false) }
    var editRows: List<MutableList<LayoutKey>> by remember { mutableStateOf(currentLayout?.rows?.map { it.keys.toMutableList() } ?: listOf(mutableListOf())) }
    var showAddKeySheet by remember { mutableStateOf(false) }
    var showEditKeySheet by remember { mutableStateOf(false) }
    var editRowIdx by remember { mutableIntStateOf(-1) }
    var editKeyIdx by remember { mutableIntStateOf(-1) }
    var editingKey by remember { mutableStateOf(LayoutKey("")) }

    // Position picker state
    var pendingKey by remember { mutableStateOf<LayoutKey?>(null) }
    var showPositionPicker by remember { mutableStateOf(false) }
    // Set when the user tapped a row's + slot - the key then lands at that row's end.
    var addTargetRow by remember { mutableIntStateOf(-1) }
    var pickerTargetRow by remember { mutableIntStateOf(0) }
    var pickerTargetPos by remember { mutableIntStateOf(0) }

    val layoutHistory = remember { LayoutHistory() }
    fun pushUndoSnapshot() { layoutHistory.push(editRows.map { it.toList() }) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) layoutManager?.let { lm -> runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(lm.exportLayout("mouse", lm.getCurrentLayoutName("mouse"))?.toByteArray()) }
        } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@runCatching
            if (layoutManager?.importLayout(json) == true) {
                val name = try { org.json.JSONObject(json).optString("name") } catch (_: Exception) { "" }
                layoutManager.getAllLayouts("mouse").find { l -> l.name == name }?.let { imported ->
                    pushUndoSnapshot()
                    editRows = imported.rows.map { it.keys.toMutableList() }
                    layoutManager.setCurrentLayoutName("mouse", name)
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
            val name = lm.getCurrentLayoutName("mouse")
            // Always keep the current name. Renaming an empty layout to a fixed
            // "Custom X" would silently overwrite any existing layout with that name.
            val layoutName = name
            lm.saveCustomLayout(LayoutDefinition(layoutName, rows.map { LayoutRow(it.toList()) }, "mouse"))
            lm.setCurrentLayoutName("mouse", layoutName)
        }
    }

    // ------------------------------------------------------------- touchpad
    // The pointer surface is a real layout key (keyType = "touchpad"): the layout
    // decides whether it exists and where it sits, while this local composable
    // owns the actual gestures. It reaches the renderer through widgetSlots.
    @Composable
    fun TouchpadWidget(modifier: Modifier) {
        Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            var lastTapTime = 0L
                            var pendingClickJob: Job? = null
                            val doubleTapWindow = 280L
                            val slop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val isPen = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                                if (isPen) {
                                    var lastPos = down.position
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val c = event.changes.firstOrNull { it.id == down.id }
                                        if (c == null || !c.pressed) break
                                        val dx = c.position.x - lastPos.x
                                        val dy = c.position.y - lastPos.y
                                        lastPos = c.position
                                        if (dx != 0f || dy != 0f) {
                                            val rawDx = dx * pointerSpeedState + remainderX
                                            val rawDy = dy * pointerSpeedState + remainderY
                                            val ix = rawDx.toInt().coerceIn(-127, 127)
                                            val iy = rawDy.toInt().coerceIn(-127, 127)
                                            remainderX = rawDx - ix
                                            remainderY = rawDy - iy
                                            bleManager.sendMouseReport(0, ix, iy, 0)
                                        }
                                    }
                                    return@awaitEachGesture
                                }
                                var moved = false
                                var multi = false
                                var didScroll = false
                                val positions = mutableMapOf(down.id to down.position)
                                var lastCentroid = down.position
                                var scrollRemainder = 0f
                                fun currentButtons(): Int = (if (isLeftPressed) 0x01 else 0) or
                                        (if (isRightPressed) 0x02 else 0) or
                                        (if (isMiddlePressed) 0x04 else 0)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { c ->
                                        if (c.pressed) positions[c.id] = c.position else positions.remove(c.id)
                                    }
                                    val pressedChanges = event.changes.filter { it.pressed }
                                    val count = pressedChanges.size
                                    if (count == 0) {
                                        if (!moved && !multi) {
                                            if (tapToClickState) {
                                                val now = System.currentTimeMillis()
                                                val pending = pendingClickJob
                                                if (pending != null && now - lastTapTime < doubleTapWindow) {
                                                    pending.cancel()
                                                    pendingClickJob = null
                                                    bleManager.sendMouseClick(0x01)
                                                    bleManager.sendMouseClick(0x01)
                                                } else {
                                                    pendingClickJob?.cancel()
                                                    pendingClickJob = gestureScope.launch {
                                                        delay(doubleTapWindow)
                                                        bleManager.sendMouseClick(0x01)
                                                        pendingClickJob = null
                                                    }
                                                    lastTapTime = now
                                                }
                                            }
                                        } else if (multi && !didScroll) {
                                            bleManager.sendMouseClick(0x02)
                                        }
                                        break
                                    }
                                    if (count >= 2) multi = true
                                    val centroid = pressedChanges.map { it.position }
                                        .reduce { a, b -> Offset(a.x + b.x, a.y + b.y) } / count.toFloat()
                                    val delta = centroid - lastCentroid
                                    lastCentroid = centroid
                                    if (!moved && delta.getDistance() > slop) {
                                        moved = true
                                        pendingClickJob?.cancel()
                                        pendingClickJob = null
                                    }
                                    if (moved) {
                                        if (multi) {
                                            var raw = delta.y * scrollSpeedState
                                            if (invertScrollState) raw = -raw
                                            scrollRemainder += raw
                                            val ticks = scrollRemainder.toInt()
                                            if (ticks != 0) {
                                                scrollRemainder -= ticks
                                                didScroll = true
                                                bleManager.sendMouseReport(0, 0, 0, ticks.coerceIn(-127, 127))
                                            }
                                        } else {
                                            val rawDx = delta.x * pointerSpeedState + remainderX
                                            val rawDy = delta.y * pointerSpeedState + remainderY
                                            val dx = rawDx.toInt().coerceIn(-127, 127)
                                            val dy = rawDy.toInt().coerceIn(-127, 127)
                                            remainderX = rawDx - dx
                                            remainderY = rawDy - dy
                                            bleManager.sendMouseReport(currentButtons(), dx, dy, 0)
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                var last: Offset? = null
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (!penModeState) { last = null; continue }
                                    val pen = event.changes.firstOrNull {
                                        it.type == PointerType.Stylus || it.type == PointerType.Eraser
                                    }
                                    if (pen == null) { last = null; continue }
                                    if (pen.pressed) { last = pen.position; continue }
                                    val pos = pen.position
                                    val prev = last
                                    last = pos
                                    if (prev != null) {
                                        val dx = pos.x - prev.x
                                        val dy = pos.y - prev.y
                                        if (dx != 0f || dy != 0f) {
                                            val rawDx = dx * pointerSpeedState + remainderX
                                            val rawDy = dy * pointerSpeedState + remainderY
                                            val ix = rawDx.toInt().coerceIn(-127, 127)
                                            val iy = rawDy.toInt().coerceIn(-127, 127)
                                            remainderX = rawDx - ix
                                            remainderY = rawDy - iy
                                            bleManager.sendMouseReport(0, ix, iy, 0)
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDDB1", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Drag to move cursor", color = TextSecondary, fontSize = 13.sp)
                        Text("Tap to click", color = TextSecondary.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }

                // Scroll bar on the edge
                if (showScrollBar) {
                    Box(
                        modifier = Modifier
                            .align(if (scrollBarLeft) Alignment.CenterStart else Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(26.dp)
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A3A4A))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var lastY = down.position.y
                                    var rem = 0f
                                    var everMoved = false
                                    var scrollJob: Job? = null
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!c.pressed) break
                                            val dy = c.position.y - lastY
                                            lastY = c.position.y
                                            if (abs(dy) > 1f) {
                                                everMoved = true
                                                scrollJob?.cancel()
                                                scrollJob = null
                                                var raw = dy * scrollSpeedState
                                                if (invertScrollState) raw = -raw
                                                rem += raw
                                                val ticks = rem.toInt()
                                                if (ticks != 0) {
                                                    rem -= ticks
                                                    bleManager.sendMouseReport(0, 0, 0, ticks.coerceIn(-127, 127))
                                                }
                                            } else if (scrollJob == null && !everMoved) {
                                                // Finger is resting: after a short hold, start
                                                // repeating. The delay means a quick tap lifts
                                                // before the job runs and sends nothing here.
                                                val dir = if (invertScrollState) -1 else 1
                                                scrollJob = gestureScope.launch {
                                                    delay(220)
                                                    while (isActive) {
                                                        bleManager.sendMouseReport(0, 0, 0, dir)
                                                        delay(120)
                                                    }
                                                }
                                            }
                                        }
                                    } finally {
                                        val wasRunning = scrollJob?.isActive == true
                                        scrollJob?.cancel()
                                        // A clean tap (no drag, released before the repeat
                                        // kicked in) still scrolls exactly one notch.
                                        if (!everMoved && !wasRunning) {
                                            val dir = if (invertScrollState) -1 else 1
                                            bleManager.sendMouseReport(0, 0, 0, dir)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (scrollBarLeft) "‹" else "›", color = TextSecondary, fontSize = 12.sp)
                    }
                }
        }
    }

    // Widgets the renderer drops into the grid at the matching key position, so the
    // pad and scroll wheel can be moved, resized or deleted like any other key.
    val touchpadSlot: @Composable (Modifier) -> Unit = { m -> TouchpadWidget(m) }
    val scrollSlot: @Composable (Modifier) -> Unit = { m ->
        ScrollWheel(
            bleManager = bleManager,
            scrollSpeed = { scrollSpeedState },
            invertScroll = { invertScrollState },
            modifier = m
        )
    }
    val widgetSlots: Map<String, @Composable (Modifier) -> Unit> = mapOf(
        "touchpad" to touchpadSlot,
        "scroll" to scrollSlot
    )

    // Mouse jiggler - periodically move the mouse
    LaunchedEffect(mouseJiggleMode) {
        if (mouseJiggleMode == "disabled") return@LaunchedEffect
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        while (true) {
            delay(jigglerIntervalState * 1000L)
            val screenOn = pm.isInteractive
            if (mouseJiggleMode == "screen_on" && !screenOn) continue
            bleManager.sendMouseReport(0, 1, 0, 0)
            delay(50)
            bleManager.sendMouseReport(0, -1, 0, 0)
        }
    }

    // Air mouse - control cursor with device tilt
    LaunchedEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) return@LaunchedEffect
        var gx = 0f; var gy = 0f; var gz = 0f
        var initialized = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                if (!airEnabledState) { initialized = false; return }
                val alpha = 0.25f
                if (!initialized) {
                    gx = e.values[0]; gy = e.values[1]; gz = e.values[2]
                    initialized = true
                    return
                }
                gx = alpha * e.values[0] + (1f - alpha) * gx
                gy = alpha * e.values[1] + (1f - alpha) * gy
                gz = alpha * e.values[2] + (1f - alpha) * gz
                val ax = (gx / 9.81f).coerceIn(-1f, 1f)
                val ay = (gy / 9.81f).coerceIn(-1f, 1f)
                val sens = airSensState
                val dx = ax * sens * 60f
                val dy = -ay * sens * 60f
                if (abs(dx) < 0.6f && abs(dy) < 0.6f) return
                bleManager.sendMouseReport(0, dx.toInt().coerceIn(-127, 127), dy.toInt().coerceIn(-127, 127), 0)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        try { awaitCancellation() } finally { sensorManager.unregisterListener(listener) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header row with action buttons
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showKeyboard = !showKeyboard; if (showKeyboard) focusRequester.requestFocus() }) {
                    Icon(Icons.Default.Keyboard, null, tint = if (showKeyboard) MaterialTheme.colorScheme.primary else TextSecondary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { localPointerSpeed = pointerSpeed; localScrollSpeed = scrollSpeed; showSettings = true }) {
                    Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = {
                    isEditing = !isEditing
                    onEditModeToggle(isEditing)
                    if (isEditing) {
                        if (editRows.all { it.isEmpty() }) {
                            val defaultLayout = layoutManager?.getCurrentLayout("mouse")
                            if (defaultLayout != null && defaultLayout.rows.any { it.keys.isNotEmpty() }) {
                                editRows = defaultLayout.rows.map { it.keys.toMutableList() }
                            } else {
                                editRows = mutableListOf(mutableListOf(LayoutKey("Left Click", mouseButton = 1, width = 1.5f)), mutableListOf(LayoutKey("Right Click", mouseButton = 2, width = 1.5f)))
                            }
                        }
                    } else {
                        saveLayout(editRows)
                    }
                }) {
                    Icon(Icons.Default.Edit, null, tint = if (isEditing) Color(0xFF4CAF50) else TextSecondary, modifier = Modifier.size(22.dp))
                }
            }

            // Collapsible keyboard input
            AnimatedVisibility(visible = showKeyboard) {
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(CardSurface).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(if (textInput.isEmpty()) TextSecondary else SuccessGreen))
                        Spacer(Modifier.width(6.dp))
                        var prevLen by remember { mutableStateOf(0) }
                        BasicTextField(
                            value = textInput,
                            onValueChange = { newText ->
                                if (newText.length < prevLen) {
                                    val deleted = prevLen - newText.length
                                    repeat(deleted) { bleManager.sendKeyPress(0, KC.KEY_BACKSPACE) }
                                } else {
                                    newText.drop(prevLen).forEach { c ->
                                        val (key, mod) = KC.charToKey(c)
                                        if (key != 0.toByte()) bleManager.sendKeyPress(mod, key)
                                    }
                                }
                                prevLen = newText.length
                                textInput = newText
                            },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester).padding(vertical = 10.dp),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            singleLine = true
                        )
                        if (textInput.isEmpty()) {
                            Text("Type with Gboard...", color = TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF3A3A3A))
                            .clickable { textInput = ""; focusRequester.requestFocus() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Clear", color = Color(0xFF888888), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Custom keys section (replaces hardcoded elements during edit)
            if (isEditing) {
                Spacer(Modifier.height(6.dp))
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
                Spacer(Modifier.height(6.dp))
            } else if (!isEditing) {
                val layoutEmpty = currentLayout == null || currentLayout.rows.none { it.keys.isNotEmpty() }
                if (layoutEmpty) {
                    EmptyLayoutState(
                        message = "Your mouse layout has no keys",
                        onRestoreDefault = { layoutManager?.resetToDefault("mouse") },
                        onAddKeys = { isEditing = true }
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    CustomLayoutView(
                        currentLayout!!, bleManager,
                        modifier = Modifier.padding(bottom = 6.dp),
                        widgetSlots = widgetSlots,
                        flexibleRows = true
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

        }

        // Edit mode bottom bar
        if (isEditing) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                EditModeBar(
                    layoutName = layoutManager?.getCurrentLayoutName("mouse") ?: "Custom Mouse",
                    layoutNames = layoutManager?.let { lm -> lm.getAllLayouts("mouse").map { it.name } } ?: emptyList(),
                    hasKeys = editRows.any { it.isNotEmpty() },
                    onLoadLayout = { name ->
                        val layout = layoutManager?.let { lm -> lm.getAllLayouts("mouse").find { l -> l.name == name } }
                        if (layout != null) {
                            pushUndoSnapshot()
                            editRows = layout.rows.map { it.keys.toMutableList() }
                            layoutManager?.setCurrentLayoutName("mouse", name)
                        }
                    },
                    onSave = { saveLayout(editRows) },
                    onAddKey = { showAddKeySheet = true },
                    onAddRow = { pushUndoSnapshot(); editRows = editRows + listOf(mutableListOf<LayoutKey>()) },
                    onNewBlank = { pushUndoSnapshot(); editRows = listOf(mutableListOf<LayoutKey>()) },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onExport = { exportLauncher.launch("bluemice-mouse-layout.json") },
                    canUndo = layoutHistory.undoStack.isNotEmpty(),
                    canRedo = layoutHistory.redoStack.isNotEmpty(),
                    onUndo = { layoutHistory.undo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onRedo = { layoutHistory.redo(editRows)?.let { editRows = it.map { r -> r.toMutableList() } } },
                    onExit = { isEditing = false; onEditModeToggle(false); saveLayout(editRows) }
                )
            }
        }
    }

    // Add key sheet → sets pending key then shows position picker
    if (showAddKeySheet) {
        AddKeySheet(
            mode = "mouse",
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
                val src = if (tpl.isEmpty()) null else lm.getAllLayouts("mouse").find { it.name == tpl }
                if (src != null) {
                    pushUndoSnapshot()
                    editRows = src.rows.map { r -> r.keys.toMutableList() }
                    lm.setCurrentLayoutName("mouse", src.name)
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

    // Edit key sheet
    if (showEditKeySheet && editRowIdx >= 0 && editKeyIdx >= 0) {
        val key = editingKey
        EditKeySheet(
            key = key,
            onApply = { newKey -> val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr[editKeyIdx] = newKey; er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onDelete = { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); nr.removeAt(editKeyIdx); er[editRowIdx] = nr; editRows = er; showEditKeySheet = false },
            onMoveLeft = { if (editKeyIdx > 0) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx-1]; nr[editKeyIdx-1] = t; er[editRowIdx] = nr; editRows = er } },
            onMoveRight = { if (editKeyIdx < editRows[editRowIdx].size - 1) { val er = editRows.toMutableList(); val nr = er[editRowIdx].toMutableList(); val t = nr[editKeyIdx]; nr[editKeyIdx] = nr[editKeyIdx+1]; nr[editKeyIdx+1] = t; er[editRowIdx] = nr; editRows = er } },
            onEditMacro = {},
            onDismiss = { showEditKeySheet = false }
        )
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState,
            containerColor = CardSurface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mouse Settings", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                Column {
                    Text("Pointer Speed: ${"%.1f".format(localPointerSpeed)}x",
                        color = TextPrimary, fontSize = 13.sp)
                    Slider(
                        value = localPointerSpeed,
                        onValueChange = { localPointerSpeed = it.coerceIn(0.5f, 5f) },
                        valueRange = 0.5f..5f, steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Column {
                    Text("Scroll Speed: ${"%.1f".format(localScrollSpeed)}x",
                        color = TextPrimary, fontSize = 13.sp)
                    Slider(
                        value = localScrollSpeed,
                        onValueChange = { localScrollSpeed = it.coerceIn(0.5f, 5f) },
                        valueRange = 0.5f..5f, steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                SettingsRow("Invert scroll direction") {
                    Switch(checked = invertScroll, onCheckedChange = { invertScroll = it; settingsManager?.invertScroll = it })
                }

                SettingsRow("Touch pad mouse clicks (tap to click)") {
                    Switch(checked = tapToClick, onCheckedChange = { tapToClick = it; settingsManager?.tapToClick = it })
                }

                // "Mouse buttons" visibility/position used to live here. It drove
                // nothing once buttons became ordinary editable layout keys, so the
                // dead toggles were removed rather than left as no-op switches.

                Column {
                    Text("Scroll bar", color = TextSecondary, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("off" to (false to false), "left" to (true to true), "right" to (true to false)).forEach { (label, pair) ->
                            val active = showScrollBar == pair.first && scrollBarLeft == pair.second
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                                .clickable {
                                    showScrollBar = pair.first
                                    scrollBarLeft = pair.second
                                    settingsManager?.showScrollBar = showScrollBar
                                    settingsManager?.scrollBarLeft = scrollBarLeft
                                }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(label, color = if (active) Color.White else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Column {
                    Text("Mouse Jiggler", color = TextSecondary, fontSize = 11.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Off" to "disabled", "Screen On" to "screen_on", "Always" to "screen_off").forEach { (label, mode) ->
                            val active = mouseJiggleMode == mode
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                                .clickable { mouseJiggleMode = mode; settingsManager?.mouseJiggleMode = mode }
                                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(label, color = if (active) Color.White else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                SettingsRow("Pen drawing mode") {
                    Switch(checked = penMode, onCheckedChange = { penMode = it; settingsManager?.penMode = it })
                }

                SettingsRow("Air Mouse") {
                    Switch(checked = airMouseEnabled, onCheckedChange = { airMouseEnabled = it; settingsManager?.airMouseEnabled = it })
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            onScrollSpeedChange(localScrollSpeed)
                            onPointerSpeedChange(localPointerSpeed)
                            showSettings = false
                        }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Apply", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun MouseButtonRow(
    bleManager: BleHidManager,
    isLeftPressed: Boolean, isRightPressed: Boolean, isMiddlePressed: Boolean,
    onLeftDown: () -> Unit, onLeftUp: () -> Unit,
    onMiddleDown: () -> Unit, onMiddleUp: () -> Unit,
    onRightDown: () -> Unit, onRightUp: () -> Unit,
    showLeft: Boolean, showMiddle: Boolean, showRight: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showLeft) {
            MouseButtonBox("Left", Color(0xFF4CAF50), Modifier.weight(1f),
                onDown = onLeftDown, onUp = onLeftUp, onCapture = null)
        }
        if (showMiddle) {
            MouseButtonBox("Middle", Color(0xFF2196F3), Modifier.weight(1f),
                onDown = onMiddleDown, onUp = onMiddleUp, onCapture = null)
        }
        if (showRight) {
            MouseButtonBox("Right", Color(0xFFF44336), Modifier.weight(1f),
                onDown = onRightDown, onUp = onRightUp, onCapture = null)
        }
    }
}

@Composable
private fun ArrowPadBtn(label: String, onClick: () -> Unit, onCapture: ((LayoutKey) -> Unit)? = null) {
    var pressed by remember { mutableStateOf(false) }
    val code = when (label) { "\u25B2" -> KC.KEY_ARROW_UP; "\u25BC" -> KC.KEY_ARROW_DOWN; "\u25C0" -> KC.KEY_ARROW_LEFT; "\u25B6" -> KC.KEY_ARROW_RIGHT; else -> 0.toByte() }
    val capturedKey = remember(label, code) { LayoutKey(label = label, keyCode = code) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) Color(0xFF5C6BC0) else Color(0xFF2A2A3A))
            .border(1.dp, if (pressed) Color(0xFF7986CB) else Color(0xFF3A3A4A), RoundedCornerShape(10.dp))
            .then(
                if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
                else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) { pressed = true; onClick(); change.consume() }
                            else { pressed = false }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (pressed) Color.White else Color(0xFFB0BEC5), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(10.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

@Composable
private fun MouseButtonBox(
    label: String, color: Color, modifier: Modifier,
    onDown: () -> Unit, onUp: () -> Unit,
    onCapture: ((LayoutKey) -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val btnCode = when (label) { "Left" -> 1; "Middle" -> 4; "Right" -> 2; else -> 0 }
    val capturedKey = remember(label, btnCode) { LayoutKey(label = "$label Click", mouseButton = btnCode) }
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) color else color.copy(alpha = 0.6f))
            .then(
                if (onCapture != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCapture(capturedKey) }
                else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) { pressed = true; onDown(); change.consume() }
                            else { pressed = false; onUp() }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (onCapture != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).background(Color(0x2200E5FF)),
                contentAlignment = Alignment.TopEnd) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp).padding(2.dp))
            }
        }
    }
}

/**
 * Dedicated scroll control.
 *
 * Behaviour, top to bottom:
 *  - Upper half: press-and-hold to scroll up (repeats after a short delay).
 *  - Lower half: press-and-hold to scroll down (repeats after a short delay).
 *  - Drag vertically anywhere: free scroll, scaled by [scrollSpeed].
 *
 * The repeat deliberately waits before the first tick so a quick tap sends
 * exactly one notch instead of a burst, and a drag never triggers the repeat.
 */
@Composable
fun ScrollWheel(
    bleManager: BleHidManager,
    scrollSpeed: () -> Float,
    invertScroll: () -> Boolean,
    modifier: Modifier = Modifier,
    onTick: ((Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()

    /** Applies direction + speed inversion and reports the wheel movement. */
    fun emit(raw: Int) {
        if (raw == 0) return
        var v = raw * scrollSpeed()
        if (invertScroll()) v = -v
        val t = v.toInt().coerceIn(-127, 127)
        if (t == 0) return
        bleManager.sendMouseReport(0, 0, 0, t)
        onTick?.invoke(t)
    }

    // Fractional carry so sub-1 speeds accumulate instead of rounding to zero.
    var carry by remember { mutableFloatStateOf(0f) }
    fun emitFractional(raw: Float) {
        carry += raw
        val whole = carry.toInt()
        if (whole != 0) { carry -= whole; emit(whole) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF2A3A4A))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .pointerInput(bleManager) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Which half determines the default repeat direction.
                    val half = size.height / 2f
                    val dir = if (down.position.y < half) -1 else 1

                    var lastY = down.position.y
                    var dragged = false
                    var movedSinceDown = 0f
                    var repeatJob: Job? = null

                    // Only start repeating once the finger has stayed put.
                    // A drag sets [dragged] and therefore never repeats.
                    fun startRepeat() {
                        if (repeatJob != null || dragged) return
                        repeatJob = scope.launch {
                            delay(220)
                            while (isActive) {
                                emit(dir)
                                delay(90)
                            }
                        }
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val c = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break

                            val dy = c.position.y - lastY
                            lastY = c.position.y
                            movedSinceDown += abs(dy)

                            if (abs(dy) >= 1f) {
                                // Past slop this becomes a free-scroll drag.
                                if (movedSinceDown > viewConfiguration.touchSlop) {
                                    dragged = true
                                    repeatJob?.cancel(); repeatJob = null
                                }
                                if (dragged) emitFractional(dy / 12f)
                            }
                        }
                    } finally {
                        repeatJob?.cancel()
                    }

                    if (!dragged) {
                        // Clean tap or hold: wait for the finger to lift, emitting
                        // after a short hold. A quick release cancels before the
                        // first tick, so we send exactly one notch instead.
                        var emittedFirst = false
                        val holdJob = scope.launch {
                            delay(220)
                            emittedFirst = true
                            emit(dir)
                            while (isActive) { delay(90); emit(dir) }
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.id == down.id && it.pressed }) break
                            }
                        } finally {
                            holdJob.cancel()
                        }
                        if (!emittedFirst) emit(dir)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Up
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("▲", color = TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            // Knurl
            Column(Modifier.clip(RoundedCornerShape(2.dp)).background(CardBorder)
                .padding(horizontal = 1.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { Box(Modifier.width(10.dp).height(1.5.dp).background(TextSecondary.copy(alpha = 0.5f))) }
            }
            // Down
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("▼", color = TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
    }
}
