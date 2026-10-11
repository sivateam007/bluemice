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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.BleHidManager
import com.example.bluemice.HidReportBuilder
import com.example.bluemice.model.KeyboardLanguage
import com.example.bluemice.model.KeyboardLanguageCatalog
import com.example.bluemice.model.LangKey
import com.example.bluemice.ui.theme.*

private val KC = HidReportBuilder.KeyCodes

fun sendChar(bleManager: BleHidManager, c: Char, language: KeyboardLanguage?) {
    if (language != null) {
        language.findChar(c.toString())?.let { (code, level) ->
            val mods = when (level) {
                1 -> KC.MOD_LEFT_SHIFT
                2 -> KC.MOD_RIGHT_ALT
                3 -> KC.MOD_RIGHT_ALT or KC.MOD_LEFT_SHIFT
                else -> 0
            }
            bleManager.sendKeyPress(mods, code)
            return
        }
    }
    val (key, mod) = KC.charToKey(c)
    if (key != 0.toByte()) bleManager.sendKeyPress(mod, key)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageKeyboard(
    bleManager: BleHidManager,
    language: KeyboardLanguage,
    onPickLanguage: () -> Unit
) {
    var shifted by remember { mutableStateOf(false) }
    var caps by remember { mutableStateOf(false) }
    var altGr by remember { mutableStateOf(false) }

    fun currentLevel(k: LangKey): Int {
        val letter = k.base.length == 1 && k.base[0].isLetter()
        val shiftOn = shifted || (caps && letter)
        return when {
            altGr && shifted -> 3
            altGr -> 2
            shiftOn -> 1
            else -> 0
        }
    }

    fun charKey(k: LangKey) {
        val level = currentLevel(k)
        val mods = when (level) {
            1 -> KC.MOD_LEFT_SHIFT
            2 -> KC.MOD_RIGHT_ALT
            3 -> KC.MOD_RIGHT_ALT or KC.MOD_LEFT_SHIFT
            else -> 0
        }
        bleManager.sendKeyPress(mods, k.code)
        if (shifted && k.base.length == 1 && k.base[0].isLetter() && !caps) shifted = false
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPickLanguage) {
                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(language.name, color = TextSecondary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
        }

        val keyBg = Color(0xFF2D2D2D)
        val fnBg = Color(0xFF3A3A3A)

        @Composable
        fun LKey(label: String, sub: String?, modifier: Modifier, accent: Boolean = false, onClick: () -> Unit) {
            Box(
                modifier = modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else keyBg)
                    .border(1.dp, if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFF3A3A4A), RoundedCornerShape(6.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (accent) MaterialTheme.colorScheme.primary else TextPrimary, maxLines = 1)
                    if (sub != null && sub != label) Text(sub, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                }
            }
        }

        // Letter rows from the physical grid
        language.rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { k ->
                    val level = currentLevel(k)
                    val label = language.charAtLevel(k, level)
                    val sub = language.charAtLevel(k, if (level == 1) 0 else 1)
                    LKey(label, sub, Modifier.weight(1f), onClick = { charKey(k) })
                }
            }
        }

        // Modifier / navigation rows
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LKey("Shift", null, Modifier.weight(1.2f), accent = shifted) { shifted = !shifted }
            LKey("Caps", null, Modifier.weight(1.2f), accent = caps) { caps = !caps }
            LKey("AltGr", null, Modifier.weight(1.2f), accent = altGr) { altGr = !altGr }
            LKey("Tab", null, Modifier.weight(1.2f), onClick = { bleManager.sendKeyPress(0, KC.KEY_TAB) })
            LKey("BkSp", null, Modifier.weight(1.4f), onClick = { bleManager.sendKeyPress(0, KC.KEY_BACKSPACE) })
            LKey("Enter", null, Modifier.weight(1.4f), onClick = { bleManager.sendKeyPress(0, KC.KEY_ENTER) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LKey("Ctrl", null, Modifier.weight(1.2f), onClick = { bleManager.sendKeyPress(KC.MOD_LEFT_CTRL, 0.toByte()) })
            LKey("Alt", null, Modifier.weight(1.2f), onClick = { bleManager.sendKeyPress(KC.MOD_LEFT_ALT, 0.toByte()) })
            LKey(" ", null, Modifier.weight(3.4f), onClick = { bleManager.sendKeyPress(0, KC.KEY_SPACE) })
            LKey("\u25C0", null, Modifier.weight(0.9f), onClick = { bleManager.sendKeyPress(0, KC.KEY_ARROW_LEFT) })
            LKey("\u25B6", null, Modifier.weight(0.9f), onClick = { bleManager.sendKeyPress(0, KC.KEY_ARROW_RIGHT) })
            LKey("Del", null, Modifier.weight(0.9f), onClick = { bleManager.sendKeyPress(0, KC.KEY_DELETE) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    currentLocale: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val languages = KeyboardLanguageCatalog.all
    val filtered = remember(query) {
        if (query.isBlank()) languages
        else languages.filter { it.name.contains(query, ignoreCase = true) || it.locale.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = CardSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Language", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A3A)).padding(horizontal = 10.dp)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    singleLine = true
                )
            }
            LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                items(filtered, key = { it.locale }) { lang ->
                    val selected = lang.locale == currentLocale
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onPick(lang.locale) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.name, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (selected) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
