package com.example.bluemice.ui

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG relative luminance and contrast ratio.
 *
 * Key faces are drawn from whatever colour the user picks, so "can I still read
 * this?" used to be a guess - a pale green key with light grey text is barely
 * legible and nothing in the app said so. These are the standard formulas, and
 * they are what the colour picker's live readout and the Auto text colour are
 * built on.
 */

private fun channel(value: Int): Double {
    val v = (value and 0xFF) / 255.0
    return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

/** 0.0 (black) .. 1.0 (white) for an ARGB or RGB long. */
fun relativeLuminance(color: Long): Double =
    0.2126 * channel((color shr 16).toInt()) +
        0.7152 * channel((color shr 8).toInt()) +
        0.0722 * channel(color.toInt())

/** WCAG contrast ratio, 1.0 .. 21.0. 4.5 is the body-text threshold. */
fun contrastRatio(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = max(la, lb)
    val lo = min(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}

/** Just off pure black, so a key face never has to fight a crushed shadow. */
private const val NEAR_BLACK = 0xFF121418L

/** Just off pure white, so a key face never glares. */
private const val NEAR_WHITE = 0xFFF4F6F8L

/**
 * The readable text colour for a key face, used whenever a key leaves its text
 * on Auto. Auto previously always meant the theme's light text, which vanished
 * against pale key colours like the green Enter or the amber volume keys. This
 * picks whichever end of the range actually contrasts instead.
 */
fun autoTextColor(background: Long): Long =
    if (contrastRatio(background, NEAR_WHITE) >= contrastRatio(background, NEAR_BLACK)) NEAR_WHITE else NEAR_BLACK

/** True when a pair clears the WCAG AA body-text threshold. */
fun isReadable(ratio: Double) = ratio >= 4.5

/** "#RRGGBB" for a colour long, or "" if it is not a real colour. */
fun colorToHex(color: Long): String =
    "#%06X".format(color and 0xFFFFFFL)

/** Parses "#RRGGBB" or "RRGGBB" back into a colour long, or null. */
fun hexToColor(text: String): Long? {
    val cleaned = text.trim().removePrefix("#").removePrefix("0x")
    if (cleaned.length != 6) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return 0xFF000000L or value
}
