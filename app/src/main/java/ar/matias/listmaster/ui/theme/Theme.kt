package ar.matias.listmaster.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    primaryContainer = AccentSoftLight,
    onPrimaryContainer = AccentLight,
    secondary = NeutralLight,
    onSecondary = Color.White,
    secondaryContainer = NeutralSoftLight,
    onSecondaryContainer = NeutralLight,
    tertiary = SuccessLight,
    onTertiary = Color.White,
    tertiaryContainer = SuccessSoftLight,
    onTertiaryContainer = SuccessLight,
    background = BackgroundLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceAltLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = AccentLight,
    inverseSurface = TextLight,
    inverseOnSurface = SurfaceLight,
    outline = OutlineLight,
    outlineVariant = SurfaceSunkenLight,
    error = DangerLight,
    onError = Color.White,
    errorContainer = DangerSoftLight,
    onErrorContainer = DangerLight,
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = BackgroundDark,
    primaryContainer = AccentSoftDark,
    onPrimaryContainer = AccentDark,
    secondary = NeutralDark,
    onSecondary = BackgroundDark,
    secondaryContainer = NeutralSoftDark,
    onSecondaryContainer = NeutralDark,
    tertiary = SuccessDark,
    onTertiary = BackgroundDark,
    tertiaryContainer = SuccessSoftDark,
    onTertiaryContainer = SuccessDark,
    background = BackgroundDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = AccentDark,
    inverseSurface = TextDark,
    inverseOnSurface = BackgroundDark,
    outline = OutlineDark,
    outlineVariant = SurfaceSunkenDark,
    error = DangerDark,
    onError = BackgroundDark,
    errorContainer = DangerSoftDark,
    onErrorContainer = DangerDark,
    scrim = Color.Black,
)

@Composable
fun ListMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Sin color dinámico a propósito: la identidad es la misma en todos los
    // celulares y coincide con la familia visual de registro_sueno.
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Los iconos del sistema siguen al tema de la app, no al del celular.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ListMasterTypography,
        content = content,
    )
}
