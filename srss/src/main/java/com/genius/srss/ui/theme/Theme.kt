package com.genius.srss.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkDefaultColorPalette = darkColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    onPrimary = ActiveElement,
    primaryContainer = ActiveElement,
    onPrimaryContainer = OnPrimary
)

private val LightDefaultColorPalette = lightColorScheme(
    primary = Primary,
    secondary = PrimaryDark,
    onPrimary = ActiveElement,
    primaryContainer = ActiveElement,
    onPrimaryContainer = OnPrimary
)

@Composable
fun SRSSTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        if (darkTheme) {
            DarkDefaultColorPalette
        } else {
            LightDefaultColorPalette
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = if (darkTheme) {
            TypographyDark
        } else {
            TypographyLight
        },
        shapes = Shapes,
        content = content
    )
}