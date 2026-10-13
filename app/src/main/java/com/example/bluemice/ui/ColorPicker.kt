package com.example.bluemice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.ui.theme.CardBorder
import com.example.bluemice.ui.theme.CardSurface
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Short-lived memory of the colours the user has just applied, newest first.
 *
 * Kept in memory rather than in prefs: it is a convenience for the current
 * session, not a setting anyone would expect to survive a restart, and keeping
 * it out of SharedPreferences means no migration if the format ever changes.
 */
object RecentColors {
    private const val MAX = 10
    private val items = mutableStateListOf<Long>()
    val all: List<Long> get() = items

    fun push(color: Long) {
        val value = color and 0xFFFFFFFFL
        items.remove(value)
        items.add(0, value)
        while (items.size > MAX) items.removeAt(items.size - 1)
    }
}

/** 24 curated colours - the app's own palette plus the neutral ramp around it. */
val presetSwatches = listOf(
    0xFF121418L, 0xFF1E2126L, 0xFF2A2A3AL, 0xFF3A3A3AL, 0xFF555555L, 0xFF8A8A8AL,
    0xFF1A3A3AL, 0xFF1A5A52L, 0xFF00A8B5L, 0xFF00E5FFL, 0xFF2A4A2AL, 0xFF4CAF50L,
    0xFF3A1A1AL, 0xFF8B3A3AL, 0xFF4A2A2AL, 0xFF8B6F3AL, 0xFF3A3A1AL, 0xFFE0A030L,
    0xFF2A2A4AL, 0xFF3A3A50L, 0xFF6A3A8BL, 0xFF8B3A6FL, 0xFF4A90D9L, 0xFFEF5350L
)

/** H, S and V in the usual ranges: 0..360, 0..1, 0..1. */
private data class Hsv(val h: Float, val s: Float, val v: Float)

