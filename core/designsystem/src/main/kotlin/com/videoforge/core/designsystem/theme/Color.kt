package com.videoforge.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Projector Dark: فحم دافئ متعدد الطبقات ───────────────────────────

val ProjectorBackground = Color(0xFF14110C)
val ProjectorSurfaceLow = Color(0xFF1A1712)
val ProjectorSurface = Color(0xFF201C15)
val ProjectorSurfaceHigh = Color(0xFF2A241B)
val ProjectorSurfaceHighest = Color(0xFF342C21)

val ProjectorOnSurface = Color(0xFFEDE6D8)
val ProjectorOnSurfaceVariant = Color(0xFFBCB2A0)
val ProjectorOutline = Color(0xFF877D6B)
val ProjectorOutlineVariant = Color(0xFF494133)

val AmberPrimary = Color(0xFFE8A33D)
val AmberOnPrimary = Color(0xFF3B2703)
val AmberContainer = Color(0xFF54390A)
val AmberOnContainer = Color(0xFFFFD9A3)

val PhosphorSecondary = Color(0xFF63C9B8)
val PhosphorOnSecondary = Color(0xFF06382F)
val PhosphorContainer = Color(0xFF114A40)
val PhosphorOnContainer = Color(0xFFA9F0E1)

val RoseTertiary = Color(0xFFDE8E80)
val RoseOnTertiary = Color(0xFF40160F)
val RoseContainer = Color(0xFF5A2A20)
val RoseOnContainer = Color(0xFFFFDAD2)

val ProjectorError = Color(0xFFEB7B6F)
val ProjectorOnError = Color(0xFF47100B)
val ProjectorErrorContainer = Color(0xFF662018)
val ProjectorOnErrorContainer = Color(0xFFFFDAD5)

// ─── Projector Light: ورق محايد دافئ ─────────────────────────────────

val PaperBackground = Color(0xFFF6F2EA)
val PaperSurfaceLow = Color(0xFFFBF8F1)
val PaperSurface = Color(0xFFEFEAE0)
val PaperSurfaceHigh = Color(0xFFE9E3D7)
val PaperSurfaceHighest = Color(0xFFE3DCCE)

val PaperOnSurface = Color(0xFF211B12)
val PaperOnSurfaceVariant = Color(0xFF57503F)
val PaperOutline = Color(0xFF7A7160)
val PaperOutlineVariant = Color(0xFFC9C0AE)

val AmberPrimaryLight = Color(0xFF7A4E00)
val AmberOnPrimaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFFDDB0)
val AmberOnContainerLight = Color(0xFF4A2E00)

val PhosphorSecondaryLight = Color(0xFF00695F)
val PhosphorOnSecondaryLight = Color(0xFFFFFFFF)
val PhosphorContainerLight = Color(0xFF9FF2E4)
val PhosphorOnContainerLight = Color(0xFF004F47)

val RoseTertiaryLight = Color(0xFF9C4143)
val RoseOnTertiaryLight = Color(0xFFFFFFFF)
val RoseContainerLight = Color(0xFFFFDAD8)
val RoseOnContainerLight = Color(0xFF5F1517)

val PaperError = Color(0xFFBA1A1A)
val PaperOnError = Color(0xFFFFFFFF)
val PaperErrorContainer = Color(0xFFFFDAD6)
val PaperOnErrorContainer = Color(0xFF410002)

// ─── المخططات ────────────────────────────────────────────────────────

internal fun projectorDarkScheme() = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary = PhosphorSecondary,
    onSecondary = PhosphorOnSecondary,
    secondaryContainer = PhosphorContainer,
    onSecondaryContainer = PhosphorOnContainer,
    tertiary = RoseTertiary,
    onTertiary = RoseOnTertiary,
    tertiaryContainer = RoseContainer,
    onTertiaryContainer = RoseOnContainer,
    error = ProjectorError,
    onError = ProjectorOnError,
    errorContainer = ProjectorErrorContainer,
    onErrorContainer = ProjectorOnErrorContainer,
    background = ProjectorBackground,
    onBackground = ProjectorOnSurface,
    surface = ProjectorBackground,
    onSurface = ProjectorOnSurface,
    surfaceVariant = ProjectorSurface,
    onSurfaceVariant = ProjectorOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0F0D09),
    surfaceContainerLow = ProjectorSurfaceLow,
    surfaceContainer = ProjectorSurface,
    surfaceContainerHigh = ProjectorSurfaceHigh,
    surfaceContainerHighest = ProjectorSurfaceHighest,
    outline = ProjectorOutline,
    outlineVariant = ProjectorOutlineVariant,
    scrim = Color(0xFF000000)
)

internal fun projectorLightScheme() = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberOnContainerLight,
    secondary = PhosphorSecondaryLight,
    onSecondary = PhosphorOnSecondaryLight,
    secondaryContainer = PhosphorContainerLight,
    onSecondaryContainer = PhosphorOnContainerLight,
    tertiary = RoseTertiaryLight,
    onTertiary = RoseOnTertiaryLight,
    tertiaryContainer = RoseContainerLight,
    onTertiaryContainer = RoseOnContainerLight,
    error = PaperError,
    onError = PaperOnError,
    errorContainer = PaperErrorContainer,
    onErrorContainer = PaperOnErrorContainer,
    background = PaperBackground,
    onBackground = PaperOnSurface,
    surface = PaperSurfaceLow,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = PaperOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = PaperSurfaceLow,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceHigh,
    surfaceContainerHighest = PaperSurfaceHighest,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    scrim = Color(0xFF000000)
)