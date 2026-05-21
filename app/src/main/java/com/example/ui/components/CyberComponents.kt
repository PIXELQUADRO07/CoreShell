package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Scanlines background modifier for an authentic CRT cyberdeck look.
 */
@Composable
fun Modifier.cyberScanlines(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "scanlines")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlines_y"
    )

    return this.drawBehind {
        val width = size.width
        val height = size.height
        if (width <= 1f || height <= 1f) return@drawBehind

        val gridStepVal = 48.dp.toPx()
        val horizontalScanlineStepVal = 8.dp.toPx()

        clipRect {
            // Draw Tech grid lines underneath for depth (with safe bounds check)
            if (gridStepVal > 1f) {
                val cols = (width / gridStepVal).toInt().coerceIn(0, 100)
                for (i in 0..cols) {
                    val x = i * gridStepVal
                    drawLine(
                        color = Color(0x0A00FFD1),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }

                val rows = (height / gridStepVal).toInt().coerceIn(0, 150)
                for (j in 0..rows) {
                    val y = j * gridStepVal
                    drawLine(
                        color = Color(0x0A00FFD1),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }
            }

            // Draw horizontal CRT scanline dark shadow bars (spaced cleanly at safe intervals, no vertical pixel loops)
            if (horizontalScanlineStepVal > 1f) {
                val numScanlines = (height / horizontalScanlineStepVal).toInt().coerceIn(0, 300)
                for (k in 0..numScanlines) {
                    val y = k * horizontalScanlineStepVal
                    drawLine(
                        color = Color(0x28000000), // Dark horizontal scanline bar
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // Dynamic Sweeping glowing CRT beam scanline
            val sweepY = yOffset * height
            drawLine(
                color = Color(0x1300FFD1),
                start = Offset(0f, sweepY),
                end = Offset(width, sweepY),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = Color(0x1BFF00FF),
                start = Offset(0f, (sweepY - 8.dp.toPx()).coerceAtLeast(0f)),
                end = Offset(width, (sweepY - 8.dp.toPx()).coerceAtLeast(0f)),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

/**
 * Beautiful glowing border styled with cyberpunk notches/angles.
 */
@Composable
fun CyberBorderCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCirc),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberDark.copy(alpha = 0.85f))
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = alpha),
                        accentColor.copy(alpha = alpha * 0.3f),
                        accentColor.copy(alpha = alpha)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp)
    ) {
        content()
    }
}

/**
 * Custom retro button in cyberpunk styled neon.
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CyberCyan,
    enabled: Boolean = true,
    testTag: String = "cyber_button"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) CyberGray else CyberGray.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = if (enabled) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[ ${text.uppercase()} ]",
                color = if (enabled) color else color.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * Cyberpunk Telemetry Oscilloscope Graph (Rendered on Canvas)
 */
@Composable
fun OscilloscopeGraph(
    metricsHistory: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    title: String = "CORE TELEMETRY",
    valueText: String = ""
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ $title",
                color = TermMuted,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
            Text(
                text = valueText,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF030308))
                .border(0.5.dp, Color(0xFF1F1F3D))
        ) {
            val width = size.width
            val height = size.height

            // Draw grid inside scope
            val gridStep = width / 10
            for (i in 1..9) {
                val dx = i * gridStep
                drawLine(
                    color = Color(0xFF0C0C1C),
                    start = Offset(dx, 0f),
                    end = Offset(dx, height),
                    strokeWidth = 1f
                )
            }
            // Draw horizontal center line
            drawLine(
                color = Color(0xFF13132D),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1.5f
            )

            if (metricsHistory.isNotEmpty()) {
                val path = Path()
                val sliceWidth = width / (metricsHistory.size - 1).coerceAtLeast(1)

                metricsHistory.forEachIndexed { idx, valPercent ->
                    // percentage mapped to height, inverting because coord system starts top-left
                    val y = height - (valPercent * height).coerceIn(2f, height - 2f)
                    val x = idx * sliceWidth
                    if (idx == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Draw solid wave line
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Fill area below gradient
                path.lineTo(width, height)
                path.lineTo(0f, height)
                path.close()
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
            }
        }
    }
}
