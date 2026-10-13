package app.cascade.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF7FD4FF)

@Composable
fun CascadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color(0xFF00334A),
            background = Color.Transparent,
            surface = Color(0xFF15191F),
            onSurface = Color.White,
        ),
        content = content,
    )
}
