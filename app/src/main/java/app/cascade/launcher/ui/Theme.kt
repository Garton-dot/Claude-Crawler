package app.cascade.launcher.ui

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

private val DefaultAccent = Color(0xFF7FD4FF)

private val LocalAccent = compositionLocalOf { DefaultAccent }

/** The launcher's highlight color: follows the wallpaper when dynamic theming is on. */
val Accent: Color
    @Composable get() = LocalAccent.current

@Composable
fun CascadeTheme(dynamicColor: Boolean, content: @Composable () -> Unit) {
    val accent = if (dynamicColor) rememberWallpaperAccent() else DefaultAccent
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = accent,
                onPrimary = Color(0xFF101418),
                background = Color.Transparent,
                surface = Color(0xFF15191F),
                onSurface = Color.White,
            ),
            content = content,
        )
    }
}

/** Reads the wallpaper's main color (Android 8.1+) and keeps it updated when the wallpaper changes. */
@Composable
private fun rememberWallpaperAccent(): Color {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return DefaultAccent
    val context = LocalContext.current
    var accent by remember { mutableStateOf(readAccent(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(WallpaperManager::class.java)
        val listener = WallpaperManager.OnColorsChangedListener { colors, _ ->
            accent = accentFrom(colors)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        accent = readAccent(context)
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return accent
}

private fun readAccent(context: Context): Color = try {
    accentFrom(context.getSystemService(WallpaperManager::class.java).getWallpaperColors(WallpaperManager.FLAG_SYSTEM))
} catch (e: Exception) {
    DefaultAccent
}

/**
 * Picks the most vivid of the wallpaper's three key colors, ignoring near-black and washed-out
 * ones (a dark wallpaper's "dominant" color is usually just its background). The result gets its
 * lightness lifted and saturation capped so it stays readable on the dark overlay. Wallpapers
 * with no vivid color keep the default.
 */
private fun accentFrom(colors: WallpaperColors?): Color {
    if (colors == null) return DefaultAccent
    val hsv = FloatArray(3)
    val best = listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
        .map { it.toArgb() }
        .filter {
            android.graphics.Color.colorToHSV(it, hsv)
            hsv[2] >= 0.3f && hsv[1] >= 0.15f
        }
        .maxByOrNull {
            android.graphics.Color.colorToHSV(it, hsv)
            hsv[1] * hsv[2]
        } ?: return DefaultAccent

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(best, hsl)
    hsl[1] = hsl[1].coerceIn(0.45f, 0.9f)
    hsl[2] = hsl[2].coerceIn(0.68f, 0.82f)
    return Color(ColorUtils.HSLToColor(hsl))
}
