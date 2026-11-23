package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import com.example.R
import com.example.model.AiRemoverMode
import com.example.model.AiRemoverZone
import com.example.model.ClipAnimationType
import com.example.model.FilterPreset
import com.example.model.ProjectData
import com.example.model.RemoveBgMode
import com.example.model.StickerOverlayItem
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.ui.theme.StudioFontCatalog

@UnstableApi
object MediaEffectBuilder {

    fun buildClipVideoEffects(
        project: ProjectData,
        clip: VideoClip,
        includeExportPresentation: Boolean,
        context: Context? = null
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        // 0. Frame Crop (normalized [0..1] -> NDC [-1..1])
        if (clip.hasCrop) {
            val leftNdc = (clip.cropLeft.coerceIn(0f, 0.85f) * 2f - 1f)
            val rightNdc = (clip.cropRight.coerceIn(clip.cropLeft + 0.1f, 1f) * 2f - 1f)
            val topNdc = (1f - clip.cropTop.coerceIn(0f, 0.85f) * 2f)
            val bottomNdc = (1f - clip.cropBottom.coerceIn(clip.cropTop + 0.1f, 1f) * 2f)
            if (rightNdc > leftNdc + 0.05f && topNdc > bottomNdc + 0.05f) {
                effects.add(Crop(leftNdc, rightNdc, bottomNdc, topNdc))
            }
        }

        // 1. Rotation, Flip, Reflected Mirror, Zoom & Clip Animation zoom framing
        val animZoom = when (clip.animation) {
            ClipAnimationType.ZOOM_IN -> 1.14f
            ClipAnimationType.ZOOM_OUT -> 1.06f
            ClipAnimationType.BOUNCE, ClipAnimationType.SHAKE_POP -> 1.05f
            else -> 1.0f
        }
        val finalScale = (clip.zoomScale * animZoom).coerceIn(0.3f, 4.0f)
        val effectiveRot = clip.totalRotationDegrees
        if (kotlin.math.abs(effectiveRot) > 0.1f ||
            clip.flipHorizontal ||
            clip.flipVertical ||
            kotlin.math.abs(finalScale - 1.0f) > 0.01f
        ) {
            val scaleX = if (clip.flipHorizontal) -finalScale else finalScale
            val scaleY = if (clip.flipVertical) -finalScale else finalScale
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(effectiveRot)
                    .setScale(scaleX, scaleY)
                    .build()
            )
        }

        // 2. Color filter preset + fine-tuned adjustments (including clarity, fade, highlights)
        val filter = project.filterPreset
        val intensity = project.filterIntensity.coerceIn(0f, 1f)
        val adj = project.adjustments
        val clarityContrast = (adj.clarity * 0.22f) + (if (adj.autoEnhanced) 0.10f else 0f)
        val autoSatBoost = if (adj.autoEnhanced) 1.12f else 1f
        val autoBrightLift = if (adj.autoEnhanced) 0.03f else 0f
        val fadeContrastReduction = adj.fade * 0.35f
        val fadeBrightnessLift = adj.fade * 0.12f
        val highlightLift = adj.highlights * 0.28f

        val combinedBrightness = (filter.brightness * intensity + adj.brightness + autoBrightLift + fadeBrightnessLift + highlightLift)
            .coerceIn(-0.9f, 0.9f)
        val combinedContrast = (filter.contrast * intensity + adj.contrast + clarityContrast - fadeContrastReduction)
            .coerceIn(-0.9f, 0.9f)
        val combinedSaturation = ((1f + (filter.saturation - 1f) * intensity) * adj.saturation * autoSatBoost)
            .coerceIn(0f, 2.5f)

        if (kotlin.math.abs(combinedBrightness) > 0.01f) {
            effects.add(Brightness(combinedBrightness))
        }
        if (kotlin.math.abs(combinedContrast) > 0.01f) {
            effects.add(Contrast(combinedContrast))
        }

