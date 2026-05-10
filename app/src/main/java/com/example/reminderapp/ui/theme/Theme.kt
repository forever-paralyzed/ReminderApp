package com.example.reminderapp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private const val THEME_PREFS = "theme_prefs"
private const val KEY_PRIMARY_COLOR = "primary_color"
private const val KEY_SECONDARY_COLOR = "secondary_color"
private const val KEY_TERTIARY_COLOR = "tertiary_color"
private const val KEY_THEME_SET = "is_theme_set"
private const val KEY_THEME_MODE = "theme_mode" // New preference for theme mode

enum class ThemeMode {
    LIGHT, DARK, CUSTOM
}

fun saveThemeColor(context: Context, key: String, color: Color) {
    val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(key, color.toArgb())
        // We don't automatically set CUSTOM mode here anymore, letting user choose
        .apply()
}

fun saveThemeMode(context: Context, mode: ThemeMode) {
    val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_THEME_MODE, mode.name)
        .apply()
}

fun getThemeColor(context: Context, key: String, defaultColor: Color): Color {
    val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    val colorInt = prefs.getInt(key, defaultColor.toArgb())
    return Color(colorInt)
}

fun getThemeMode(context: Context): ThemeMode {
    val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
    return try {
        ThemeMode.valueOf(modeName ?: ThemeMode.LIGHT.name)
    } catch (e: Exception) {
        ThemeMode.LIGHT
    }
}

fun isCustomThemeSet(context: Context): Boolean {
    // Compatibility for old preference if needed, or check mode
    val prefs = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    // If theme mode is set to CUSTOM, then custom theme is active
    val modeName = prefs.getString(KEY_THEME_MODE, null)
    if (modeName != null) {
        return modeName == ThemeMode.CUSTOM.name
    }
    // Fallback to old boolean check for backward compatibility
    return prefs.getBoolean(KEY_THEME_SET, false)
}

// Ручная реализация смешивания цветов, чтобы не зависеть от ColorUtils и избежать крашей
private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    val inverseRatio = 1f - ratio
    val r = color1.red * inverseRatio + color2.red * ratio
    val g = color1.green * inverseRatio + color2.green * ratio
    val b = color1.blue * inverseRatio + color2.blue * ratio
    val a = color1.alpha * inverseRatio + color2.alpha * ratio
    return Color(r, g, b, a)
}

// Ручное определение контрастного цвета (черный или белый)
private fun getContrastColor(color: Color): Color {
    // Формула яркости (Luminance)
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

private fun generateCustomColorScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    isDark: Boolean
): ColorScheme {
    
    val baseBackground = if (isDark) Color.Black else Color.White
    
    // 1-й цвет (primary) -> Фон (Background) с прозрачностью 30% (смешиваем с базовым фоном)
    // ratio 0.3 означает 30% цвета primary и 70% цвета baseBackground
    val background = blendColors(baseBackground, primary, 0.3f)
    val surface = background 

    // 2-й цвет (secondary) -> Шапка/Подвал
    val mainColor = secondary

    // 3-й цвет (tertiary) -> Кнопки
    val buttonColor = tertiary
    
    val onBackground = getContrastColor(background)
    val onSurface = getContrastColor(surface)
    
    val primaryContainerColor = mainColor
    val onPrimaryContainerColor = getContrastColor(primaryContainerColor)

    val primaryActionColor = buttonColor
    val onPrimaryActionColor = getContrastColor(primaryActionColor)

    val secondaryActionColor = buttonColor 
    val onSecondaryActionColor = getContrastColor(secondaryActionColor)
    
    val secondaryContainerColor = buttonColor.copy(alpha = 0.8f) 
    val onSecondaryContainerColor = getContrastColor(secondaryContainerColor)


    return if (isDark) {
        darkColorScheme(
            primary = primaryActionColor, 
            onPrimary = onPrimaryActionColor,
            primaryContainer = primaryContainerColor, 
            onPrimaryContainer = onPrimaryContainerColor,
            
            secondary = secondaryActionColor, 
            onSecondary = onSecondaryActionColor,
            secondaryContainer = secondaryContainerColor,
            onSecondaryContainer = onSecondaryContainerColor,
            
            tertiary = secondaryActionColor, 
            onTertiary = onSecondaryActionColor,
            tertiaryContainer = secondaryContainerColor,
            onTertiaryContainer = onSecondaryContainerColor,
            
            background = background, 
            onBackground = onBackground,
            surface = surface, 
            onSurface = onSurface,
            surfaceVariant = primaryContainerColor, 
            onSurfaceVariant = onPrimaryContainerColor
        )
    } else {
        lightColorScheme(
            primary = primaryActionColor, 
            onPrimary = onPrimaryActionColor,
            primaryContainer = primaryContainerColor, 
            onPrimaryContainer = onPrimaryContainerColor,
            
            secondary = secondaryActionColor,
            onSecondary = onSecondaryActionColor,
            secondaryContainer = secondaryContainerColor,
            onSecondaryContainer = onSecondaryContainerColor,
            
            tertiary = secondaryActionColor,
            onTertiary = onSecondaryActionColor,
            tertiaryContainer = secondaryContainerColor,
            onTertiaryContainer = onSecondaryContainerColor,

            background = background, 
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = primaryContainerColor, 
            onSurfaceVariant = onPrimaryContainerColor
        )
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        if (context.baseContext == null) return null // Защита от null
        if (context === context.baseContext) return null // Защита от цикла
        context = context.baseContext
    }
    return null
}

@Composable
fun ReminderAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeMode = getThemeMode(context)
    
    val primaryColor = getThemeColor(context, KEY_PRIMARY_COLOR, if (darkTheme) Purple80 else Purple40)
    val secondaryColor = getThemeColor(context, KEY_SECONDARY_COLOR, if (darkTheme) PurpleGrey80 else PurpleGrey40)
    val tertiaryColor = getThemeColor(context, KEY_TERTIARY_COLOR, if (darkTheme) Pink80 else Pink40)

    val colorScheme = when (themeMode) {
        ThemeMode.CUSTOM -> {
             generateCustomColorScheme(primaryColor, secondaryColor, tertiaryColor, darkTheme)
        }
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = context.findActivity()
                if (activity != null) {
                    val window = activity.window
                    window.statusBarColor = colorScheme.primaryContainer.toArgb()
                    // For Dark/Light/Custom modes, adjust status bar icon color
                    val isLightStatusBars = when(themeMode) {
                        ThemeMode.LIGHT -> true
                        ThemeMode.DARK -> false
                        ThemeMode.CUSTOM -> !darkTheme // Or calculate contrast based on primaryContainer
                    }
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightStatusBars
                }
            } catch (t: Throwable) {
                // Игнорируем любые ошибки при установке цветов статус-бара, чтобы не крашить приложение
                t.printStackTrace()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
