package com.example.bluemice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.model.GamepadAction
import com.example.bluemice.model.GamepadConfig
import com.example.bluemice.model.defaultActionFor
import com.example.bluemice.model.gamepadActionCatalog
import com.example.bluemice.model.gamepadPreset
import com.example.bluemice.ui.theme.CardSurface
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary

private val Neon = Color(0xFF00E5FF)

/** Physical buttons, in the order they're laid out on screen. */
private data class PadButton(val bit: Int, val label: String)

private val padButtons = listOf(
    PadButton(0, "A"), PadButton(1, "B"), PadButton(2, "X"), PadButton(3, "Y"),
    PadButton(4, "LB"), PadButton(5, "RB"), PadButton(6, "LT"), PadButton(7, "RT"),
    PadButton(8, "Select"), PadButton(9, "Start"),
    PadButton(10, "L3"), PadButton(11, "R3"),
    PadButton(12, "D-pad Up"), PadButton(13, "D-pad Left"),
    PadButton(14, "D-pad Down"), PadButton(15, "D-pad Right")
)

/**
 * Full gamepad configuration: preset, per-button remap, stick tuning and
 * trigger threshold. Writes straight through to [onConfigChange], which the
 * host persists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadSettingsSheet(
    config: GamepadConfig,
    onConfigChange: (GamepadConfig) -> Unit,
    padOpacity: Float = 1f,
    onPadOpacityChange: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local copy so sliders don't thrash prefs on every frame; committed on change.
    var draft by remember { mutableStateOf(config) }
    var editingBit by remember { mutableStateOf<Int?>(null) }

    fun commit(next: GamepadConfig) {
        draft = next
        onConfigChange(next)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gamepad Settings", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    commit(gamepadPreset(GamepadConfig.PRESET_XBOX))
                }) { Text("Reset", color = Neon, fontSize = 12.sp) }
            }

            // ---------------------------------------------------------- presets
            SectionLabel("Preset")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    GamepadConfig.PRESET_XBOX,
                    GamepadConfig.PRESET_NINTENDO,
                    GamepadConfig.PRESET_WASD
                ).forEach { name ->
                    val selected = draft.preset == name
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Neon.copy(alpha = 0.22f) else Color(0xFF2A2A3A))
                            .clickable { commit(gamepadPreset(name)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            color = if (selected) Neon else TextSecondary
                        )
                    }
                }
            }
            Text(
                when (draft.preset) {
                    GamepadConfig.PRESET_NINTENDO ->
                        "Swaps A<->B and X<->Y to match Nintendo's diamond."
                    GamepadConfig.PRESET_WASD ->
                        "Face buttons and shoulders become keyboard keys."
                    else -> "Standard Xbox layout — matches the HID descriptor."
                },
                color = TextSecondary, fontSize = 10.sp
            )

            // ------------------------------------------------------------ sticks
            SectionLabel("Sticks")
            SliderRow(
                label = "Deadzone",
                value = draft.stickDeadzone, valueText = "%.0f%%".format(draft.stickDeadzone * 100),
                range = 0f..0.5f
            ) { commit(draft.copy(stickDeadzone = it)) }
            SliderRow(
                label = "Sensitivity",
                value = draft.stickSensitivity, valueText = "%.2fx".format(draft.stickSensitivity),
                range = 0.25f..3f
            ) { commit(draft.copy(stickSensitivity = it)) }

            ToggleRow("Invert Left X", draft.invertLeftX) { commit(draft.copy(invertLeftX = it)) }
            ToggleRow("Invert Left Y", draft.invertLeftY) { commit(draft.copy(invertLeftY = it)) }
            ToggleRow("Invert Right X", draft.invertRightX) { commit(draft.copy(invertRightX = it)) }
            ToggleRow("Invert Right Y", draft.invertRightY) { commit(draft.copy(invertRightY = it)) }

            // --------------------------------------------------------- triggers
            SectionLabel("Triggers")
            SliderRow(
                label = "Digital threshold",
                value = draft.triggerThreshold, valueText = "%.0f%%".format(draft.triggerThreshold * 100),
                range = 0f..1f
            ) { commit(draft.copy(triggerThreshold = it)) }

            // ------------------------------------------------------ play mode
            SectionLabel("Play Mode")
            SliderRow(
                label = "Pad opacity",
                value = padOpacity, valueText = "%.0f%%".format(padOpacity * 100),
                range = 0.2f..1f
            ) { onPadOpacityChange(it) }
            Text(
                "Used in play mode (landscape + Fullscreen), where the pad is the only thing on screen. " +
                    "Drop it so the game stays visible underneath.",
                color = TextSecondary, fontSize = 10.sp
            )

            // ---------------------------------------------------------- remap
            SectionLabel("Button Remap")
            Text(
                "Tap a button to send a different input to the PC.",
                color = TextSecondary, fontSize = 10.sp
            )
            padButtons.forEach { pb ->
                val current = draft.overrides[pb.bit] ?: defaultActionFor(pb.bit)
                RemapRow(
                    label = pb.label,
                    action = current,
                    onClick = { editingBit = pb.bit }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    editingBit?.let { bit ->
        ActionPickerSheet(
            title = "Remap ${padButtons.firstOrNull { it.bit == bit }?.label ?: "Button $bit"}",
            selected = draft.overrides[bit] ?: defaultActionFor(bit),
            onPick = { action ->
                // A plain gamepad target matching the bit is the default, so
                // drop the override rather than storing a no-op entry.
                val nextOverrides = if (action.kind == GamepadAction.Kind.GAMEPAD && action.code == bit) {
                    draft.overrides - bit
                } else {
                    draft.overrides + (bit to action)
                }
                commit(draft.copy(overrides = nextOverrides, preset = "Custom"))
                editingBit = null
            },
            onDismiss = { editingBit = null }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(Neon))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Neon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(valueText, color = Neon, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Neon, activeTrackColor = Neon)
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Neon, checkedThumbColor = Color(0xFF00323A))
        )
    }
}

@Composable
private fun RemapRow(label: String, action: GamepadAction, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF22222E))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(84.dp))
        Spacer(Modifier.weight(1f))
        Text(action.label, color = Neon, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text("›", color = TextSecondary, fontSize = 14.sp)
    }
}

/**
 * Grouped picker of everything a gamepad button can be remapped to: another
 * gamepad button, a keyboard key, a shortcut, media, mouse, or nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerSheet(
    title: String,
    selected: GamepadAction,
    onPick: (GamepadAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val catalog = remember { gamepadActionCatalog() }

    val groups = listOf(
        "Gamepad" to catalog.filter { it.kind == GamepadAction.Kind.GAMEPAD },
        "Keyboard" to catalog.filter { it.kind == GamepadAction.Kind.KEYBOARD },
        "Media" to catalog.filter { it.kind == GamepadAction.Kind.CONSUMER },
        "Mouse" to catalog.filter { it.kind == GamepadAction.Kind.MOUSE },
        "Other" to catalog.filter { it.kind == GamepadAction.Kind.HEX }
    ).filter { it.second.isNotEmpty() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            groups.forEach { (groupName, actions) ->
                Text(groupName, color = Neon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp))

                // Two columns keeps the list from running off the screen.
                actions.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { action ->
                            val isSel = action.id == selected.id
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Neon.copy(alpha = 0.22f) else Color(0xFF2A2A3A))
                                    .clickable { onPick(action) }.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    action.label, fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) Neon else TextPrimary
                                )
                            }
                        }
                        // Pad an odd last row so both columns keep equal width.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
