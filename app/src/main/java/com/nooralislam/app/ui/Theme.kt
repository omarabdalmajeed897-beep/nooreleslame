package com.nooralislam.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object NoorColors {
    val Emerald = Color(0xFF0F5E4D)
    val EmeraldDark = Color(0xFF0B4A3C)
    val EmeraldGlow = Color(0xFF34D399)
    val Gold = Color(0xFFB68A35)
    val GoldLight = Color(0xFFFBBF24)
    val Parchment = Color(0xFFFAF8F4)
    val Cream = Color(0xFFF3EFE6)
    val BorderLight = Color(0xFFE5DEC9)
    val InkLight = Color(0xFF1C2422)
    val MutedLight = Color(0xFF5B6864)

    val DarkBg = Color(0xFF0B1312)
    val DarkSurface = Color(0xFF131F1D)
    val DarkSurfaceAlt = Color(0xFF0E1816)
    val DarkBorder = Color(0xFF243734)
    val DarkInk = Color(0xFFECF1EF)
    val DarkMuted = Color(0xFF9BB0AA)
}

@Composable
fun NoorTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkMode) {
        darkColorScheme(
            primary = NoorColors.EmeraldGlow,
            onPrimary = NoorColors.DarkBg,
            secondary = NoorColors.GoldLight,
            background = NoorColors.DarkBg,
            surface = NoorColors.DarkSurface,
            onBackground = NoorColors.DarkInk,
            onSurface = NoorColors.DarkInk,
            surfaceVariant = NoorColors.DarkSurfaceAlt,
            outline = NoorColors.DarkBorder
        )
    } else {
        lightColorScheme(
            primary = NoorColors.Emerald,
            onPrimary = Color.White,
            secondary = NoorColors.Gold,
            background = NoorColors.Parchment,
            surface = Color.White,
            onBackground = NoorColors.InkLight,
            onSurface = NoorColors.InkLight,
            surfaceVariant = NoorColors.Cream,
            outline = NoorColors.BorderLight
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

fun cardBorder(darkMode: Boolean) = BorderStroke(
    1.dp,
    if (darkMode) NoorColors.DarkBorder else NoorColors.BorderLight
)

@Composable
fun surfaceCardColors(darkMode: Boolean) = CardDefaults.cardColors(
    containerColor = if (darkMode) NoorColors.DarkSurface else Color.White
)

val CardShape = RoundedCornerShape(16.dp)
