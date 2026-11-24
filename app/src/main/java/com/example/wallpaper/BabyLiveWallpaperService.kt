package com.example.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Optional battery-efficient Android Live Wallpaper rendering BABY AI's
 * animated dark cosmic galaxy, moving cyan/purple particles, and glowing AI orb.
 * Requires zero microphone permissions.
 */
class BabyLiveWallpaperService : WallpaperService() {

    private data class Star(val x: Float, val y: Float, val r: Float, val speed: Float, val purple: Boolean, val phase: Float)

    override fun onCreateEngine(): Engine = GalaxyOrbEngine()

    private inner class GalaxyOrbEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var timeSec = 0f

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        private val stars = List(60) { i ->
            val rng = Random(1000 + i)
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = 1.5f + rng.nextFloat() * 3.5f,
                speed = 0.015f + rng.nextFloat() * 0.028f,
                purple = i % 2 == 0,
                phase = rng.nextFloat() * 6.28f
            )
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    timeSec += 0.033f
                    handler.postDelayed(this, 33L) // Battery-friendly ~30fps
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderGalaxy(canvas)
                }
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }

        private fun renderGalaxy(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            if (w <= 0f || h <= 0f) return

            bgPaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(
                    Color.parseColor("#030510"),
                    Color.parseColor("#070C22"),
                    Color.parseColor("#0F183D"),
                    Color.parseColor("#030510")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Cosmic waves
            for (wave in 0..2) {
                val path = Path()
                val yMid = h * (0.35f + wave * 0.16f)
                val amp = 32f + wave * 12f
                var x = 0f
                while (x <= w + 28f) {
                    val y = yMid + sin(x * (2f * PI.toFloat() / w) + timeSec * (0.9f + wave * 0.3f)) * amp
                    if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 28f
                }
                wavePaint.color = if (wave % 2 == 0) Color.argb(45, 0, 212, 255) else Color.argb(45, 157, 78, 221)
                wavePaint.strokeWidth = 4f
                canvas.drawPath(path, wavePaint)
            }

            // Floating particles
            for (star in stars) {
                val py = ((star.y - timeSec * star.speed) % 1f + 1f) % 1f
                val px = ((star.x + sin(timeSec * 0.4f + star.phase) * 0.02f) % 1f + 1f) % 1f
                val alpha = (140 + (90 * sin(timeSec * 2.2f + star.phase))).toInt().coerceIn(40, 255)
                particlePaint.color = if (star.purple) Color.argb(alpha, 157, 78, 221) else Color.argb(alpha, 0, 212, 255)
                canvas.drawCircle(px * w, py * h, star.r * 2.2f, particlePaint)
                particlePaint.color = Color.argb(alpha, 255, 255, 255)
                canvas.drawCircle(px * w, py * h, star.r, particlePaint)
            }

            // Center animated BABY AI orb
            val cx = w * 0.5f
            val cy = h * 0.44f
            val orbRadius = (minOf(w, h) * 0.16f) * (1f + 0.04f * sin(timeSec * 2.2f))

            orbPaint.shader = RadialGradient(
                cx, cy, orbRadius * 2.1f,
                intArrayOf(
                    Color.argb(120, 0, 212, 255),
                    Color.argb(60, 157, 78, 221),
                    Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, orbRadius * 2.1f, orbPaint)

            orbPaint.shader = RadialGradient(
                cx - orbRadius * 0.25f, cy - orbRadius * 0.25f, orbRadius * 1.2f,
                intArrayOf(
                    Color.WHITE,
                    Color.parseColor("#00D4FF"),
                    Color.parseColor("#9D4EDD"),
                    Color.parseColor("#0A1030")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, orbRadius, orbPaint)
            orbPaint.shader = null

            ringPaint.color = Color.argb(110, 0, 212, 255)
            ringPaint.strokeWidth = 3f
            val rRing = orbRadius * 1.32f
            val angle = (timeSec * 36f) % 360f
            canvas.drawArc(cx - rRing, cy - rRing, cx + rRing, cy + rRing, angle, 120f, false, ringPaint)
            ringPaint.color = Color.argb(110, 157, 78, 221)
            canvas.drawArc(cx - rRing, cy - rRing, cx + rRing, cy + rRing, angle + 180f, 120f, false, ringPaint)
        }
    }
}
