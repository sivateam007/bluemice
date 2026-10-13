package com.example.bluemice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.model.LayoutDefinition
import com.example.bluemice.model.LayoutKey
import com.example.bluemice.ui.theme.CardSurface
import com.example.bluemice.ui.theme.SuccessGreen
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

private val KCodes = HidReportBuilder.KeyCodes

/* ------------------------------------------------------------------ *
 * Live layout renderer
 * ------------------------------------------------------------------ */

/**
 * Renders a user's layout exactly as it was saved. Every key - including the
 * touchpad, scroll wheel and analog sticks - is just another [LayoutKey], so the
 * screen can hand the renderer a widget to drop in at that position.
 *
 * [widgetSlots] maps a `keyType` ("touchpad", "scroll", ...) to the composable the
 * screen wants there. The layout decides *whether* the widget exists and *where*;
 * the screen keeps full control over the gesture handling inside it.
 *
 * [flexibleRows] is opt-in and only safe inside a non-scrolling parent: a row that
 * owns a touchpad then absorbs the leftover vertical space instead of being pinned
 * to its nominal height, and a scroll wheel in that same row stretches with it so
 * the two read as one trackpad module.
 */
@Composable
fun CustomLayoutView(
    layout: LayoutDefinition,
    bleManager: BleHidManager,
    modifier: Modifier = Modifier,
    widgetSlots: Map<String, @Composable (Modifier) -> Unit> = emptyMap(),
    flexibleRows: Boolean = false,
    hapticsEnabled: Boolean = false,
    onStickTouch: (String, Float, Float, Boolean) -> Unit = { _, _, _, _ -> },
    onKeySent: ((LayoutKey) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        layout.rows.forEach { row ->
            val rowIsFlexible = flexibleRows && row.keys.any { it.keyType == "touchpad" }
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (rowIsFlexible) Modifier.weight(1f, fill = true) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                // Rows mix widget keys (80dp sticks) with 48dp keys; without this the
                // short ones would hang off the top of the row.
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.keys.forEach { key ->
                    // Modifier.weight is a RowScope extension, so the row's constraint
                    // has to be built here - inside the Row - not down in the child.
                    val rowScopeModifier = Modifier.weight(key.width.coerceAtLeast(0.5f))
                    KeyCell(
                        key = key,
                        bleManager = bleManager,
                        baseModifier = rowScopeModifier,
                        // The scroll wheel shares the hero row with the touchpad on the
                        // mouse screen, so it stretches with the pad rather than floating
                        // as a short strip in the middle of a tall cell.
                        fillHeight = rowIsFlexible &&
                            (key.keyType == "touchpad" || key.keyType == "scroll"),
                        widgetSlots = widgetSlots,
                        haptics = haptics,
                        hapticsEnabled = hapticsEnabled,
                        onStickTouch = onStickTouch,
                        onKeySent = onKeySent
                    )
                }
            }
        }
    }
}

/** Base height for the widget key types, scaled by the key's own height. */
private fun widgetHeight(key: LayoutKey): Dp {
    val base = when (key.keyType) {
        "touchpad" -> 140f
        // Sticks and the D-pad / ABXY pads share one base so they sit flush in a
        // row instead of top-aligning at three different heights, and so a
        // three-row controller still fits a landscape viewport.
        "stickL", "stickR", "dpad", "face" -> 80f
        "scroll" -> 46f
        "typeBar", "languageBoard" -> 44f
        // 48f, not 40f: Material's minimum touch target is 48x48dp, and the old
        // 40f base put every default key below spec - which is why the pads read
        // as cramped. Widgets keep their own tuned bases above.
        else -> 48f
    }
    return (base * key.height.coerceIn(0.75f, 2.5f)).dp
}

/* ------------------------------------------------------------------ *
 * Gamepad pads (D-pad cross + ABXY diamond)
 * ------------------------------------------------------------------ */

/** Shape a [DirectionPad] draws behind its labels. */
private enum class PadShape { Cross, Diamond }

/** D-pad buttons in up / down / left / right order (HID indexes 12-15). */
private val DPAD_BUTTONS = intArrayOf(12, 14, 13, 15)

/** ABXY in the same order: Y on top, A below, X left, B right. */
private val FACE_BUTTONS = intArrayOf(3, 0, 2, 1)

/** Classic Xbox face-button colours, matching [FACE_BUTTONS]' order. */
private val FACE_COLORS = listOf(
    Color(0xFFFDD835), // Y - top
    Color(0xFF66BB6A), // A - bottom
    Color(0xFF42A5F5), // X - left
    Color(0xFFEF5350)  // B - right
)

