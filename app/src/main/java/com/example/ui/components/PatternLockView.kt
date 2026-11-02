package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.LiquidGlassTheme
import com.example.ui.theme.RoseNeon
import kotlin.math.hypot

enum class PatternState {
    INPUT,
    SUCCESS,
    ERROR
}

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    patternState: PatternState = PatternState.INPUT,
    enabled: Boolean = true,
    onPatternComplete: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }

    val colors = LiquidGlassTheme.colors

    val targetLineColor = when (patternState) {
        PatternState.INPUT -> colors.primaryNeon
        PatternState.SUCCESS -> EmeraldNeon
        PatternState.ERROR -> RoseNeon
    }
    val animatedLineColor by animateColorAsState(
        targetValue = targetLineColor,
        animationSpec = tween(durationMillis = 250),
        label = "pattern_line_color"
    )

    fun triggerVibration() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.glassCard)
            .padding(24.dp)
            .pointerInput(enabled, patternState) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes.clear()
                        currentTouchPoint = offset
                        val node = findNodeAtOffset(offset, size.width.toFloat(), size.height.toFloat())
                        if (node != null) {
                            selectedNodes.add(node)
                            triggerVibration()
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        currentTouchPoint = pos
                        val node = findNodeAtOffset(pos, size.width.toFloat(), size.height.toFloat())
                        if (node != null && !selectedNodes.contains(node)) {
                            selectedNodes.add(node)
                            triggerVibration()
                        }
                    },
                    onDragEnd = {
                        val result = selectedNodes.toList()
                        currentTouchPoint = null
                        if (result.isNotEmpty()) {
                            onPatternComplete(result)
                        }
                    },
                    onDragCancel = {
                        selectedNodes.clear()
                        currentTouchPoint = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / 3f
            val cellH = size.height / 3f

            // Compute center points for all 9 nodes (0..8)
            val nodeCenters = (0..8).map { idx ->
                val col = idx % 3
                val row = idx / 3
                Offset(
                    x = col * cellW + cellW / 2f,
                    y = row * cellH + cellH / 2f
                )
            }

            // Draw connected lines
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val p1 = nodeCenters[selectedNodes[i]]
                    val p2 = nodeCenters[selectedNodes[i + 1]]

                    // Outer neon glow line
                    drawLine(
                        color = animatedLineColor.copy(alpha = 0.35f),
                        start = p1,
                        end = p2,
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                    // Core sharp neon line
                    drawLine(
                        color = animatedLineColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch point if dragging
            if (selectedNodes.isNotEmpty() && currentTouchPoint != null && patternState == PatternState.INPUT) {
                val lastCenter = nodeCenters[selectedNodes.last()]
                drawLine(
                    color = animatedLineColor.copy(alpha = 0.3f),
                    start = lastCenter,
                    end = currentTouchPoint!!,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = animatedLineColor.copy(alpha = 0.85f),
                    start = lastCenter,
                    end = currentTouchPoint!!,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // Draw each of the 9 nodes
            nodeCenters.forEachIndexed { index, center ->
                val isSelected = selectedNodes.contains(index)
                val nodeColor = if (isSelected) animatedLineColor else colors.glassBorder

                // Outer glass ring
                drawCircle(
                    color = if (isSelected) animatedLineColor.copy(alpha = 0.25f) else colors.glassBorderSubtle,
                    radius = if (isSelected) cellW * 0.32f else cellW * 0.22f,
                    center = center
                )

                // Glass ring outline
                drawCircle(
                    color = nodeColor,
                    radius = if (isSelected) cellW * 0.32f else cellW * 0.22f,
                    center = center,
                    style = Stroke(width = if (isSelected) 3.5f else 1.8f)
                )

                // Inner core dot
                drawCircle(
                    color = if (isSelected) nodeColor else colors.textMuted,
                    radius = if (isSelected) cellW * 0.12f else cellW * 0.07f,
                    center = center
                )
            }
        }
    }
}

private fun findNodeAtOffset(offset: Offset, totalWidth: Float, totalHeight: Float): Int? {
    val cellW = totalWidth / 3f
    val cellH = totalHeight / 3f
    val threshold = cellW * 0.35f

    for (i in 0..8) {
        val col = i % 3
        val row = i / 3
        val cx = col * cellW + cellW / 2f
        val cy = row * cellH + cellH / 2f
        val dist = hypot(offset.x - cx, offset.y - cy)
        if (dist <= threshold) {
            return i
        }
    }
    return null
}

fun List<Int>.toPatternHash(): String = joinToString("-")
