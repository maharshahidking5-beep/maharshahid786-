package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransitionType
import com.example.model.VideoEffectPreset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EffectPreviewOverlay(
    effectPreset: VideoEffectPreset,
    effectIntensity: Float,
    vignette: Float,
    playheadMs: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val strength = effectIntensity.coerceIn(0.15f, 1f)
    val tSec = playheadMs / 1000f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (effectPreset) {
            VideoEffectPreset.ANAMORPHIC_BARS -> {
                val barH = h * 0.12f * strength.coerceIn(0.4f, 1f)
                drawRect(Color.Black, Offset.Zero, Size(w, barH))
                drawRect(Color.Black, Offset(0f, h - barH), Size(w, barH))
            }

            VideoEffectPreset.CLUB_LASER -> {
                val sweep = (sin(tSec * 3.4f) * 0.5f + 0.5f)
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.48f * strength),
                    start = Offset(w * 0.1f, 0f),
                    end = Offset(w * (0.2f + sweep * 0.7f), h),
                    strokeWidth = 22.dp.toPx() * strength
                )
                drawLine(
                    color = Color(0xFFFF2E88).copy(alpha = 0.45f * strength),
                    start = Offset(w * 0.9f, 0f),
                    end = Offset(w * (0.8f - sweep * 0.7f), h),
                    strokeWidth = 20.dp.toPx() * strength
                )
            }

            VideoEffectPreset.STROBE_FLASH -> {
                val beat = abs(sin(tSec * 9.0f))
                if (beat > 0.78f) {
                    drawRect(
                        color = Color.White.copy(alpha = (beat - 0.78f) * 2.4f * strength),
                        size = size
                    )
                }
            }

            VideoEffectPreset.BLACK_FLASH -> {
                val beat = abs(sin(tSec * 7.5f))
                if (beat > 0.75f) {
                    drawRect(
                        color = Color.Black.copy(alpha = ((beat - 0.75f) * 3.2f * strength).coerceAtMost(0.85f)),
                        size = size
                    )
                }
            }

            VideoEffectPreset.VHS_RETRO -> {
                var y = 0f
                val step = 7.dp.toPx()
                while (y < h) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f * strength),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += step
                }
                val trackingY = ((tSec * 0.35f) % 1f) * h
                drawRect(
                    color = Color.White.copy(alpha = 0.14f * strength),
                    topLeft = Offset(0f, trackingY),
                    size = Size(w, 8.dp.toPx())
                )
            }

            VideoEffectPreset.DV_CAM_98 -> {
                val blinkOn = ((playheadMs / 450L) % 2L) == 0L
                if (blinkOn) {
                    drawCircle(
                        color = Color(0xFFFF2E88),
                        radius = 5.dp.toPx(),
                        center = Offset(18.dp.toPx(), 20.dp.toPx())
                    )
                }
                val hud = textMeasurer.measure(
                    text = "REC  SP  1998",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(hud, topLeft = Offset(28.dp.toPx(), 13.dp.toPx()))
                drawRect(
                    color = Color.White.copy(alpha = 0.35f * strength),
                    topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                    size = Size(w - 16.dp.toPx(), h - 16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            VideoEffectPreset.GOLDEN_SUNSET -> {
                val pulse = 0.85f + 0.15f * sin(tSec * 2.2f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB020).copy(alpha = 0.45f * strength * pulse),
                            Color(0xFFFF6B35).copy(alpha = 0.18f * strength),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.82f, h * 0.16f),
                        radius = w * 0.72f
                    ),
                    size = size
                )
            }

            VideoEffectPreset.PRISM_RAINBOW -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF2E88).copy(alpha = 0.22f * strength),
                            Color(0xFFFACC15).copy(alpha = 0.16f * strength),
                            Color(0xFF00E5FF).copy(alpha = 0.24f * strength),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    ),
                    size = size
                )
            }

            VideoEffectPreset.SPARKLE_BLING -> {
                val stars = listOf(0.18f to 0.20f, 0.78f to 0.16f, 0.25f to 0.72f, 0.82f to 0.62f, 0.52f to 0.30f)
                stars.forEachIndexed { idx, (nx, ny) ->
                    val tw = (sin(tSec * 4.5f + idx * 1.3f) * 0.5f + 0.5f)
                    val r = (7.dp.toPx() + 8.dp.toPx() * tw) * strength
                    val cx = w * nx
                    val cy = h * ny
                    val c = Color.White.copy(alpha = (0.4f + 0.6f * tw) * strength)
                    drawLine(c, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 2.dp.toPx())
                    drawLine(c, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 2.dp.toPx())
                }
            }

            VideoEffectPreset.DIAMOND_BLINK -> {
                val diamonds = listOf(0.22f to 0.26f, 0.75f to 0.22f, 0.35f to 0.68f, 0.78f to 0.72f)
                diamonds.forEachIndexed { idx, (nx, ny) ->
                    val tw = (sin(tSec * 6f + idx * 1.1f) * 0.5f + 0.5f)
                    val r = (8.dp.toPx() + 10.dp.toPx() * tw) * strength
                    val cx = w * nx
                    val cy = h * ny
                    val c = Color(0xFF67E8F9).copy(alpha = (0.45f + 0.55f * tw) * strength)
                    drawLine(c, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 2.5.dp.toPx())
                    drawLine(c, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 2.5.dp.toPx())
                    drawLine(c, Offset(cx - r * 0.6f, cy - r * 0.6f), Offset(cx + r * 0.6f, cy + r * 0.6f), strokeWidth = 1.5.dp.toPx())
                    drawLine(c, Offset(cx + r * 0.6f, cy - r * 0.6f), Offset(cx - r * 0.6f, cy + r * 0.6f), strokeWidth = 1.5.dp.toPx())
                }
            }

            VideoEffectPreset.SPLIT_4_GRID -> {
                val lineColor = Color.White.copy(alpha = 0.85f * strength)
                drawLine(lineColor, Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 2.dp.toPx())
                drawLine(lineColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 2.dp.toPx())
            }

            VideoEffectPreset.MIRROR_REFLECT -> {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.14f * strength),
                            Color(0xFF00E5FF).copy(alpha = 0.22f * strength)
                        ),
                        startX = w * 0.45f,
                        endX = w
                    ),
                    topLeft = Offset(w / 2f, 0f),
                    size = Size(w / 2f, h)
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f * strength),
                    start = Offset(w / 2f, 0f),
                    end = Offset(w / 2f, h),
                    strokeWidth = 2.dp.toPx()
                )
            }

            VideoEffectPreset.MOTION_BLUR -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f * strength),
                            Color.Black.copy(alpha = 0.24f * strength)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = maxOf(w, h) * 0.6f
                    ),
                    size = size
                )
            }

            VideoEffectPreset.CYBER_SCAN -> {
                val cyan = Color(0xFF00E5FF).copy(alpha = 0.85f * strength)
                val m = 12.dp.toPx()
                val l = 22.dp.toPx()
                val sw = 2.dp.toPx()
                drawLine(cyan, Offset(m, m), Offset(m + l, m), sw)
                drawLine(cyan, Offset(m, m), Offset(m, m + l), sw)
                drawLine(cyan, Offset(w - m, m), Offset(w - m - l, m), sw)
                drawLine(cyan, Offset(w - m, m), Offset(w - m, m + l), sw)
                drawLine(cyan, Offset(m, h - m), Offset(m + l, h - m), sw)
                drawLine(cyan, Offset(m, h - m), Offset(m, h - m - l), sw)
                drawLine(cyan, Offset(w - m, h - m), Offset(w - m - l, h - m), sw)
                drawLine(cyan, Offset(w - m, h - m), Offset(w - m, h - m - l), sw)

                val scanY = ((tSec * 0.6f) % 1f) * h
                drawLine(
                    color = cyan.copy(alpha = 0.55f * strength),
                    start = Offset(0f, scanY),
                    end = Offset(w, scanY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            VideoEffectPreset.GLITCH_RGB -> {
                val slice = ((tSec * 7f).toInt() % 5)
                val bandY = h * (0.18f + slice * 0.15f)
                drawRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.22f * strength),
                    topLeft = Offset(0f, bandY),
                    size = Size(w, 6.dp.toPx())
                )
                drawRect(
                    color = Color(0xFFFF2E88).copy(alpha = 0.22f * strength),
                    topLeft = Offset(0f, bandY + 8.dp.toPx()),
                    size = Size(w, 5.dp.toPx())
                )
            }

            VideoEffectPreset.TV_STATIC -> {
                val barOffset = ((tSec * 12f).toInt() % 6)
                for (i in 0 until 5) {
                    val y = h * (((i * 0.19f) + barOffset * 0.04f) % 1f)
                    drawRect(
                        color = Color.White.copy(alpha = 0.18f * strength),
                        topLeft = Offset(0f, y),
                        size = Size(w, 3.dp.toPx())
                    )
                }
            }

            VideoEffectPreset.SOFT_HALO -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f * strength),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.16f * strength)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = maxOf(w, h) * 0.6f
                    ),
                    size = size
                )
            }

            VideoEffectPreset.NEON_GLOW, VideoEffectPreset.CHROMATIC_PULSE -> {
                val pulse = 0.65f + 0.35f * cos(tSec * 4.0f)
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.45f * strength * pulse),
                            Color(0xFFFF2E88).copy(alpha = 0.45f * strength * pulse)
                        )
                    ),
                    size = size,
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            else -> Unit
        }

        if (vignette > 0.05f ||
            effectPreset == VideoEffectPreset.VIGNETTE_CINEMA ||
            effectPreset == VideoEffectPreset.FISHEYE_POP ||
            effectPreset == VideoEffectPreset.FILM_GRAIN
        ) {
            val vigAlpha = (vignette + 0.42f).coerceAtMost(0.82f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = vigAlpha)),
                    center = Offset(w / 2f, h / 2f),
                    radius = maxOf(w, h) * 0.62f
                ),
                size = size
            )
        }
    }
}

