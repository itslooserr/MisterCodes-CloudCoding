package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF001F18),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
  )

private val PremiumDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFFFD700), // Royal Amber Gold
    secondary = Color(0xFFF39C12), // Warm Bronze Accent
    tertiary = Color(0xFFFFE082), // Soft Light Gold
    background = Color(0xFF09090D), // Premium Jet Slate
    surface = Color(0xFF13141F), // Obsidian Card Surface
    onPrimary = Color(0xFF1E1E2C),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFF8F9FA),
    onSurface = Color(0xFFECEFF1)
  )

private val PremiumLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF8C7A3E), // Olive Antique Gold
    secondary = Color(0xFFB3923B), // Soft Gold
    tertiary = Color(0xFF2C3E50),
    background = Color(0xFFFAF9F6), // Warm Alabaster
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1E),
    onSurface = Color(0xFF2E2E32)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  isPremium: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      isPremium -> {
        if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme
      }
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Color.Transparent.toArgb()
      window.navigationBarColor = Color.Transparent.toArgb()
      
      // Make status bar and navigation bar icons readable
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
