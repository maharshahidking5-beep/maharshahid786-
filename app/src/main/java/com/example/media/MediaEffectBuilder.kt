package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import com.example.R
import com.example.model.FilterPreset
import com.example.model.ProjectData
import com.example.model.StickerOverlayItem
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.google.common.collect.ImmutableList

@UnstableApi
object MediaEffectBuilder {

    fun buildClipVideoEffects(
        project: ProjectData,
        clip: VideoClip,
        includeExportPresentation: Boolean,
        context: Context? = null
    ): List<Effect> {
        val effects = mutableListOf<Effect>()

        // 1. Rotation and Flip
        if (clip.rotationDegrees != 0 || clip.flipHorizontal || clip.zoomScale != 1.0f) {
            val scaleX = if (clip.flipHorizontal) -clip.zoomScale else clip.zoomScale
            val scaleY = clip.zoomScale
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(clip.rotationDegrees.toFloat())
                    .setScale(scaleX, scaleY)
                    .build()
            )
        }

        // 2. Color filter preset + fine-tuned adjustments
        val filter = project.filterPreset
        val intensity = project.filterIntensity.coerceIn(0f, 1f)
        val adj = project.adjustments

        val combinedBrightness = (filter.brightness * intensity + adj.brightness).coerceIn(-0.9f, 0.9f)
        val combinedContrast = (filter.contrast * intensity + adj.contrast).coerceIn(-0.9f, 0.9f)
        val combinedSaturation = ((1f + (filter.saturation - 1f) * intensity) * adj.saturation).coerceIn(0f, 2.5f)

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

        // 3. Creative Video FX presets
        when (project.effectPreset) {
            VideoEffectPreset.NONE -> Unit
            VideoEffectPreset.NEON_GLOW -> {
                effects.add(Contrast(0.28f))
                effects.add(HslAdjustment.Builder().adjustSaturation(45f).build())
            }
            VideoEffectPreset.VHS_RETRO -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.12f)
                        .setGreenScale(0.95f)
                        .setBlueScale(1.08f)
                        .build()
                )
                effects.add(Contrast(-0.12f))
            }
            VideoEffectPreset.ZOOM_PULSE -> {
                effects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setScale(1.08f, 1.08f)
                        .build()
                )
            }
            VideoEffectPreset.THERMAL -> {
                effects.add(RgbFilter.createInvertedFilter())
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
                effects.add(Contrast(0.22f))
                effects.add(Brightness(-0.05f))
            }
            VideoEffectPreset.GLITCH_RGB -> {
                effects.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.18f)
                        .setGreenScale(0.88f)
                        .setBlueScale(1.22f)
                        .build()
                )
            }
        }

        // 4. Export Resolution & Aspect Ratio framing
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

            // Bake text & sticker overlays into exported MP4 using OverlayEffect
            if (context != null && (project.textOverlays.isNotEmpty() || project.stickerOverlays.isNotEmpty())) {
                val overlayBmp = renderOverlaysBitmap(
                    context = context,
                    width = evenW,
                    height = evenH,
                    texts = project.textOverlays,
                    stickers = project.stickerOverlays
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
        stickers: List<StickerOverlayItem>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val typeface = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD

        // Draw stickers
        stickers.forEach { sticker ->
            val cx = sticker.normX.coerceIn(0.1f, 0.9f) * width
            val cy = sticker.normY.coerceIn(0.1f, 0.9f) * height
            val textSize = (width * 0.045f * sticker.scale).coerceIn(24f, 96f)
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
            canvas.drawRoundRect(rect, 24f, 24f, bgPaint)
            canvas.drawText(sticker.badgeText, cx, cy, textPaint)
        }

        // Draw text overlays
        texts.forEach { item ->
            val cx = item.normX.coerceIn(0.1f, 0.9f) * width
            val cy = item.normY.coerceIn(0.1f, 0.9f) * height
            val textSize = (width * 0.062f * item.scale).coerceIn(28f, 120f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorHex.toInt()
                this.textSize = textSize
                this.typeface = typeface
                textAlign = Paint.Align.CENTER
            }
            val textWidth = textPaint.measureText(item.text)
            if ((item.bgHex ushr 24) > 0) {
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
            if (item.stylePreset.hasOutline) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = textSize * 0.12f
                    color = Color.BLACK
                }
                canvas.drawText(item.text, cx, cy, strokePaint)
            }
            canvas.drawText(item.text, cx, cy, textPaint)
        }

        return bitmap
    }

    /**
     * Compose ColorMatrix for ultra-responsive preview rendering on Image clips and thumbnails.
     */
    fun buildComposeColorFilter(project: ProjectData): ColorFilter {
        val filter = project.filterPreset
        val intensity = project.filterIntensity.coerceIn(0f, 1f)
        val adj = project.adjustments

        val sat = ((1f + (filter.saturation - 1f) * intensity) * adj.saturation).coerceIn(0f, 2.5f)
        val matrix = ColorMatrix().apply {
            setToSaturation(if (filter == FilterPreset.BW_NOIR && intensity > 0.5f) 0f else sat)
        }

        val brightnessShift = (filter.brightness * intensity + adj.brightness) * 255f
        val contrastScale = (1f + filter.contrast * intensity + adj.contrast).coerceIn(0.3f, 2.2f)
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
