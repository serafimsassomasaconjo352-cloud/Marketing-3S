package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class RumoFinancialColors(
    val profit: Color,
    val loss: Color,
    val warning: Color,
    val heroDeep: Color,
    val heroGold: Color
)

val LocalRumoColors = staticCompositionLocalOf {
    RumoFinancialColors(
        profit = ProfitGreenLight,
        loss = LossCrimsonLight,
        warning = WarningAmberLight,
        heroDeep = HeroEmeraldDeep,
        heroGold = HeroGoldAccent
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldOnContainerDark,
    tertiary = SlateTertiaryDark,
    onTertiary = SlateOnTertiaryDark,
    tertiaryContainer = SlateContainerDark,
    onTertiaryContainer = SlateOnContainerDark,
    background = ObsidianBackgroundDark,
    onBackground = OnSurfaceDark,
    surface = CharcoalSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = LossCrimsonDark,
    onError = Color(0xFF410002)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = GoldOnSecondaryLight,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = GoldOnContainerLight,
    tertiary = SlateTertiaryLight,
    onTertiary = SlateOnTertiaryLight,
    tertiaryContainer = SlateContainerLight,
    onTertiaryContainer = SlateOnContainerLight,
    background = IvoryBackgroundLight,
    onBackground = OnSurfaceLight,
    surface = CrispSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = LossCrimsonLight,
    onError = Color.White
)

private val RumoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RumoCfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val financialColors = if (darkTheme) {
        RumoFinancialColors(
            profit = ProfitGreenDark,
            loss = LossCrimsonDark,
            warning = WarningAmberDark,
            heroDeep = HeroEmeraldDeep,
            heroGold = HeroGoldAccent
        )
    } else {
        RumoFinancialColors(
            profit = ProfitGreenLight,
            loss = LossCrimsonLight,
            warning = WarningAmberLight,
            heroDeep = HeroEmeraldDeep,
            heroGold = HeroGoldAccent
        )
    }

    CompositionLocalProvider(LocalRumoColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RumoTypography,
            shapes = RumoShapes,
            content = content
        )
    }
}