        if (filter == FilterPreset.BW_NOIR && intensity > 0.6f) {
            effects.add(RgbFilter.createGrayscaleFilter())
        } else {
            val satDelta = ((combinedSaturation - 1f) * 100f).coerceIn(-100f, 100f)
            if (kotlin.math.abs(satDelta) > 1f) {
                effects.add(
                    HslAdjustment.Builder()
                        .adjustSaturation(satDelta)
                        .build()
                )
            }
        }

        val warmRed = (1f + adj.warmth * 0.35f) * (1f + (filter.redScale - 1f) * intensity)
        val warmGreen = (1f + (filter.greenScale - 1f) * intensity)
        val warmBlue = (1f - adj.warmth * 0.35f) * (1f + (filter.blueScale - 1f) * intensity)
        if (kotlin.math.abs(warmRed - 1f) > 0.02f ||
            kotlin.math.abs(warmGreen - 1f) > 0.02f ||
            kotlin.math.abs(warmBlue - 1f) > 0.02f
        ) {
            effects.add(
                RgbAdjustment.Builder()
                    .setRedScale(warmRed.coerceIn(0.4f, 1.8f))
                    .setGreenScale(warmGreen.coerceIn(0.4f, 1.8f))
                    .setBlueScale(warmBlue.coerceIn(0.4f, 1.8f))
                    .build()
            )
        }

