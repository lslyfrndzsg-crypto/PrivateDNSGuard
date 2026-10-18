package com.privatednsguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Semantic Accent Constants
val DarkGuardGreen = Color(0xFF00E676)
val LightGuardGreen = Color(0xFF008744)
val GuardGreen: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardGreen else LightGuardGreen

val DarkGuardGreenDark = Color(0xFF00B248)
val LightGuardGreenDark = Color(0xFF005A2C)
val GuardGreenDark: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardGreenDark else LightGuardGreenDark

val DarkGuardGreenGlow = Color(0x3300E676)
val LightGuardGreenGlow = Color(0x22008744)
val GuardGreenGlow: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardGreenGlow else LightGuardGreenGlow

val DarkGuardBlue = Color(0xFF00B0FF)
val LightGuardBlue = Color(0xFF0288D1)
val GuardBlue: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardBlue else LightGuardBlue

val DarkGuardBlueGlow = Color(0x3300B0FF)
val LightGuardBlueGlow = Color(0x220288D1)
val GuardBlueGlow: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardBlueGlow else LightGuardBlueGlow

val DarkGuardAmber = Color(0xFFFFB300)
val LightGuardAmber = Color(0xFFD97706)
val GuardAmber: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardAmber else LightGuardAmber

val DarkGuardAmberGlow = Color(0x33FFB300)
val LightGuardAmberGlow = Color(0x22D97706)
val GuardAmberGlow: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardAmberGlow else LightGuardAmberGlow

val DarkGuardRed = Color(0xFFFF5252)
val LightGuardRed = Color(0xFFD32F2F)
val GuardRed: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardRed else LightGuardRed

val DarkGuardRedGlow = Color(0x33FF5252)
val LightGuardRedGlow = Color(0x22D32F2F)
val GuardRedGlow: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkGuardRedGlow else LightGuardRedGlow

// Surfaces & Backgrounds (Adapting between sleek OLED Dark & modern crisp Light)
val DarkBackground: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF0D1117) else Color(0xFFF6F8FA)

val DarkSurface: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF161B22) else Color(0xFFFFFFFF)

val DarkSurfaceElevated: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF21262D) else Color(0xFFF0F2F5)

val DarkBorder: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF30363D) else Color(0xFFD0D7DE)

// Typography
val TextPrimary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFF0F6FC) else Color(0xFF1F2328)

val TextSecondary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8B949E) else Color(0xFF656D76)

val TextMuted: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF6E7681) else Color(0xFF8C959F)
