package com.qrtoolkit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandIndigo40,
    onPrimary = Color.White,
    secondary = NeutralGrey40,
    background = Color.White,
    surface = Color.White,
    onBackground = NeutralGrey10,
    onSurface = NeutralGrey10,
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigo80,
    onPrimary = BrandIndigo20,
    secondary = NeutralGrey90,
    background = NeutralGrey10,
    surface = NeutralGrey10,
    onBackground = NeutralGrey90,
    onSurface = NeutralGrey90,
)

@Composable
fun QrToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
