package com.example.bluemice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    var inner: Modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(CardSurface)
        .border(1.dp, CardBorder, shape)
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