/**
 * Four-way pad shipped as a single [LayoutKey].
 *
 * The whole cell is the touch target and the offset of the press picks the
 * direction, instead of splitting the cell into four fixed sub-buttons: in
 * landscape the row is wide and short, so four fixed zones would fall under
 * the 48dp minimum while the cell itself is comfortably large. The drawing is
 * fitted to `min(width, height)` so the pad stays round at any cell aspect.
 *
 * Reports are momentary - a direction is held only while the finger is down.
 */
@Composable
private fun DirectionPad(
    bleManager: BleHidManager,
    shape: PadShape,
    buttons: IntArray,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    // 0 up, 1 down, 2 left, 3 right, -1 idle.
    var active by remember(buttons) { mutableIntStateOf(-1) }

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(Dimens.KeyRadius))
            .background(CardSurface)
            .border(
                1.dp,
                if (active >= 0) AccentCyan.copy(alpha = 0.55f) else Color(0x33FFFFFF),
                RoundedCornerShape(Dimens.KeyRadius)
            )
            .pointerInput(buttons) {
                var last = -1
                awaitPointerEventScope {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.pressed }
                        val dir = if (change == null) -1 else {
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val nx = change.position.x / w * 2f - 1f
                            val ny = change.position.y / h * 2f - 1f
                            when {
                                // Small dead centre so a careless tap does not fire a direction.
                                nx * nx + ny * ny < 0.05f -> -1
                                abs(nx) > abs(ny) -> if (nx > 0f) 3 else 2
                                else -> if (ny > 0f) 1 else 0
                            }
                        }
                        if (dir != last) {
                            last = dir
                            active = dir
                            if (dir < 0) bleManager.sendEmptyGamepadReport()
                            else bleManager.sendGamepadReport(0, 0, 0, 0, 1 shl buttons[dir])
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Everything is laid out inside a square of side min(w, h), so the pad
        // never stretches into an oval no matter how wide the row becomes.
        Box(Modifier.size(minOf(maxWidth, maxHeight)), contentAlignment = Alignment.Center) {
            val dim = Color.White.copy(alpha = 0.07f)
            val arm = 0.34f

            if (shape == PadShape.Cross) {
                fun armMod(align: Alignment) =
                    Modifier.align(align).fillMaxWidth(arm).fillMaxHeight(0.5f)
                val live = AccentCyan.copy(alpha = 0.34f)
                Box(armMod(Alignment.TopCenter)
                    .background(if (active == 0) live else dim, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)))
                Box(armMod(Alignment.BottomCenter)
                    .background(if (active == 1) live else dim, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)))
                val hArm = Modifier.fillMaxWidth(0.5f).fillMaxHeight(arm)
                Box(Modifier.align(Alignment.CenterStart).then(hArm)
                    .background(if (active == 2) live else dim, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)))
                Box(Modifier.align(Alignment.CenterEnd).then(hArm)
                    .background(if (active == 3) live else dim, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)))
                // Hub covers the four inner arm ends so the cross reads as one piece.
                Box(Modifier.align(Alignment.Center).fillMaxWidth(arm).aspectRatio(1f)
                    .background(if (active >= 0) AccentCyan.copy(alpha = 0.30f) else dim, CircleShape))

                Icon(Icons.Default.KeyboardArrowUp, "Up",
                    tint = if (active == 0) AccentCyan else TextSecondary,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(16.dp))
                Icon(Icons.Default.KeyboardArrowDown, "Down",
                    tint = if (active == 1) AccentCyan else TextSecondary,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(16.dp))
                Icon(Icons.Default.KeyboardArrowLeft, "Left",
                    tint = if (active == 2) AccentCyan else TextSecondary,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 3.dp).size(16.dp))
                Icon(Icons.Default.KeyboardArrowRight, "Right",
                    tint = if (active == 3) AccentCyan else TextSecondary,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp).size(16.dp))
            } else {
                val glyphs = listOf("Y", "A", "X", "B")
                val places = listOf(
                    Alignment.TopCenter, Alignment.BottomCenter,
                    Alignment.CenterStart, Alignment.CenterEnd
                )
                places.forEachIndexed { i, place ->
                    val on = active == i
                    Box(
                        Modifier.align(place).fillMaxWidth(0.40f).aspectRatio(1f)
                            .clip(CircleShape)
                            .background(colors[i].copy(alpha = if (on) 0.90f else 0.20f))
                            .border(1.dp, colors[i].copy(alpha = if (on) 1f else 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            glyphs[i],
                            color = if (on) Color.Black else colors[i].copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** One key inside a live row: a screen-supplied widget, or a plain pressable key. */
@Composable
private fun KeyCell(
    key: LayoutKey,
    bleManager: BleHidManager,
    baseModifier: Modifier,
    fillHeight: Boolean,
    widgetSlots: Map<String, @Composable (Modifier) -> Unit>,
    haptics: HapticFeedback,
    hapticsEnabled: Boolean,
    onStickTouch: (String, Float, Float, Boolean) -> Unit,
    onKeySent: ((LayoutKey) -> Unit)?
) {
    val slotModifier = baseModifier.then(
        if (fillHeight) Modifier.fillMaxHeight() else Modifier.height(widgetHeight(key))
    )

    // A spacer is layout geometry, not a control: it holds a gap open so keys on
    // different rows can line up (D-pad cross, ABXY diamond, shoulder clusters).
    // It paints nothing and dispatches nothing - the empty space is the feature.
    if (key.keyType == "spacer") {
        Box(slotModifier)
        return
    }

    val slot = widgetSlots[key.keyType]
    if (slot != null) {
        slot(slotModifier)
        return
    }

    when (key.keyType) {
        "scroll" -> {
            FallbackScrollWheel(bleManager = bleManager, modifier = slotModifier)
            return
        }
        "stickL", "stickR" -> {
            val stickId = if (key.keyType == "stickL") "L" else "R"
            NeonStick(
                label = stickId,
                onTouch = { x, y, touched -> onStickTouch(stickId, x, y, touched) },
                modifier = slotModifier
            )
            return
        }
        "dpad" -> {
            DirectionPad(
                bleManager = bleManager,
                shape = PadShape.Cross,
                buttons = DPAD_BUTTONS,
                colors = List(4) { AccentCyan },
                modifier = slotModifier
            )
            return
        }
        "face" -> {
            DirectionPad(
                bleManager = bleManager,
                shape = PadShape.Diamond,
                buttons = FACE_BUTTONS,
                colors = FACE_COLORS,
                modifier = slotModifier
            )
            return
        }
        "touchpad" -> {
            // No touchpad was supplied for this layout - leave a visible hole rather
            // than silently dropping the key, so the user can find and fix it.
            WidgetPlaceholder(label = key.label, hint = "Touchpad", modifier = slotModifier)
            return
        }
        "typeBar", "languageBoard" -> {
            WidgetPlaceholder(label = key.label, hint = "Text input", modifier = slotModifier)
            return
        }
    }

    // Keys had no press feedback at all - a tap looked identical to an idle key,
    // which is the biggest reason the pad read as a static picture rather than a
    // control surface. Both input paths now feed one shared `pressed` flag.
    val interactionSource = remember(key) { MutableInteractionSource() }
    val tapPressed by interactionSource.collectIsPressedAsState()
    val gamepadHeld = remember(key) { mutableStateOf(false) }

    // Gamepad keys are momentary: the report has to stay held while the finger is
    // down, so they bypass the tap path entirely.
    val pressModifier = if (key.isGamepad) {
        Modifier.pointerInput(key.gamepadButton) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val down = event.changes.any { it.pressed }
                    gamepadHeld.value = down
                    if (down) {
                        bleManager.sendGamepadReport(0, 0, 0, 0, 1 shl key.gamepadButton)
                    } else {
                        bleManager.sendEmptyGamepadReport()
                    }
                }
            }
        }
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            handleKeyPress(key, bleManager, KCodes, onKeySent)
        }
    }
    val pressed = if (key.isGamepad) gamepadHeld.value else tapPressed

    val tColor = key.textColor?.let { Color(it) } ?: TextPrimary
    val icon = keyIcon(key)
    val text = keyDisplayText(key)

    // A flat fill alone reads as unfinished. Layer a top-lit highlight over the
    // key's own colour and finish with a brighter hairline, so every key on every
    // screen picks up the same subtle depth without losing its identity.
    val keyShape = RoundedCornerShape(Dimens.KeyRadius)
    Box(
        slotModifier
            .clip(keyShape)
            .background(if (pressed) Color(key.backgroundColor).copy(alpha = 0.82f) else Color(key.backgroundColor))
            .background(
                Brush.verticalGradient(
                    // Swapped for a darkening wash while held, so the key sinks
                    // instead of catching more light.
                    if (pressed) {
                        listOf(Color.Black.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.02f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.0f))
                    }
                )
            )
            .border(
                1.dp,
                if (pressed) AccentCyan.copy(alpha = 0.65f) else Color(0x33FFFFFF),
                keyShape
            )
            .then(pressModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.10f))
        )
        when {
            key.keyType == "textBlock" -> Text(
                text,
                color = tColor,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            key.isGamepad -> Text(
                text.ifEmpty { key.label },
                color = tColor.copy(alpha = 0.92f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            icon != null && text.isEmpty() -> Icon(
                icon, contentDescription = key.label, tint = tColor, modifier = Modifier.size(18.dp)
            )
            icon != null -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(icon, contentDescription = key.label, tint = tColor, modifier = Modifier.size(14.dp))
                Text(
                    text, color = tColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1
                )
            }
            else -> Text(
                text.ifEmpty { key.label },
                color = tColor,
                fontSize = if (key.height >= 1.5f) 14.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}

@Composable
private fun WidgetPlaceholder(label: String, hint: String, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(Dimens.KeyRadius))
            .background(CardSurface)
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(Dimens.KeyRadius)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.ifEmpty { hint }, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(hint, color = TextSecondary.copy(alpha = 0.6f), fontSize = 8.sp)
        }
    }
}

