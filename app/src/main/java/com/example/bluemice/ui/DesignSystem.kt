package com.example.bluemice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluemice.ui.theme.CardBorder
import com.example.bluemice.ui.theme.CardSurface
import com.example.bluemice.ui.theme.NavyDark
import com.example.bluemice.ui.theme.TextPrimary
import com.example.bluemice.ui.theme.TextSecondary

/**
 * Shared visual language for every key-based screen.
 *
 * Before this existed each screen picked its own background (gradient, flat
 * navy, `SurfaceDark`, or nothing at all), its own title styling and its own
 * content frame - which is exactly why the app read as six separate apps
 * stitched together rather than one product.
 */

/** The single background every screen sits on. */
val ScreenBackground: Brush =
    Brush.verticalGradient(listOf(NavyDark, Color(0xFF0C1F33)))

/** The one accent used for screen titles and active states. */
val AccentCyan = Color(0xFF00E5FF)

/** Radius / spacing scale, so nothing is an arbitrary one-off. */
object Dimens {
    val PanelRadius = 14.dp
    val KeyRadius = 10.dp
    val ControlRadius = 8.dp
    val ChipRadius = 6.dp
    val Gap = 6.dp
    /** Minimum comfortable touch target for a tappable control. */
    val TouchTarget = 36.dp
}

/** Applies [ScreenBackground] to a screen root. */
fun Modifier.screenBackground(): Modifier =
    this.background(ScreenBackground)

/**
 * The framed surface every screen's content lives in.
 *
 * [scroll] adds a *single* vertical scroll container nested inside the frame,
 * so the rounded border and fill stay put while the content moves under them.
 * Screens whose root already scrolls must leave this false - two nested vertical
 * scrollables crash with an infinity-constraint error.
 */
@Composable
fun ScreenPanel(
    modifier: Modifier = Modifier,
    scroll: Boolean = false,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(Dimens.PanelRadius)
    // Padding sits *outside* the scroll container so the inset stays put while
    // content moves under it; putting it inside would make the top gutter scroll away.
    var inner: Modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(CardSurface)
        .border(1.dp, CardBorder, shape)
        .padding(horizontal = 6.dp, vertical = 6.dp)
    if (scroll) inner = inner.verticalScroll(rememberScrollState())
    Box(
        modifier = modifier.then(inner),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Consistent screen header: accent bar, title, optional subtitle, then the
 * screen's actions pushed to the trailing edge.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = AccentCyan,
    scrollActions: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(if (subtitle == null) 17.dp else 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
        val actionsModifier = if (scrollActions) {
            Modifier.horizontalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Row(actionsModifier, verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/** Small uppercase group label used above a block of controls. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, accent: Color = AccentCyan) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text.uppercase(),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Rounded pill used for the compact actions in [ScreenHeader]. Gives every
 * header control the same shape, hit area and colour ramp instead of each
 * screen inventing its own.
 */
@Composable
fun HeaderAction(
    modifier: Modifier = Modifier,
    background: Color = CardSurface,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .height(Dimens.TouchTarget)
            .clip(RoundedCornerShape(Dimens.ControlRadius))
            .background(background)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Layout switcher that sits directly under [ScreenHeader] on every key-based
 * screen.
 *
 * Creating a layout used to be something you could only discover inside Edit
 * mode, which meant most people never found it. This puts both halves on the
 * surface: the active layout's name opens a list of everything saved for the
 * mode, and **+ New** runs the template picker, so a new layout is one visible
 * tap away.
 *
 * The screen owns [currentName], [names] and the rows because LayoutManager is
 * backed by SharedPreferences, which Compose cannot observe - bumping the
 * screen's version state is what actually makes a switch appear.
 */
@Composable
fun LayoutBar(
    mode: String,
    currentName: String,
    names: List<String>,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    val tap = remember { MutableInteractionSource() }

    Row(
        modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.weight(1f)
                .height(Dimens.TouchTarget)
                .clip(RoundedCornerShape(Dimens.ControlRadius))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(Dimens.ControlRadius))
                .clickable(tap, indication = null) { menuOpen = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currentName.ifEmpty { "Layout" },
                    color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, "Switch layout",
                    tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (names.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No saved layouts yet", fontSize = 12.sp, color = TextSecondary) },
                        onClick = { menuOpen = false }, enabled = false)
                }
                names.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (name == currentName) {
                                    Icon(Icons.Default.Check, null, tint = AccentCyan,
                                        modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(name, fontSize = 12.sp, maxLines = 1)
                            }
                        },
                        onClick = { menuOpen = false; if (name != currentName) onSelect(name) }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Box(
            Modifier.height(Dimens.TouchTarget)
                .clip(RoundedCornerShape(Dimens.ControlRadius))
                .background(AccentCyan)
                .clickable(tap, indication = null) { showTemplates = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF062033), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("New", color = Color(0xFF062033), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showTemplates) {
        TemplatePickerDialog(
            mode = mode,
            onSelect = { template -> showTemplates = false; onCreate(template) },
            onDismiss = { showTemplates = false }
        )
    }
}