        // 3. Creative CapCut Pro Video FX presets (scaled by effectIntensity)
        val fxStrength = project.effectIntensity.coerceIn(0.1f, 1f)
        when (project.effectPreset) {
            VideoEffectPreset.NONE -> Unit
            VideoEffectPreset.ZOOM_ON_BURST -> {
                val z = 1f + 0.24f * fxStrength
                effects.add(ScaleAndRotateTransformation.Builder().setScale(z, z).build())
            }
            VideoEffectPreset.ZOOM_OFF_PULL -> {
                val z = (1f - 0.14f * fxStrength).coerceAtLeast(0.75f)
                effects.add(ScaleAndRotateTransformation.Builder().setScale(z, z).build())
            }
            VideoEffectPreset.MIRROR_REFLECT -> {
                effects.add(ScaleAndRotateTransformation.Builder().setScale(-1f, 1f).build())
            }
            VideoEffectPreset.SPLIT_4_GRID -> {
                effects.add(Contrast(0.16f * fxStrength))
                effects.add(HslAdjustment.Builder().adjustSaturation(16f * fxStrength).build())
            }
            VideoEffectPreset.DIAMOND_BLINK -> {
                effects.add(Brightness(0.08f * fxStrength))
                effects.add(Contrast(0.22f * fxStrength))
            }
            VideoEffectPreset.MOTION_BLUR -> {
                val z = 1f + 0.06f * fxStrength
                effects.add(ScaleAndRotateTransformation.Builder().setScale(z, z).build())
                effects.add(Contrast(0.10f * fxStrength))
            }
            VideoEffectPreset.NEON_GLOW -> {
                effects.add(Contrast(0.28f * fxStrength))
                effects.add(HslAdjustment.Builder().adjustSaturation(45f * fxStrength).build())
            }
            VideoEffectPreset.ZOOM_PULSE,
            VideoEffectPreset.HEARTBEAT_ZOOM -> {
                val z = 1f + 0.09f * fxStrength
                effects.add(ScaleAndRotateTransformation.Builder().setScale(z, z).build())
            }
            VideoEffectPreset.CHROMATIC_PULSE -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1f + 0.16f * fxStrength)
                        .setGreenScale(0.95f)
                        .setBlueScale(1f + 0.22f * fxStrength)
                        .build()
                )
                effects.add(HslAdjustment.Builder().adjustSaturation(28f * fxStrength).build())
            }
            VideoEffectPreset.THERMAL -> {
                effects.add(RgbFilter.createInvertedFilter())
            }
            VideoEffectPreset.SHAKE_BEAT -> {
                val z = 1f + 0.06f * fxStrength
                effects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(1.5f * fxStrength)
                        .setScale(z, z)
                        .build()
                )
            }
            VideoEffectPreset.STROBE_FLASH -> {
                effects.add(Brightness(0.14f * fxStrength))
                effects.add(Contrast(0.24f * fxStrength))
            }
            VideoEffectPreset.CLUB_LASER -> {
                effects.add(Contrast(0.20f * fxStrength))
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.94f)
                        .setGreenScale(1f + 0.08f * fxStrength)
                        .setBlueScale(1f + 0.22f * fxStrength)
                        .build()
                )
            }
            VideoEffectPreset.BLACK_FLASH -> {
                effects.add(Brightness(-0.10f * fxStrength))
                effects.add(Contrast(0.30f * fxStrength))
            }
            VideoEffectPreset.VHS_RETRO -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1f + 0.12f * fxStrength)
                        .setGreenScale(0.95f)
                        .setBlueScale(1f + 0.08f * fxStrength)
                        .build()
                )
                effects.add(Contrast(-0.12f * fxStrength))
            }
            VideoEffectPreset.DV_CAM_98 -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.06f)
                        .setGreenScale(1.04f)
                        .setBlueScale(0.92f)
                        .build()
                )
                effects.add(Contrast(0.10f * fxStrength))
            }
            VideoEffectPreset.FILM_GRAIN -> {
                effects.add(Contrast(0.16f * fxStrength))
                effects.add(Brightness(-0.03f * fxStrength))
                effects.add(HslAdjustment.Builder().adjustSaturation(-14f * fxStrength).build())
            }
            VideoEffectPreset.SEPIA -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.22f)
                        .setGreenScale(1.02f)
                        .setBlueScale(0.74f)
                        .build()
                )
            }
            VideoEffectPreset.VIGNETTE_CINEMA -> {
                effects.add(Contrast(0.22f * fxStrength))
                effects.add(Brightness(-0.05f * fxStrength))
            }
            VideoEffectPreset.ANAMORPHIC_BARS -> {
                effects.add(Contrast(0.14f * fxStrength))
                effects.add(HslAdjustment.Builder().adjustSaturation(10f * fxStrength).build())
            }
            VideoEffectPreset.FISHEYE_POP -> {
                val z = 1f + 0.14f * fxStrength
                effects.add(ScaleAndRotateTransformation.Builder().setScale(z, z).build())
                effects.add(Contrast(0.12f * fxStrength))
            }
            VideoEffectPreset.SOFT_HALO -> {
                effects.add(Brightness(0.10f * fxStrength))
                effects.add(Contrast(-0.10f * fxStrength))
                effects.add(HslAdjustment.Builder().adjustSaturation(14f * fxStrength).build())
            }
            VideoEffectPreset.GOLDEN_SUNSET -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1f + 0.24f * fxStrength)
                        .setGreenScale(1f + 0.06f * fxStrength)
                        .setBlueScale((1f - 0.18f * fxStrength).coerceAtLeast(0.6f))
                        .build()
                )
                effects.add(Brightness(0.06f * fxStrength))
            }
            VideoEffectPreset.PRISM_RAINBOW -> {
                effects.add(HslAdjustment.Builder().adjustSaturation(32f * fxStrength).build())
                effects.add(Brightness(0.05f * fxStrength))
            }
            VideoEffectPreset.SPARKLE_BLING -> {
                effects.add(Brightness(0.06f * fxStrength))
                effects.add(Contrast(0.15f * fxStrength))
            }
            VideoEffectPreset.GLITCH_RGB -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1f + 0.18f * fxStrength)
                        .setGreenScale(0.88f)
                        .setBlueScale(1f + 0.22f * fxStrength)
                        .build()
                )
            }
            VideoEffectPreset.TV_STATIC -> {
                effects.add(HslAdjustment.Builder().adjustSaturation(-35f * fxStrength).build())
                effects.add(Contrast(0.26f * fxStrength))
            }
            VideoEffectPreset.CYBER_SCAN -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.88f)
                        .setGreenScale(1.06f)
                        .setBlueScale(1f + 0.20f * fxStrength)
                        .build()
                )
            }
        }

        // 4. Export Resolution, Aspect Ratio framing, AI Remover, Visual FX graphics & Text/Sticker Overlays
        if (includeExportPresentation) {
            val baseW = (project.aspectRatio.exportWidth * project.resolution.scaleFactor).toInt()
                .coerceAtLeast(480)
            val baseH = (project.aspectRatio.exportHeight * project.resolution.scaleFactor).toInt()
                .coerceAtLeast(480)
            val evenW = baseW - (baseW % 2)
            val evenH = baseH - (baseH % 2)
            effects.add(
                Presentation.createForWidthAndHeight(
                    evenW,
                    evenH,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )

            val hasVisualFxOverlay = project.effectPreset in setOf(
                VideoEffectPreset.ANAMORPHIC_BARS,
                VideoEffectPreset.VHS_RETRO,
                VideoEffectPreset.DV_CAM_98,
                VideoEffectPreset.CLUB_LASER,
                VideoEffectPreset.GOLDEN_SUNSET,
                VideoEffectPreset.PRISM_RAINBOW,
                VideoEffectPreset.SPARKLE_BLING,
                VideoEffectPreset.DIAMOND_BLINK,
                VideoEffectPreset.SPLIT_4_GRID,
                VideoEffectPreset.MIRROR_REFLECT,
                VideoEffectPreset.CYBER_SCAN,
                VideoEffectPreset.TV_STATIC,
                VideoEffectPreset.VIGNETTE_CINEMA,
                VideoEffectPreset.FISHEYE_POP
            ) || adj.vignette > 0.05f

            val hasRemoverOrBg = clip.aiRemoverZones.isNotEmpty() || clip.removeBgMode != RemoveBgMode.NONE

            if (context != null && (project.textOverlays.isNotEmpty() || project.stickerOverlays.isNotEmpty() || hasVisualFxOverlay || hasRemoverOrBg || project.addWatermark)) {
                val overlayBmp = renderOverlaysBitmap(
                    context = context,
                    width = evenW,
                    height = evenH,
                    texts = project.textOverlays,
                    stickers = project.stickerOverlays,
                    effectPreset = project.effectPreset,
                    effectIntensity = project.effectIntensity,
                    vignette = adj.vignette,
                    removerZones = clip.aiRemoverZones,
                    removeBgMode = clip.removeBgMode,
                    customBgHex = clip.customBgHex,
                    addWatermark = project.addWatermark
                )
                val settings = StaticOverlaySettings.Builder()
                    .setAlphaScale(1f)
                    .build()
                val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBmp, settings)
                effects.add(OverlayEffect(listOf<TextureOverlay>(bitmapOverlay)))
            }
        }

        return effects
    }

    private fun renderOverlaysBitmap(
        context: Context,
        width: Int,
        height: Int,
        texts: List<TextOverlayItem>,
        stickers: List<StickerOverlayItem>,
        effectPreset: VideoEffectPreset,
        effectIntensity: Float,
        vignette: Float,
        removerZones: List<AiRemoverZone>,
        removeBgMode: RemoveBgMode,
        customBgHex: Long,
        addWatermark: Boolean
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val typeface = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD
        val alphaInt = (effectIntensity.coerceIn(0.2f, 1f) * 200).toInt()

        // Render Remove BG framing backdrop matte if enabled
        if (removeBgMode != RemoveBgMode.NONE) {
            val mattePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f, height / 2f, maxOf(width, height) * 0.45f,
                    Color.TRANSPARENT,
                    Color.argb(
                        215,
                        Color.red(customBgHex.toInt()),
                        Color.green(customBgHex.toInt()),
                        Color.blue(customBgHex.toInt())
                    ),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mattePaint)
        }

        // Render AI Object/Watermark Remover zones
        removerZones.forEach { zone ->
            val cx = zone.normX * width
            val cy = zone.normY * height
            val rw = zone.normWidth * width
            val rh = zone.normHeight * height
            val rect = RectF(cx - rw / 2f, cy - rh / 2f, cx + rw / 2f, cy + rh / 2f)
            when (zone.mode) {
                AiRemoverMode.SMART_PATCH -> {
                    val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx, cy, maxOf(rw, rh) * 0.65f,
                            Color.argb(245, 22, 26, 36),
                            Color.argb(140, 22, 26, 36),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRoundRect(rect, 28f, 28f, patchPaint)
                }
                AiRemoverMode.SOFT_BLUR -> {
                    val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(215, 200, 210, 225)
                    }
                    canvas.drawRoundRect(rect, 32f, 32f, blurPaint)
                }
                AiRemoverMode.MOSAIC -> {
                    val cols = 6
                    val rows = 4
                    val cw = rw / cols
                    val ch = rh / rows
                    val tilePaint = Paint()
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val shade = if ((r + c) % 2 == 0) 48 else 82
                            tilePaint.color = Color.argb(242, shade, shade + 6, shade + 14)
                            canvas.drawRect(
                                rect.left + c * cw,
                                rect.top + r * ch,
                                rect.left + (c + 1) * cw,
                                rect.top + (r + 1) * ch,
                                tilePaint
                            )
                        }
                    }
                }
            }
        }

        // Render baked CapCut visual effect overlays for export parity
        when (effectPreset) {
            VideoEffectPreset.ANAMORPHIC_BARS -> {
                val barH = height * 0.12f * effectIntensity.coerceIn(0.4f, 1f)
                val barPaint = Paint().apply { color = Color.BLACK }
                canvas.drawRect(0f, 0f, width.toFloat(), barH, barPaint)
                canvas.drawRect(0f, height - barH, width.toFloat(), height.toFloat(), barPaint)
            }
            VideoEffectPreset.SPLIT_4_GRID -> {
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(220, 255, 255, 255)
                    strokeWidth = 4f
                }
                canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), gridPaint)
                canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, gridPaint)
            }
            VideoEffectPreset.MIRROR_REFLECT -> {
                val splitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(180, 0, 229, 255)
                    strokeWidth = 3f
                }
                canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), splitPaint)
            }
            VideoEffectPreset.VHS_RETRO, VideoEffectPreset.TV_STATIC -> {
                val linePaint = Paint().apply {
                    color = Color.argb((alphaInt * 0.28f).toInt(), 255, 255, 255)
                    strokeWidth = 2f
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
                    y += 10f
                }
            }
            VideoEffectPreset.DV_CAM_98 -> {
                val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(230, 255, 255, 255)
                    textSize = width * 0.036f
                    this.typeface = Typeface.MONOSPACE
                }
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(235, 255, 46, 136)
                }
                canvas.drawCircle(width * 0.08f, height * 0.07f, width * 0.012f, dotPaint)
                canvas.drawText("REC  SP  1998", width * 0.11f, height * 0.08f, hudPaint)
            }
            VideoEffectPreset.CLUB_LASER -> {
                val beam1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height * 0.8f,
                        Color.argb((alphaInt * 0.45f).toInt(), 0, 229, 255),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                val beam2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        width.toFloat(), 0f, 0f, height * 0.85f,
                        Color.argb((alphaInt * 0.42f).toInt(), 255, 46, 136),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), beam1)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), beam2)
            }
            VideoEffectPreset.GOLDEN_SUNSET -> {
                val flare = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.82f, height * 0.18f, width * 0.65f,
                        Color.argb((alphaInt * 0.55f).toInt(), 255, 180, 60),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flare)
            }
            VideoEffectPreset.PRISM_RAINBOW -> {
                val prism = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        intArrayOf(
                            Color.argb(65, 255, 46, 136),
                            Color.argb(45, 250, 204, 21),
                            Color.argb(65, 0, 229, 255)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), prism)
            }
            VideoEffectPreset.SPARKLE_BLING, VideoEffectPreset.DIAMOND_BLINK -> {
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(230, 255, 255, 255)
                    strokeWidth = 3f
                }
                val pts = listOf(0.18f to 0.22f, 0.78f to 0.16f, 0.24f to 0.74f, 0.84f to 0.62f, 0.52f to 0.32f)
                pts.forEach { (nx, ny) ->
                    val cx = nx * width
                    val cy = ny * height
                    val r = width * 0.028f
                    canvas.drawLine(cx - r, cy, cx + r, cy, starPaint)
                    canvas.drawLine(cx, cy - r, cx, cy + r, starPaint)
                }
            }
            VideoEffectPreset.CYBER_SCAN -> {
                val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(210, 0, 229, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                val m = width * 0.05f
                val l = width * 0.10f
                canvas.drawLine(m, m, m + l, m, hudPaint)
                canvas.drawLine(m, m, m, m + l, hudPaint)
                canvas.drawLine(width - m, m, width - m - l, m, hudPaint)
                canvas.drawLine(width - m, m, width - m, m + l, hudPaint)
                canvas.drawLine(m, height - m, m + l, height - m, hudPaint)
                canvas.drawLine(m, height - m, m, height - m - l, hudPaint)
                canvas.drawLine(width - m, height - m, width - m - l, height - m, hudPaint)
                canvas.drawLine(width - m, height - m, width - m, height - m - l, hudPaint)
            }
            else -> Unit
        }

        if (vignette > 0.05f || effectPreset == VideoEffectPreset.VIGNETTE_CINEMA || effectPreset == VideoEffectPreset.FISHEYE_POP) {
            val vigAlpha = ((vignette + 0.45f).coerceAtMost(0.85f) * 255).toInt()
            val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    width / 2f, height / 2f, maxOf(width, height) * 0.65f,
                    Color.TRANSPARENT,
                    Color.argb(vigAlpha, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vigPaint)
        }

        // Draw stickers with rotation
        stickers.forEach { sticker ->
            val cx = sticker.normX.coerceIn(0.08f, 0.92f) * width
            val cy = sticker.normY.coerceIn(0.08f, 0.92f) * height
            val textSize = (width * 0.045f * sticker.scale).coerceIn(22f, 110f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.textSize = textSize
                this.typeface = typeface
                textAlign = Paint.Align.CENTER
            }
            val textWidth = textPaint.measureText(sticker.badgeText)
            val padX = textSize * 0.7f
            val padY = textSize * 0.4f
            val rect = RectF(
                cx - textWidth / 2f - padX,
                cy - textSize * 0.75f - padY,
                cx + textWidth / 2f + padX,
                cy + textSize * 0.35f + padY
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sticker.accentHex.toInt()
            }
            canvas.save()
            canvas.rotate(sticker.rotationDegrees, cx, cy)
            canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
            canvas.drawText(sticker.badgeText, cx, cy, textPaint)
            canvas.restore()
        }

        // Draw text overlays with rotation, multilingual fonts, curved arc, neon glow & 3D shadow
        texts.forEach { item ->
            val cx = item.normX.coerceIn(0.08f, 0.92f) * width
            val cy = item.normY.coerceIn(0.08f, 0.92f) * height
            val textSize = (width * 0.062f * item.scale).coerceIn(24f, 140f)
            val fontRes = StudioFontCatalog.optionForKey(item.fontFamilyKey).fontResId
            val itemTypeface = ResourcesCompat.getFont(context, fontRes) ?: typeface
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorHex.toInt()
                this.textSize = textSize
                this.typeface = itemTypeface
                textAlign = Paint.Align.CENTER
                if (item.isNeonGlow || item.stylePreset.hasGlow) {
                    setShadowLayer(textSize * 0.35f, 0f, 0f, item.colorHex.toInt())
                }
            }
            val textWidth = textPaint.measureText(item.text)
            canvas.save()
            canvas.rotate(item.rotationDegrees, cx, cy)
            if ((item.bgHex ushr 24) > 0 && !item.isCurved) {
                val padX = textSize * 0.6f
                val padY = textSize * 0.35f
                val rect = RectF(
                    cx - textWidth / 2f - padX,
                    cy - textSize * 0.8f - padY,
                    cx + textWidth / 2f + padX,
                    cy + textSize * 0.3f + padY
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = item.bgHex.toInt()
                }
                canvas.drawRoundRect(rect, 20f, 20f, bgPaint)
            }
            if (item.isCurved) {
                val arcRadius = (textWidth * 0.65f).coerceAtLeast(textSize * 3.0f)
                val arcOval = RectF(cx - arcRadius, cy - arcRadius * 0.4f, cx + arcRadius, cy + arcRadius * 1.6f)
                val arcPath = android.graphics.Path().apply {
                    addArc(arcOval, 210f, 120f)
                }
                if (item.is3D) {
                    val depthPaint = Paint(textPaint).apply { color = Color.argb(220, 15, 23, 42) }
                    canvas.save()
                    canvas.translate(textSize * 0.09f, textSize * 0.09f)
                    canvas.drawTextOnPath(item.text, arcPath, 0f, 0f, depthPaint)
                    canvas.restore()
                }
                canvas.drawTextOnPath(item.text, arcPath, 0f, 0f, textPaint)
            } else {
                if (item.is3D) {
                    val depthPaint = Paint(textPaint).apply {
                        clearShadowLayer()
                        color = Color.argb(230, 15, 23, 42)
                    }
                    for (layer in 4 downTo 1) {
                        val off = layer * (textSize * 0.028f)
                        canvas.drawText(item.text, cx + off, cy + off, depthPaint)
                    }
                }
                if (item.stylePreset.hasOutline) {
                    val strokePaint = Paint(textPaint).apply {
                        clearShadowLayer()
                        style = Paint.Style.STROKE
                        strokeWidth = textSize * 0.12f
                        color = Color.BLACK
                    }
                    canvas.drawText(item.text, cx, cy, strokePaint)
                }
                canvas.drawText(item.text, cx, cy, textPaint)
            }
            canvas.restore()
        }

        if (addWatermark) {
            SmartMediaHelpers.drawWatermarkBadge(canvas, width, height, typeface)
        }

        return bitmap
    }

    fun buildComposeColorFilter(project: ProjectData): ColorFilter {
        val filter = project.filterPreset
        val intensity = project.filterIntensity.coerceIn(0f, 1f)
        val adj = project.adjustments
        val clarityContrast = (adj.clarity * 0.22f) + (if (adj.autoEnhanced) 0.10f else 0f)
        val autoSatBoost = if (adj.autoEnhanced) 1.12f else 1f
        val autoBrightLift = if (adj.autoEnhanced) 0.03f else 0f

        val sat = ((1f + (filter.saturation - 1f) * intensity) * adj.saturation * autoSatBoost).coerceIn(0f, 2.5f)
        val matrix = ColorMatrix().apply {
            setToSaturation(if (filter == FilterPreset.BW_NOIR && intensity > 0.5f) 0f else sat)
        }

        val brightnessShift = (filter.brightness * intensity + adj.brightness + autoBrightLift + adj.fade * 0.12f + adj.highlights * 0.28f) * 255f
        val contrastScale = (1f + filter.contrast * intensity + adj.contrast + clarityContrast - adj.fade * 0.35f).coerceIn(0.3f, 2.2f)
        val contrastTranslate = (-0.5f * contrastScale + 0.5f) * 255f + brightnessShift

        val rScale = (1f + (filter.redScale - 1f) * intensity) * (1f + adj.warmth * 0.3f) * contrastScale
        val gScale = (1f + (filter.greenScale - 1f) * intensity) * contrastScale
        val bScale = (1f + (filter.blueScale - 1f) * intensity) * (1f - adj.warmth * 0.3f) * contrastScale

        val scaleMatrix = ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, contrastTranslate,
                0f, gScale, 0f, 0f, contrastTranslate,
                0f, 0f, bScale, 0f, contrastTranslate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.timesAssign(scaleMatrix)

        if (project.effectPreset == VideoEffectPreset.THERMAL) {
            val invert = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.timesAssign(invert)
        } else if (project.effectPreset == VideoEffectPreset.SEPIA) {
            val sepia = ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.timesAssign(sepia)
        }

        return ColorFilter.colorMatrix(matrix)
    }
}