/**
 * Minimal scroll wheel used when a layout has a "scroll" key but the screen did not
 * supply a widget for it (e.g. after importing a foreign layout into another mode).
 */
@Composable
fun FallbackScrollWheel(
    bleManager: BleHidManager,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardSurface)
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(8.dp))
            .pointerInput(bleManager) {
                var carry = 0f
                detectDragGestures(
                    onDragEnd = { bleManager.sendMouseReport(0, 0, 0, 0) }
                ) { change, dragAmount ->
                    change.consume()
                    carry += -dragAmount.y / 12f
                    val whole = carry.toInt()
                    if (whole != 0) {
                        carry -= whole
                        bleManager.sendMouseReport(0, 0, 0, whole.coerceIn(-127, 127))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u25B2", color = TextSecondary, fontSize = 9.sp)
            Text("\u21C5", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("\u25BC", color = TextSecondary, fontSize = 9.sp)
        }
    }
}

/**
 * Shown when a screen's layout has no keys left. Without this a user who deletes
 * their last key (or their touchpad) would be stranded with an unusable screen.
 */
@Composable
fun EmptyLayoutState(
    modifier: Modifier = Modifier,
    message: String = "This layout is empty",
    onRestoreDefault: () -> Unit,
    onAddKeys: () -> Unit
) {
    Box(modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon + two buttons rather than two lines of naked text, so the empty
            // state reads as a deliberate screen instead of a missing layout.
            Box(
                Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.12f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Widgets, null, tint = AccentCyan, modifier = Modifier.size(26.dp))
            }
            Text(message, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "You can add keys with Edit,\nor put the original layout back.",
                color = TextSecondary.copy(alpha = 0.85f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Dimens.ControlRadius))
                        .background(AccentCyan)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onAddKeys() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("+ Add keys", color = Color(0xFF03222C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Dimens.ControlRadius))
                        .background(SuccessGreen.copy(alpha = 0.16f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.45f), RoundedCornerShape(Dimens.ControlRadius))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRestoreDefault() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("Restore default", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ *
 * Edit mode
 * ------------------------------------------------------------------ */

@Composable
fun EditableLayoutView(
    rows: List<MutableList<LayoutKey>>,
    onEditKey: (Int, Int) -> Unit,
    onMoveLeft: (Int, Int) -> Unit,
    onMoveRight: (Int, Int) -> Unit,
    onMoveUp: (Int, Int) -> Unit = { _, _ -> },
    onMoveDown: (Int, Int) -> Unit = { _, _ -> },
    onDeleteKey: (Int, Int) -> Unit,
    onMoveRowUp: (Int) -> Unit,
    onMoveRowDown: (Int) -> Unit,
    onDeleteRow: (Int) -> Unit,
    onSwapKeys: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onResizeKey: (Int, Int, Float) -> Unit = { _, _, _ -> },
    onResizeHeight: (Int, Int, Float) -> Unit = { _, _, _ -> },
    onEqualizeRow: (Int) -> Unit = {},
    onAlignRow: (Int) -> Unit = {},
    onAddToRow: (Int) -> Unit = {},
    onDuplicateKey: (Int, Int) -> Unit = { _, _ -> },
    onStickTouch: (String, Float, Float, Boolean) -> Unit = { _, _, _, _ -> },
    history: LayoutHistory? = null,
    modifier: Modifier = Modifier
) {
    var dragTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var selectedKey by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // Two-step swap: arm Swap on the selected key, then tap the target key.
    var swapSource by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val pushU: () -> Unit = { history?.push(rows.map { it.toList() }) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { ri, row ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(CardSurface).padding(6.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Row ${ri + 1}", color = TextSecondary, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2A3A3A))
                            .clickable { if (ri > 0) { pushU(); onMoveRowUp(ri) } },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowUpward, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        }
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2A3A3A))
                            .clickable { if (ri < rows.size - 1) { pushU(); onMoveRowDown(ri) } },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowDownward, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        }
                        if (rows.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3A1A1A))
                                .clickable { pushU(); onDeleteRow(ri) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.size(24.dp).clip(CircleShape).background(AccentCyan.copy(alpha = 0.16f))
                            .clickable { pushU(); onEqualizeRow(ri) }, contentAlignment = Alignment.Center) {
                            Text("=1", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.size(24.dp).clip(CircleShape).background(AccentCyan.copy(alpha = 0.16f))
                            .clickable { pushU(); onAlignRow(ri) }, contentAlignment = Alignment.Center) {
                            Text("Mx", color = AccentCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (row.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp))
                            .background(CardSurface)
                            .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable { onAddToRow(ri) }, contentAlignment = Alignment.Center) {
                            Text("\uFF0B  Add keys to this row", color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEachIndexed { ki, key ->
                                val bgColor = Color(key.backgroundColor)
                                val isDragOrig = dragStart == ri to ki
                                val isTouchpad = key.keyType == "touchpad"
                                val isStick = key.keyType == "stickL" || key.keyType == "stickR"
                                val isPad = key.keyType == "dpad" || key.keyType == "face"
                                val isSpacer = key.keyType == "spacer"
                                val tColor = key.textColor?.let { Color(it) } ?: TextPrimary
                                // 48f to match widgetHeight, so a key is the same height
                                // in the editor as it will be on the live pad.
                                val keyH = (48f * key.height.coerceIn(0.7f, 2.5f)).dp
                                val currentKey by rememberUpdatedState(key)
                                val isSelected = selectedKey == ri to ki
                                Box(modifier = Modifier
                                    .weight(key.width.coerceAtLeast(0.5f))
                                    .height(keyH)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isDragOrig -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                            isSpacer -> Color.Transparent
                                            else -> bgColor
                                        }
                                    )
                                    .then(
                                        // A spacer must not look like a key, but it still has
                                        // to be findable and tappable to move or delete it.
                                        if (isSpacer) Modifier.drawBehind {
                                            val sw = 1.dp.toPx()
                                            drawRoundRect(
                                                color = if (isSelected) AccentCyan
                                                        else Color(0x38FFFFFF),
                                                style = Stroke(
                                                    width = sw,
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(9f, 7f), 0f
                                                    )
                                                ),
                                                cornerRadius = CornerRadius(8.dp.toPx())
                                            )
                                        } else Modifier
                                    )
                                    .border(1.dp, when {
                                        isDragOrig -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                                        isSelected -> Color(0xFF00E5FF)
                                        isSpacer -> Color.Transparent
                                        else -> Color(0x24FFFFFF)
                                    }, RoundedCornerShape(8.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (dragStart == null) {
                                            // A swap is pending: this tap picks the target
                                            // and completes it instead of opening the editor.
                                            val src = swapSource
                                            if (src != null) {
                                                if (src.first == ri && src.second != ki) {
                                                    pushU()
                                                    onSwapKeys(ri, src.second, ki)
                                                }
                                                swapSource = null
                                                selectedKey = src
                                            } else {
                                                selectedKey = ri to ki
                                                onEditKey(ri, ki)
                                            }
                                        }
                                    }
                                    .pointerInput(ri to ki) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                pushU()
                                                dragStart = ri to ki; dragAccum = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccum += dragAmount.x
                                                val threshold = 40f
                                                while (dragAccum > threshold && ki < row.size - 1) {
                                                    onMoveRight(ri, ki)
                                                    dragAccum -= threshold
                                                    dragStart = ri to (ki + 1)
                                                }
                                                while (dragAccum < -threshold && ki > 0) {
                                                    onMoveLeft(ri, ki)
                                                    dragAccum += threshold
                                                    dragStart = ri to (ki - 1)
                                                }
                                                val rowThreshold = 80f
                                                if (dragAmount.y < -rowThreshold && ri > 0) {
                                                    onMoveUp(ri, ki)
                                                    dragStart = (ri - 1) to (rows[ri - 1].size)
                                                    dragAccum = 0f
                                                } else if (dragAmount.y > rowThreshold && ri < rows.size - 1) {
                                                    onMoveDown(ri, ki)
                                                    dragStart = (ri + 1) to 0
                                                    dragAccum = 0f
                                                }
                                            },
                                            onDragEnd = { dragStart = null; dragAccum = 0f },
                                            onDragCancel = { dragStart = null; dragAccum = 0f }
                                        )
                                    },
                                    contentAlignment = Alignment.Center) {
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF202030))
                                                .clickable { pushU(); onMoveLeft(ri, ki) }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.NavigateBefore, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                            val eIcon = keyIcon(key)
                                            val eText = keyDisplayText(key)
                                            if (isSpacer) {
                                                Text("GAP", color = TextSecondary.copy(alpha = 0.75f),
                                                    fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                            } else if (isStick) {
                                                Text("\u26AB ${if (key.keyType == "stickL") "Stick L" else "Stick R"}",
                                                    color = Color(0xFF00E5FF).copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                            } else if (isPad) {
                                                Text(if (key.keyType == "dpad") "✛ D-Pad" else "◆ ABXY",
                                                    color = Color(0xFF00E5FF).copy(alpha = 0.65f),
                                                    fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                            } else if (isTouchpad) {
                                                Text("\uD83D\uDDB1", fontSize = if (keyH > 60.dp) 22.sp else 16.sp)
                                            } else if (eIcon != null && eText.isEmpty()) {
                                                Icon(eIcon, contentDescription = key.label, tint = tColor, modifier = Modifier.size(16.dp).padding(horizontal = 2.dp))
                                            } else if (eIcon != null && eText.isNotEmpty()) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Icon(eIcon, contentDescription = key.label, tint = tColor, modifier = Modifier.size(12.dp))
                                                    Text(eText, color = tColor, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                                }
                                            } else {
                                                Text(eText.ifEmpty { key.label }, color = tColor, fontSize = if (isTouchpad) 9.sp else 8.sp,
                                                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                                                    maxLines = 2, modifier = Modifier.padding(horizontal = 2.dp))
                                            }
                                            Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF000000).copy(alpha = 0.3f))
                                                .clickable { pushU(); onMoveRight(ri, ki) }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.NavigateNext, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A4A))
                                                .clickable { pushU(); onResizeKey(ri, ki, (key.width - 0.5f).coerceAtLeast(0.5f)) }, contentAlignment = Alignment.Center) {
                                                Text("\u2212", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("${key.width}x", color = Color(0xFF00E5FF), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 3.dp))
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A4A))
                                                .clickable { pushU(); onResizeKey(ri, ki, (key.width + 0.5f).coerceAtMost(4f)) }, contentAlignment = Alignment.Center) {
                                                Text("+", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A4A))
                                                .clickable { pushU(); onResizeHeight(ri, ki, (key.height - 0.25f).coerceAtLeast(0.75f)) }, contentAlignment = Alignment.Center) {
                                                Text("\u2212", color = Color(0xFF66BB6A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("H${key.height}x", color = Color(0xFF66BB6A), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 3.dp))
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2A4A))
                                                .clickable { pushU(); onResizeHeight(ri, ki, (key.height + 0.25f).coerceAtMost(2.5f)) }, contentAlignment = Alignment.Center) {
                                                Text("+", color = Color(0xFF66BB6A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2A3A))
                                                .clickable { if (ri > 0) { pushU(); onMoveUp(ri, ki) } }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ArrowUpward, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                                            }
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2A3A))
                                                .clickable { if (ri < rows.size - 1) { pushU(); onMoveDown(ri, ki) } }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ArrowDownward, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF3A1A1A))
                                                .clickable { pushU(); onDeleteKey(ri, ki) }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                    Box(
                                        Modifier.align(Alignment.BottomEnd).size(16.dp)
                                            .background(Color(0x5500E5FF))
                                            .pointerInput(ri to ki) {
                                                var startWidth = 0f
                                                var startHeight = 0f
                                                detectDragGestures(
                                                    onDragStart = {
                                                        pushU()
                                                        startWidth = currentKey.width; startHeight = currentKey.height
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val snapW = Math.round((startWidth + dragAmount.x / 40f) * 2f) / 2f
                                                        val snapH = Math.round((startHeight + dragAmount.y / 40f) * 4f) / 4f
                                                        onResizeKey(ri, ki, snapW.coerceIn(0.5f, 4f))
                                                        onResizeHeight(ri, ki, snapH.coerceIn(0.75f, 2.5f))
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                            // Tappable + slot: append a new key straight to this row.
                            Box(Modifier.width(26.dp).height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16162A))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .clickable { onAddToRow(ri) }, contentAlignment = Alignment.Center) {
                                Text("\uFF0B", color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        selectedKey?.let { (sri, ski) ->
            val sk = rows.getOrNull(sri)?.getOrNull(ski) ?: return@let
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(CardSurface).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Key Tools \u00B7 ${sk.label}", color = Color(0xFF64B5F6), fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Row ${sri + 1} \u00B7 #${ski + 1} \u00B7 W${sk.width}x \u00B7 H${sk.height}x", color = TextSecondary, fontSize = 9.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF3A1A1A))
                        .clickable { selectedKey = null }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(11.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolChip("\u25C0 Move", Color(0xFF64B5F6)) { pushU(); onMoveLeft(sri, ski) }
                    ToolChip("\u25B6 Move", Color(0xFF64B5F6)) { pushU(); onMoveRight(sri, ski) }
                    ToolChip("\u25B2 Row", Color(0xFF64B5F6)) { pushU(); onMoveUp(sri, ski) }
                    ToolChip("\u25BC Row", Color(0xFF64B5F6)) { pushU(); onMoveDown(sri, ski) }
                    ToolChip("Edit", Color(0xFF4CAF50)) { onEditKey(sri, ski) }
                    ToolChip(
                        if (swapSource == (sri to ski)) "Tap target\u2026" else "Swap",
                        Color(0xFFB39DDB)
                    ) {
                        // Two-step: arm here, then tap the key to swap with.
                        swapSource = if (swapSource == (sri to ski)) null else (sri to ski)
                    }
                    ToolChip("Copy", Color(0xFF4A90D9)) { pushU(); onDuplicateKey(sri, ski) }
                    ToolChip("Delete", Color(0xFFEF5350)) { pushU(); onDeleteKey(sri, ski) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", color = TextSecondary, fontSize = 9.sp)
                    Spacer(Modifier.width(6.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ToolChip("W\u2212", Color(0xFF00E5FF)) { pushU(); onResizeKey(sri, ski, (sk.width - 0.5f).coerceAtLeast(0.5f)) }
                        ToolChip("W+", Color(0xFF00E5FF)) { pushU(); onResizeKey(sri, ski, (sk.width + 0.5f).coerceAtMost(4f)) }
                        listOf(0.5f, 1f, 1.5f, 2f, 3f, 4f).forEach { w ->
                            // 0.5 and 1.5 both fall in the else branch, so label by value.
                            val label = when {
                                w % 1f == 0f -> "W${w.toInt()}"
                                w < 1f -> "W\u00BD"
                                else -> "W1\u00BD"
                            }
                            ToolChip(label, Color(0xFF00E5FF)) { pushU(); onResizeKey(sri, ski, w) }
                        }
                        ToolChip("H\u2212", Color(0xFF66BB6A)) { pushU(); onResizeHeight(sri, ski, (sk.height - 0.25f).coerceAtLeast(0.75f)) }
                        ToolChip("H+", Color(0xFF66BB6A)) { pushU(); onResizeHeight(sri, ski, (sk.height + 0.25f).coerceAtMost(2.5f)) }
                        ToolChip("=1 Row", Color(0xFFB39DDB)) { pushU(); onEqualizeRow(sri) }
                        ToolChip("Max Row", Color(0xFFB39DDB)) { pushU(); onAlignRow(sri) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolChip(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun NeonStick(label: String, onTouch: (x: Float, y: Float, touched: Boolean) -> Unit, modifier: Modifier = Modifier) {
    var position by remember { mutableStateOf(Offset.Zero) }
    var isTouched by remember { mutableStateOf(false) }
    val CyanNeon = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val touch = event.changes.firstOrNull() ?: break
                        if (touch.pressed) {
                            touch.consume()
                            // Fitted to the cell rather than a fixed pixel radius, so the
                            // throw scales with the key and the two sticks stay matched.
                            val r = minOf(size.width, size.height) * 0.40f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val raw = Offset(touch.position.x - center.x, touch.position.y - center.y)
                            val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                            val clamped = if (dist > r) Offset(raw.x / dist * r, raw.y / dist * r) else raw
                            position = clamped; isTouched = true
                            onTouch(if (r > 0f) clamped.x / r else 0f, if (r > 0f) clamped.y / r else 0f, true)
                        } else if (isTouched) {
                            position = Offset.Zero; isTouched = false; onTouch(0f, 0f, false)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Drawn as a circle fitted to the short side: the cell is often much
            // wider than tall in landscape, and clipping it to CircleShape turned
            // the stick into a stretched oval.
            val r = minOf(size.width, size.height) * 0.40f
            drawCircle(Color(0xFF101828).copy(alpha = 0.92f), r * 1.14f, center)
            drawCircle(CyanNeon.copy(alpha = 0.06f), r * 1.14f, center)
            drawCircle(CyanNeon.copy(alpha = 0.12f), r * 1.07f, center)
            drawCircle(CyanNeon.copy(alpha = 0.30f), r, center, style = Stroke(2.5f))
            drawCircle(CyanNeon.copy(alpha = 0.10f), r * 0.70f, center, style = Stroke(4f))
            if (isTouched) {
                drawCircle(CyanNeon.copy(alpha = 0.25f), r * 0.42f, center + position)
                drawCircle(CyanNeon, r * 0.30f, center + position)
            }
        }
        if (!isTouched) {
            Text(label, color = CyanNeon.copy(alpha = 0.35f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ------------------------------------------------------------------ *
 * Key helpers
 * ------------------------------------------------------------------ */

/** Icon for keys whose label is a well-known symbol; null means "show the label". */
internal fun keyIcon(key: LayoutKey): ImageVector? {
    if (key.isGamepad || key.macroSteps.isNotEmpty()) return null
    if (key.mouseButton != 0 || key.consumerCode != 0 || key.comboMods != 0) return null
    return when (key.label.trim()) {
        "\u2191", "Up" -> Icons.Default.ArrowUpward
        "\u2193", "Down" -> Icons.Default.ArrowDownward
        "\u2190", "Left" -> Icons.Default.ArrowBack
        "\u2192", "Right" -> Icons.Default.ArrowForward
        "Enter", "\u23CE", "\u21B5" -> Icons.Default.KeyboardReturn
        "BkSp", "\u232B", "Backspace" -> Icons.Default.Backspace
        "Del", "Delete" -> Icons.Default.Delete
        "Esc", "Escape" -> Icons.Default.Close
        else -> null
    }
}

/** Short label shown on a key. */
internal fun keyDisplayText(key: LayoutKey): String = when {
    key.keyType == "textBlock" -> key.textBlock.replace('\n', ' ')
    key.label.isBlank() -> "Space"
    else -> key.label
}

/** Expands the [[DATE]] / [[TIME]] / [[UUID]] tokens a text-block key may contain. */
private fun expandTokens(text: String): String {
    if (!text.contains("[[")) return text
    val now = Date()
    return text
        .replace("[[DATE]]", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now))
        .replace("[[TIME]]", SimpleDateFormat("HH:mm", Locale.getDefault()).format(now))
        .replace("[[UUID]]", UUID.randomUUID().toString())
}

private fun playMacroStep(
    step: com.example.bluemice.model.MacroStep,
    bleManager: BleHidManager,
    KC: HidReportBuilder.KeyCodes
) {
    when (step.type) {
        "key", "keystroke" -> bleManager.sendKeyPress(0, step.keyCode)
        "consumer" -> bleManager.sendConsumerPress(step.consumerCode)
        "mouse" -> bleManager.sendMouseClick(step.mouseButton)
        "text" -> step.text.forEach { c ->
            val (k, m) = KC.charToKey(c)
            if (k != 0.toByte()) bleManager.sendKeyPress(m, k)
        }
        "delay" -> Thread.sleep(step.delayMs.toLong().coerceIn(0L, 5000L))
    }
}

private fun handleKeyPress(
    key: LayoutKey,
    bleManager: BleHidManager,
    KC: HidReportBuilder.KeyCodes,
    onKeySent: ((LayoutKey) -> Unit)? = null
) {
    // Analog sticks report position, not keystrokes - KeyCell owns those.
    if (key.keyType == "stickL" || key.keyType == "stickR") return

    // App-level toggles carry no HID payload at all.
    when (key.keyType) {
        "speech", "keyboard_toggle", "fullscreen_toggle" -> {
            onKeySent?.invoke(key)
            return
        }
    }

    when {
        key.macroSteps.isNotEmpty() -> {
            val steps = key.macroSteps
            // A delay blocks, so anything with one runs off the UI thread.
            if (steps.any { it.type == "delay" }) {
                Thread { steps.forEach { playMacroStep(it, bleManager, KC) } }.start()
            } else {
                steps.forEach { playMacroStep(it, bleManager, KC) }
            }
        }

        key.comboKey != 0.toByte() -> bleManager.sendKeyPress(key.comboMods, key.comboKey)

        // Momentary gamepad buttons are handled by the press-and-hold modifier.
        key.isGamepad -> Unit

        key.mouseButton != 0 -> bleManager.sendMouseClick(key.mouseButton)
        key.consumerCode != 0 -> bleManager.sendConsumerPress(key.consumerCode)
        key.mouseScroll != 0 -> bleManager.sendMouseReport(0, 0, 0, key.mouseScroll)

        key.keyType == "textBlock" -> expandTokens(key.textBlock).forEach { c ->
            sendChar(bleManager, c, null)
        }

        // "C"/"CE"/"+-" style keys deliberately send nothing - the screen only
        // needs to hear about them to update its own readout.
        else -> if (key.keyCode != 0.toByte() || key.label.length > 1 || key.label == " ") {
            bleManager.sendKeyPress(key.comboMods, key.keyCode)
        }
    }

    onKeySent?.invoke(key)
}
