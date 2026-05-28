package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = GreenSecondaryDark,
    tertiary = GreenTertiaryDark,
    background = GreenBackgroundDark,
    surface = GreenSurfaceDark,
    onPrimary = GreenOnPrimaryDark,
    onBackground = GreenOnBackgroundDark,
    onSurface = GreenOnSurfaceDark,
    surfaceVariant = GreenSurfaceVariantDark,
    onSurfaceVariant = GreenOnSurfaceVariantDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GreenPrimaryLight,
    secondary = GreenSecondaryLight,
    tertiary = GreenTertiaryLight,
    background = GreenBackgroundLight,
    surface = GreenSurfaceLight,
    onPrimary = GreenOnPrimaryLight,
    onBackground = GreenOnBackgroundLight,
    onSurface = GreenOnSurfaceLight,
    surfaceVariant = GreenSurfaceVariantLight,
    onSurfaceVariant = GreenOnSurfaceVariantLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ but we disable it to preserve our clean green theme brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