private fun hsvOf(color: Long): Hsv {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val hi = maxOf(r, g, b)
    val lo = minOf(r, g, b)
    val d = hi - lo
    val h = when {
        d == 0f -> 0f
        hi == r -> 60f * (((g - b) / d) % 6f)
        hi == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Hsv(if (h < 0f) h + 360f else h, if (hi == 0f) 0f else d / hi, hi)
}

private fun rgbOf(h: Float, s: Float, v: Float): Long {
    val c = v * s
    val hp = ((((h % 360f) + 360f) % 360f) / 60f)
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = v - c
    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/**
 * Colour picker for a key face or its text.
 *
 * A ring of hues around a saturation/value disc, matching how every desktop
 * colour chooser is laid out, backed by three sliders for exact values and a
 * hex box for anyone pasting a colour in. Both previews are shown at once - the
 * colour as a face with text on it, and as text on a face - because which one
 * matters depends on which of the two the user is picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    title: String,
    initial: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val start = hsvOf(initial)
    var hue by remember { mutableStateOf(start.h) }
    var sat by remember { mutableStateOf(start.s) }
    var val_ by remember { mutableStateOf(start.v) }
    val picked = rgbOf(hue, sat, val_)

    var hex by remember { mutableStateOf("") }
    var hexError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text(colorToHex(picked), color = TextSecondary, fontSize = 12.sp)
            }

            // Both readings of the colour at once: as a face, and as text on a face.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SwatchPreview(
                    caption = "On this colour",
                    modifier = Modifier.weight(1f),
                    face = picked, text = autoTextColor(picked)
                )
                SwatchPreview(
                    caption = "This colour as text",
                    modifier = Modifier.weight(1f),
                    face = presetSwatches[2], text = picked
                )
            }

            HsvWheel(
                hue = hue, saturation = sat, value = val_,
                onHue = { hue = it },
                onSv = { s, v -> sat = s; val_ = v },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            HueSlider(hue, { hue = it })
            SatSlider(sat) { sat = it }
            ValueSlider(val_) { val_ = it }

            // Hex, for anyone matching a colour from somewhere else.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hex, onValueChange = { hex = it; hexError = false },
                    singleLine = true, isError = hexError,
                    placeholder = { Text(colorToHex(picked), fontSize = 12.sp, color = TextSecondary) },
                    label = { Text("Hex", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (hex.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFF2A2A3A))
                        .clickable(enabled = hex.isNotBlank()) {
                            val parsed = hexToColor(hex)
                            if (parsed == null) {
                                hexError = true
                            } else {
                                val next = hsvOf(parsed)
                                hue = next.h; sat = next.s; val_ = next.v
                                hexError = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Set", color = if (hex.isNotBlank()) Color.White else TextSecondary, fontSize = 12.sp)
                }
            }

            SwatchGrid(presetSwatches, picked) { c ->
                val next = hsvOf(c); hue = next.h; sat = next.s; val_ = next.v
            }

            if (RecentColors.all.isNotEmpty()) {
                Text("Recent", color = TextSecondary, fontSize = 11.sp)
                SwatchGrid(RecentColors.all, picked) { c ->
                    val next = hsvOf(c); hue = next.h; sat = next.s; val_ = next.v
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A3A))
                        .clickable { onDismiss() }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = TextSecondary, fontSize = 12.sp) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary)
                        .clickable { RecentColors.push(picked); onPick(picked) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SwatchPreview(caption: String, face: Long, text: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(face)),
            contentAlignment = Alignment.Center
        ) { Text("Aa", color = Color(text), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(3.dp))
        Text(caption, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun SwatchGrid(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.chunked(12).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c ->
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == selected) 2.dp else 1.dp,
                                if (c == selected) Color.White else Color(0x33FFFFFF), CircleShape)
                            .clickable { onPick(c) }
                    )
                }
            }
        }
    }
}

private val WheelSize = 232.dp

/**
 * Hue ring around a saturation/value disc. Tapping or dragging anywhere on the
 * widget picks the colour under the finger - ring for hue, disc for saturation
 * and value - so there is no separate "mode" to get stuck in.
 */
@Composable
private fun HsvWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onHue: (Float) -> Unit,
    onSv: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val wheelPx = with(LocalDensity.current) { WheelSize.toPx() }
    val center = Offset(wheelPx / 2f, wheelPx / 2f)
    val outerR = wheelPx / 2f
    val ringWidth = wheelPx * 0.10f
    val innerR = outerR - ringWidth

    // Relative geometry only, never the current hue, so the gesture handlers can
    // stay in a pointerInput(Unit) without restarting mid-drag.
    fun applyPoint(offset: Offset) {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        if (hypot(dx, dy) >= innerR) {
            val deg = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0
            onHue(deg.toFloat())
        } else {
            onSv(
                ((dx + innerR) / (2f * innerR)).coerceIn(0f, 1f),
                ((innerR - dy) / (2f * innerR)).coerceIn(0f, 1f)
            )
        }
    }

    Canvas(
        modifier.size(WheelSize)
            .pointerInput(Unit) { detectTapGestures { applyPoint(it) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume(); applyPoint(change.position) } }
    ) {
        val arcTopLeft = Offset(center.x - outerR, center.y - outerR)
        val arcSize = Size(wheelPx, wheelPx)
        val steps = 72
        val sweep = 360f / steps
        for (i in 0 until steps) {
            val h = i * sweep
            drawArc(
                color = Color(rgbOf(h, 1f, 1f)),
                startAngle = h - sweep / 2f - 90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringWidth)
            )
        }

        // Two stacked ovals are the classic trick for a saturation/value disc:
        // a white-to-hue wash, then transparent-to-black. Each is already
        // clipped to the oval, so no path clipping is needed.
        val discTopLeft = Offset(center.x - innerR, center.y - innerR)
        val discSize = Size(innerR * 2f, innerR * 2f)
        drawOval(
            Brush.horizontalGradient(listOf(Color.White, Color(rgbOf(hue, 1f, 1f)))),
            topLeft = discTopLeft, size = discSize
        )
        drawOval(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            topLeft = discTopLeft, size = discSize
        )

        // Hue marker on the ring.
        val ringMid = outerR - ringWidth / 2f
        val rad = Math.toRadians((hue - 90.0))
        val ringPoint = Offset(
            center.x + ringMid * Math.cos(rad).toFloat(),
            center.y + ringMid * Math.sin(rad).toFloat()
        )
        drawCircle(Color.Black.copy(alpha = 0.35f), radius = ringWidth / 2f + 2.dp.toPx(), center = ringPoint)
        drawCircle(Color.White, radius = ringWidth / 2f + 1.dp.toPx(), center = ringPoint, style = Stroke(width = 2.dp.toPx()))

        // Saturation/value marker inside the disc.
        val marker = Offset(
            discTopLeft.x + discSize.width * saturation,
            discTopLeft.y + discSize.height * (1f - value)
        )
        drawCircle(Color.Black.copy(alpha = 0.35f), radius = 10.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = marker, style = Stroke(width = 2.5.dp.toPx()))
    }
}

/** One slider with its live value printed next to the label. */
@Composable
private fun VscRow(label: String, readout: String, content: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.width(6.dp))
            Text(readout, color = TextPrimary, fontSize = 11.sp)
        }
        content()
    }
}

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    VscRow("Hue", "${hue.roundToInt()}°") {
        Slider(
            value = hue, onValueChange = onChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}

@Composable
private fun SatSlider(sat: Float, onChange: (Float) -> Unit) {
    VscRow("Saturation", "${(sat * 100).roundToInt()}%") {
        Slider(
            value = sat, onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}

@Composable
private fun ValueSlider(val_: Float, onChange: (Float) -> Unit) {
    VscRow("Brightness", "${(val_ * 100).roundToInt()}%") {
        Slider(
            value = val_, onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color(0xFF3A3A3A)
            )
        )
    }
}