@Composable
fun TransitionPreviewOverlay(
    transitionType: TransitionType,
    progressRemaining: Float, // 1f at transition start, 0f at end of clip
    modifier: Modifier = Modifier
) {
    if (transitionType == TransitionType.NONE || progressRemaining <= 0f) return
    val p = (1f - progressRemaining).coerceIn(0f, 1f) // 0 -> 1 as clip exits

    Canvas(modifier = modifier.fillMaxSize()) {
        when (transitionType) {
            TransitionType.FLASH -> {
                drawRect(Color.White.copy(alpha = (p * 0.9f).coerceIn(0f, 0.9f)), size = size)
            }
            TransitionType.BLACK_FADE, TransitionType.FADE -> {
                drawRect(Color.Black.copy(alpha = (p * 0.85f).coerceIn(0f, 0.85f)), size = size)
            }
            TransitionType.WIPE_RIGHT -> {
                drawRect(Color.Black.copy(alpha = 0.88f), topLeft = Offset.Zero, size = Size(size.width * p, size.height))
            }
            TransitionType.BURN -> {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF6B35).copy(alpha = p * 0.85f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.8f
                    ),
                    size = size
                )
            }
            TransitionType.GLITCH_CUT -> {
                drawRect(Color(0xFF00E5FF).copy(alpha = p * 0.4f), topLeft = Offset(0f, size.height * 0.25f), size = Size(size.width, 24.dp.toPx()))
                drawRect(Color(0xFFFF2E88).copy(alpha = p * 0.4f), topLeft = Offset(0f, size.height * 0.65f), size = Size(size.width, 20.dp.toPx()))
            }
            else -> Unit
        }
    }
}
