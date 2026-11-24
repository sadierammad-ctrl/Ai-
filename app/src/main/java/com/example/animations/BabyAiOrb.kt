package com.example.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalThemePreset
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BabyAiOrb(
    state: OrbAnimationState,
    audioLevel: Float = 0f,
    animationsEnabled: Boolean = true,
    batterySavingMode: Boolean = false,
    size: Dp = 210.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val preset = LocalThemePreset.current
    val cyan = preset.primaryColor
    val purple = preset.secondaryColor
    val blue = preset.tertiaryColor

    val smoothedAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f),
        label = "orb_audio_level"
    )

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animationsEnabled, batterySavingMode) {
        if (!animationsEnabled) return@LaunchedEffect
        var last = 0L
        while (true) {
            if (batterySavingMode) {
                delay(33L)
                time += 0.033f
            } else {
                withFrameNanos { now ->
                    if (last != 0L) {
                        time += ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    }
                    last = now
                }
            }
        }
    }

    val stateDesc = when (state) {
        OrbAnimationState.IDLE -> "BABY AI Orb Idle"
        OrbAnimationState.LISTENING -> "BABY AI Orb Listening"
        OrbAnimationState.THINKING -> "BABY AI Orb Thinking"
        OrbAnimationState.SPEAKING -> "BABY AI Orb Speaking"
        OrbAnimationState.PROCESSING -> "BABY AI Orb Processing"
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .semantics { contentDescription = stateDesc }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, color = cyan),
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val minDim = this.size.minDimension
            val t = if (animationsEnabled) time else 0f

            val pulseFactor = when (state) {
                OrbAnimationState.IDLE -> 1f + 0.035f * sin(t * 2.0f)
                OrbAnimationState.LISTENING -> 1.04f + smoothedAudio * 0.14f + 0.04f * sin(t * 4.5f)
                OrbAnimationState.THINKING -> 1.02f + 0.05f * sin(t * 5.0f)
                OrbAnimationState.SPEAKING -> 1.05f + (0.08f + smoothedAudio * 0.14f) * kotlin.math.abs(sin(t * 6.5f))
                OrbAnimationState.PROCESSING -> 1.03f + 0.045f * sin(t * 7.0f)
            }

            val baseRadius = minDim * 0.28f * pulseFactor

            // Outer ambient glow
            val haloColor = when (state) {
                OrbAnimationState.LISTENING -> cyan
                OrbAnimationState.THINKING -> purple
                OrbAnimationState.SPEAKING -> cyan
                OrbAnimationState.PROCESSING -> blue
                OrbAnimationState.IDLE -> purple
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloColor.copy(alpha = 0.42f),
                        purple.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = minDim * 0.49f
                ),
                radius = minDim * 0.49f,
                center = c
            )

            // State-specific outer kinetic effects
            when (state) {
                OrbAnimationState.LISTENING -> {
                    // Expanding blue sonic energy waves around the orb
                    val waveRings = if (batterySavingMode) 2 else 3
                    for (i in 0 until waveRings) {
                        val phase = ((t * 0.85f + i / waveRings.toFloat()) % 1f)
                        val r = baseRadius * (1.15f + phase * 0.65f)
                        drawCircle(
                            color = cyan.copy(alpha = (1f - phase) * (0.45f + smoothedAudio * 0.35f)),
                            radius = r,
                            center = c,
                            style = Stroke(width = (2.8f - phase * 1.5f).dp.toPx())
                        )
                    }
                }

                OrbAnimationState.THINKING -> {
                    // Purple rotating energy vortex arcs
                    val arcRadius = baseRadius * 1.38f
                    for (i in 0 until 3) {
                        val startAngle = (t * 140f * (if (i % 2 == 0) 1f else -0.8f) + i * 120f) % 360f
                        drawArc(
                            color = if (i % 2 == 0) purple.copy(alpha = 0.85f) else cyan.copy(alpha = 0.6f),
                            startAngle = startAngle,
                            sweepAngle = 78f,
                            useCenter = false,
                            topLeft = Offset(c.x - arcRadius, c.y - arcRadius),
                            size = Size(arcRadius * 2f, arcRadius * 2f),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                OrbAnimationState.SPEAKING -> {
                    // Synchronized voice-activity radial bars around the orb
                    val barCount = if (batterySavingMode) 24 else 36
                    for (i in 0 until barCount) {
                        val angleRad = (i * (360f / barCount)) * (PI.toFloat() / 180f)
                        val wave = 0.5f + 0.5f * sin(t * 8f + i * 0.55f)
                        val barLen = minDim * (0.03f + wave * (0.07f + smoothedAudio * 0.08f))
                        val rInner = baseRadius * 1.12f
                        val rOuter = rInner + barLen
                        drawLine(
                            color = if (i % 2 == 0) cyan.copy(alpha = 0.85f) else purple.copy(alpha = 0.75f),
                            start = Offset(c.x + cos(angleRad) * rInner, c.y + sin(angleRad) * rInner),
                            end = Offset(c.x + cos(angleRad) * rOuter, c.y + sin(angleRad) * rOuter),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                OrbAnimationState.PROCESSING -> {
                    // Holographic radar scan & dual counter-rotating orbital rings
                    rotate(degrees = t * 180f, pivot = c) {
                        val rScan = baseRadius * 1.42f
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color.Transparent, cyan.copy(alpha = 0.85f), purple.copy(alpha = 0.7f)),
                                center = c
                            ),
                            startAngle = 0f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(c.x - rScan, c.y - rScan),
                            size = Size(rScan * 2f, rScan * 2f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                OrbAnimationState.IDLE -> {
                    // Gentle holographic orbital ring
                    rotate(degrees = t * 24f, pivot = c) {
                        val rRing = baseRadius * 1.32f
                        drawArc(
                            color = cyan.copy(alpha = 0.38f),
                            startAngle = 20f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(c.x - rRing, c.y - rRing),
                            size = Size(rRing * 2f, rRing * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = purple.copy(alpha = 0.35f),
                            startAngle = 200f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(c.x - rRing, c.y - rRing),
                            size = Size(rRing * 2f, rRing * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // 3D Sphere core with layered plasma gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        cyan.copy(alpha = 0.90f),
                        purple.copy(alpha = 0.85f),
                        Color(0xFF0A1030)
                    ),
                    center = Offset(c.x - baseRadius * 0.28f, c.y - baseRadius * 0.28f),
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius,
                center = c
            )

            // Specular highlight & internal energy ring
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = baseRadius * 0.96f,
                center = c,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c.x - baseRadius * 0.32f, c.y - baseRadius * 0.34f),
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = Offset(c.x - baseRadius * 0.32f, c.y - baseRadius * 0.34f)
            )
        }
    }
}
