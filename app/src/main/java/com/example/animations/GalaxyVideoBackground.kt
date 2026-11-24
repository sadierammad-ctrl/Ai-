package com.example.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.LocalThemePreset
import com.example.ui.theme.NebulaNavy
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class GalaxyParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val speedY: Float,
    val driftX: Float,
    val phase: Float,
    val isPurple: Boolean,
    val baseAlpha: Float
)

@Composable
fun GalaxyVideoBackground(
    orbState: OrbAnimationState,
    animationsEnabled: Boolean,
    batterySavingMode: Boolean,
    modifier: Modifier = Modifier
) {
    val preset = LocalThemePreset.current
    val primaryGlow = preset.primaryColor
    val secondaryGlow = preset.secondaryColor
    val tertiaryGlow = preset.tertiaryColor

    val particles = remember {
        val rng = Random(2026)
        List(84) { idx ->
            GalaxyParticle(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radiusDp = 1.0f + rng.nextFloat() * 2.6f,
                speedY = 0.012f + rng.nextFloat() * 0.032f,
                driftX = (rng.nextFloat() - 0.5f) * 0.025f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                isPurple = idx % 2 == 0,
                baseAlpha = 0.25f + rng.nextFloat() * 0.65f
            )
        }
    }

    var timeSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(animationsEnabled, batterySavingMode) {
        if (!animationsEnabled) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            if (batterySavingMode) {
                delay(33L) // ~30 FPS cap for battery saving
                timeSeconds += 0.033f
            } else {
                withFrameNanos { frameNanos ->
                    if (lastNanos != 0L) {
                        val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        timeSeconds += dt
                    }
                    lastNanos = frameNanos
                }
            }
        }
    }

    val stateEnergyMultiplier = when (orbState) {
        OrbAnimationState.IDLE -> 1.0f
        OrbAnimationState.LISTENING -> 1.65f
        OrbAnimationState.THINKING -> 2.0f
        OrbAnimationState.SPEAKING -> 1.85f
        OrbAnimationState.PROCESSING -> 2.15f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = if (animationsEnabled) timeSeconds else 0f

        // 1. Deep cosmic navy-black base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(CosmicBlack, DeepNavy, NebulaNavy, CosmicBlack)
            )
        )

        // 2. Ambient nebula radial blooms (blue + purple neon lighting)
        val bloomOffset1 = Offset(
            x = w * (0.28f + 0.08f * sin(t * 0.28f)),
            y = h * (0.24f + 0.06f * cos(t * 0.22f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = 0.22f * stateEnergyMultiplier.coerceAtMost(1.4f)),
                    Color.Transparent
                ),
                center = bloomOffset1,
                radius = w * 0.72f
            ),
            center = bloomOffset1,
            radius = w * 0.72f
        )

        val bloomOffset2 = Offset(
            x = w * (0.76f + 0.08f * cos(t * 0.25f)),
            y = h * (0.68f + 0.07f * sin(t * 0.30f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryGlow.copy(alpha = 0.24f * stateEnergyMultiplier.coerceAtMost(1.45f)),
                    Color.Transparent
                ),
                center = bloomOffset2,
                radius = w * 0.78f
            ),
            center = bloomOffset2,
            radius = w * 0.78f
        )

        // 3. Cosmic animated energy waves
        val waveCount = if (batterySavingMode) 2 else 4
        val stepPx = if (batterySavingMode) 36f else 18f
        for (waveIdx in 0 until waveCount) {
            val path = Path()
            val yCenter = h * (0.32f + waveIdx * 0.16f)
            val amplitude = (26f + waveIdx * 10f) * (0.85f + 0.25f * stateEnergyMultiplier)
            val freq = (1.6f + waveIdx * 0.45f) * PI.toFloat() / w
            val phase = t * (0.7f + waveIdx * 0.25f) * stateEnergyMultiplier * 0.65f

            var x = 0f
            while (x <= w + stepPx) {
                val y = yCenter +
                    sin(x * freq + phase) * amplitude +
                    cos(x * freq * 0.5f - phase * 0.7f) * (amplitude * 0.4f)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += stepPx
            }

            val waveColor = when (waveIdx % 3) {
                0 -> primaryGlow
                1 -> secondaryGlow
                else -> tertiaryGlow
            }
            drawPath(
                path = path,
                color = waveColor.copy(alpha = 0.14f - waveIdx * 0.02f),
                style = Stroke(width = (3.5f - waveIdx * 0.5f).dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 4. Holographic perspective rings near horizon
        if (!batterySavingMode) {
            val horizonCenter = Offset(w * 0.5f, h * 0.88f)
            for (ring in 1..3) {
                val ringRadius = w * (0.35f + ring * 0.22f) + sin(t * 0.8f + ring) * 12f
                drawCircle(
                    color = primaryGlow.copy(alpha = 0.06f),
                    radius = ringRadius,
                    center = horizonCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // 5. Floating luminous galaxy particles
        val activeParticleCount = if (batterySavingMode) 32 else particles.size
        for (i in 0 until activeParticleCount) {
            val p = particles[i]
            val yProgress = (p.yFraction - t * p.speedY * stateEnergyMultiplier * 0.55f)
            val wrappedY = ((yProgress % 1f) + 1f) % 1f
            val xDrift = (p.xFraction + sin(t * 0.4f + p.phase) * p.driftX)
            val wrappedX = ((xDrift % 1f) + 1f) % 1f

            val center = Offset(wrappedX * w, wrappedY * h)
            val twinkle = 0.55f + 0.45f * sin(t * 2.2f + p.phase)
            val color = if (p.isPurple) secondaryGlow else primaryGlow
            val r = p.radiusDp.dp.toPx()

            if (!batterySavingMode) {
                drawCircle(
                    color = color.copy(alpha = p.baseAlpha * twinkle * 0.28f),
                    radius = r * 3.0f,
                    center = center
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = p.baseAlpha * twinkle * 0.9f),
                radius = r,
                center = center
            )
        }
    }
}
