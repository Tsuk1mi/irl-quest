package com.irlquest.app.ui.theme

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

// Светлая тема - стиль светлой таверны
private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    tertiary = TavernWood,
    onTertiary = OnSecondary
)

// Темная тема - стиль темной таверны при свечах
private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Background,
    primaryContainer = TavernWood,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnBackground,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    background = DarkBackground, // Темное дерево
    onBackground = Background,
    surface = DarkSurface, // Темный пергамент
    onSurface = Background,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    tertiary = CandleLight,
    onTertiary = OnBackground,
    outline = TavernWoodLight,
    surfaceVariant = DarkTavernWood
)

@Composable
fun IRLQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Отключаем dynamic color для сохранения фэнтези-темы
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Предпочитаем нашу фэнтези-тему вместо dynamic colors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Используем цвет таверны для статус-бара
            window.statusBarColor = if (darkTheme) DarkTavernWood.toArgb() else TavernWood.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
