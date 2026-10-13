package app.cascade.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp

/**
 * The letters along the right edge. Dragging a finger over them makes the letters near the
 * finger bulge out in a wave, and jumps the list to the letter under the finger.
 */
@Composable
fun AlphabetScroller(
    letters: List<Char>,
    onActiveChange: (Boolean) -> Unit,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return
    val haptics = LocalHapticFeedback.current
    val waveWidthPx = with(LocalDensity.current) { 56.dp.toPx() }
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnActive by rememberUpdatedState(onActiveChange)

    var height by remember { mutableIntStateOf(0) }
    var touchY by remember { mutableStateOf<Float?>(null) }
    var activeIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier
            .width(36.dp)
            .fillMaxHeight()
            .onSizeChanged { height = it.height }
            .pointerInput(letters) {
                fun track(y: Float) {
                    touchY = y
                    if (height <= 0) return
                    val index = (y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    if (index != activeIndex) {
                        activeIndex = index
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnLetter(letters[index])
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    currentOnActive(true)
                    track(down.position.y)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        track(change.position.y)
                    }
                    touchY = null
                    activeIndex = -1
                    currentOnActive(false)
                }
            },
    ) {
        Column(Modifier.fillMaxHeight()) {
            val slot = height.toFloat() / letters.size
            val spread = slot * 2.2f
            letters.forEachIndexed { index, letter ->
                val center = (index + 0.5f) * slot
                val target = touchY?.let { y ->
                    val d = (y - center) / spread
                    exp(-d * d)
                } ?: 0f
                val wave by animateFloatAsState(target, spring(dampingRatio = 0.7f, stiffness = 900f), label = "wave")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        color = if (index == activeIndex) Accent else Color.White.copy(alpha = 0.55f + 0.45f * wave),
                        fontSize = 11.sp,
                        fontWeight = if (wave > 0.5f) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.graphicsLayer {
                            translationX = -wave * waveWidthPx
                            val scale = 1f + wave * 1.1f
                            scaleX = scale
                            scaleY = scale
                        },
                    )
                }
            }
        }
    }
}
