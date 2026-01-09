package com.campus.lostfound.ui.theme

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

// ===== COLOR PALETTE DEFINITIONS =====
// Setiap palette punya light & dark variant

enum class ThemeColor(
    val displayName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color
) {
    DEFAULT(
        displayName = "Default (Biru Navy)",
        primaryLight = Color(0xFF0A1628),
        primaryDark = Color(0xFF8EBBFF),
        secondaryLight = Color(0xFF00C896),
        secondaryDark = Color(0xFF00C896),
        tertiaryLight = Color(0xFF0085FF),
        tertiaryDark = Color(0xFF82B1FF)
    ),
    OCEAN_BLUE(
        displayName = "Ocean Blue",
        primaryLight = Color(0xFF1565C0),
        primaryDark = Color(0xFF90CAF9),
        secondaryLight = Color(0xFF0097A7),
        secondaryDark = Color(0xFF80DEEA),
        tertiaryLight = Color(0xFF00838F),
        tertiaryDark = Color(0xFF4DD0E1)
    ),
    TEAL(
        displayName = "Teal",
        primaryLight = Color(0xFF00796B),
        primaryDark = Color(0xFF80CBC4),
        secondaryLight = Color(0xFF00897B),
        secondaryDark = Color(0xFF4DB6AC),
        tertiaryLight = Color(0xFF26A69A),
        tertiaryDark = Color(0xFF80CBC4)
    ),
    GREEN(
        displayName = "Forest Green",
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFF81C784),
        secondaryLight = Color(0xFF43A047),
        secondaryDark = Color(0xFFA5D6A7),
        tertiaryLight = Color(0xFF66BB6A),
        tertiaryDark = Color(0xFFC8E6C9)
    ),
    PURPLE(
        displayName = "Royal Purple",
        primaryLight = Color(0xFF5E35B1),
        primaryDark = Color(0xFFB39DDB),
        secondaryLight = Color(0xFF7E57C2),
        secondaryDark = Color(0xFFD1C4E9),
        tertiaryLight = Color(0xFF9575CD),
        tertiaryDark = Color(0xFFEDE7F6)
    ),
    PINK(
        displayName = "Rose Pink",
        primaryLight = Color(0xFFAD1457),
        primaryDark = Color(0xFFF48FB1),
        secondaryLight = Color(0xFFC2185B),
        secondaryDark = Color(0xFFF8BBD9),
        tertiaryLight = Color(0xFFE91E63),
        tertiaryDark = Color(0xFFFCE4EC)
    ),
    ORANGE(
        displayName = "Sunset Orange",
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFFB74D),
        secondaryLight = Color(0xFFEF6C00),
        secondaryDark = Color(0xFFFFCC80),
        tertiaryLight = Color(0xFFF57C00),
        tertiaryDark = Color(0xFFFFE0B2)
    ),
    RED(
        displayName = "Cherry Red",
        primaryLight = Color(0xFFC62828),
        primaryDark = Color(0xFFEF9A9A),
        secondaryLight = Color(0xFFD32F2F),
        secondaryDark = Color(0xFFFFCDD2),
        tertiaryLight = Color(0xFFE53935),
        tertiaryDark = Color(0xFFFFEBEE)
    ),
    BROWN(
        displayName = "Warm Brown",
        primaryLight = Color(0xFF5D4037),
        primaryDark = Color(0xFFBCAAA4),
        secondaryLight = Color(0xFF6D4C41),
        secondaryDark = Color(0xFFD7CCC8),
        tertiaryLight = Color(0xFF795548),
        tertiaryDark = Color(0xFFEFEBE9)
    ),
    GREY(
        displayName = "Cool Grey",
        primaryLight = Color(0xFF455A64),
        primaryDark = Color(0xFFB0BEC5),
        secondaryLight = Color(0xFF546E7A),
        secondaryDark = Color(0xFFCFD8DC),
        tertiaryLight = Color(0xFF607D8B),
        tertiaryDark = Color(0xFFECEFF1)
    )
}

// Function to create Light Color Scheme based on ThemeColor
fun createLightColorScheme(themeColor: ThemeColor) = lightColorScheme(
    primary = themeColor.primaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = themeColor.primaryLight.copy(alpha = 0.12f),
    onPrimaryContainer = themeColor.primaryLight,
    secondary = themeColor.secondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = themeColor.secondaryLight.copy(alpha = 0.12f),
    onSecondaryContainer = themeColor.secondaryLight,
    tertiary = themeColor.tertiaryLight,
    onTertiary = Color(0xFFFFFFFF),
    error = LostRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = LostRedLight,
    onErrorContainer = LostRedDark,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFE0E0E0)
)

// Function to create Dark Color Scheme based on ThemeColor
fun createDarkColorScheme(themeColor: ThemeColor) = darkColorScheme(
    primary = themeColor.primaryDark,
    onPrimary = Color(0xFF000000),
    primaryContainer = themeColor.primaryDark.copy(alpha = 0.24f),
    onPrimaryContainer = themeColor.primaryDark,
    secondary = themeColor.secondaryDark,
    onSecondary = Color(0xFF000000),
    secondaryContainer = themeColor.secondaryDark.copy(alpha = 0.24f),
    onSecondaryContainer = themeColor.secondaryDark,
    tertiary = themeColor.tertiaryDark,
    onTertiary = Color(0xFF000000),
    error = LostRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = LostRedDark,
    onErrorContainer = Color(0xFFFFFFFF),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// Legacy color schemes for backward compatibility
private val DarkColorScheme = createDarkColorScheme(ThemeColor.DEFAULT)
private val LightColorScheme = createLightColorScheme(ThemeColor.DEFAULT)


@Composable
fun CampusLostFoundTheme(
    themeMode: String = "system", // "system", "light", "dark"
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    dynamicColor: Boolean = false, // Disabled by default to use custom colors
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDarkTheme // "system"
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(themeColor)
        else -> createLightColorScheme(themeColor)
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Dynamic status bar color based on theme
            window.statusBarColor = colorScheme.surface.toArgb()
            // Always use light status bar icons for premium dark primary
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

