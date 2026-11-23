package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.FaceDetector
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.AspectRatioOption
import com.example.model.FilterPreset
import com.example.model.TimelineMarker
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs

data class SmartAutoCropResult(
    val cropLeft: Float,
    val cropTop: Float,
    val cropRight: Float,
    val cropBottom: Float,
    val panX: Float,
    val panY: Float,
    val detectedSubjectLabel: String
)

data class BatchPhotoResult(
    val processedCount: Int,
    val savedUris: List<Uri>,
    val outputFolderLabel: String
)

object SmartMediaHelpers {

    /**
     * Analyzes a clip frame using Android FaceDetector with a luminance/edge saliency centroid fallback
     * so switching to 9:16, 16:9, 1:1, or 4:5 keeps the primary person or object framed cleanly.
     */
    suspend fun computeSmartAutoCrop(
        context: Context,
        clip: VideoClip,
        targetRatio: AspectRatioOption
    ): SmartAutoCropResult = withContext(Dispatchers.IO) {
        val raw = loadSampledBitmap(context, clip, maxDim = 480)
            ?: return@withContext fallbackCenteredCrop(targetRatio, "Center auto-framed")
        try {
            val w = raw.width.coerceAtLeast(32)
            val h = raw.height.coerceAtLeast(32)

            // 1. Try built-in Android FaceDetector on an even-width RGB_565 bitmap
            val evenW = if (w % 2 == 0) w else w - 1
            val rgb565 = Bitmap.createBitmap(evenW, h, Bitmap.Config.RGB_565)
            Canvas(rgb565).drawBitmap(raw, 0f, 0f, null)

            val faces = arrayOfNulls<FaceDetector.Face>(3)
            val detector = FaceDetector(evenW, h, 3)
            val faceCount = runCatching { detector.findFaces(rgb565, faces) }.getOrDefault(0)
            rgb565.recycle()

            var focusX = 0.5f
            var focusY = 0.44f
            var label = "Focal subject tracked"

            val bestFace = faces.take(faceCount).filterNotNull().maxByOrNull { it.confidence() }
            if (bestFace != null) {
                val mid = android.graphics.PointF()
                bestFace.getMidPoint(mid)
                focusX = (mid.x / evenW.toFloat()).coerceIn(0.15f, 0.85f)
                focusY = (mid.y / h.toFloat()).coerceIn(0.15f, 0.85f)
                label = "Face tracked & centered"
            } else {
                // 2. Luminance gradient saliency centroid fallback
                val centroid = computeSaliencyCentroid(raw)
                focusX = centroid.first
                focusY = centroid.second
                label = "Smart subject framed"
            }

            computeCropRectAroundFocus(
                sourceWidth = w.toFloat(),
                sourceHeight = h.toFloat(),
                focusX = focusX,
                focusY = focusY,
                targetRatio = targetRatio,
                label = label
            )
        } finally {
            raw.recycle()
        }
    }

    fun computeCropRectAroundFocus(
        sourceWidth: Float,
        sourceHeight: Float,
        focusX: Float,
        focusY: Float,
        targetRatio: AspectRatioOption,
        label: String = "Subject tracked"
    ): SmartAutoCropResult {
        val srcAspect = (sourceWidth / sourceHeight.coerceAtLeast(1f)).coerceIn(0.25f, 4f)
        val tgtAspect = (targetRatio.ratioWidth / targetRatio.ratioHeight.coerceAtLeast(1f)).coerceIn(0.25f, 4f)

        val normCropW: Float
        val normCropH: Float
        if (srcAspect > tgtAspect) {
            normCropH = 0.94f
            normCropW = (normCropH * tgtAspect / srcAspect).coerceIn(0.28f, 0.96f)
        } else {
            normCropW = 0.94f
            normCropH = (normCropW * srcAspect / tgtAspect).coerceIn(0.28f, 0.96f)
        }

        val cx = focusX.coerceIn(normCropW / 2f, 1f - normCropW / 2f)
        val cy = focusY.coerceIn(normCropH / 2f, 1f - normCropH / 2f)
        val left = (cx - normCropW / 2f).coerceIn(0f, 1f - normCropW)
        val top = (cy - normCropH / 2f).coerceIn(0f, 1f - normCropH)
        val right = (left + normCropW).coerceIn(left + 0.22f, 1f)
        val bottom = (top + normCropH).coerceIn(top + 0.22f, 1f)

        val panX = ((0.5f - focusX) * 0.35f).coerceIn(-0.45f, 0.45f)
        val panY = ((0.5f - focusY) * 0.35f).coerceIn(-0.45f, 0.45f)
        return SmartAutoCropResult(
            cropLeft = left,
            cropTop = top,
            cropRight = right,
            cropBottom = bottom,
            panX = panX,
            panY = panY,
            detectedSubjectLabel = label
        )
    }

    private fun fallbackCenteredCrop(targetRatio: AspectRatioOption, label: String): SmartAutoCropResult {
        return computeCropRectAroundFocus(1080f, 1920f, 0.5f, 0.45f, targetRatio, label)
    }

    private fun computeSaliencyCentroid(bmp: Bitmap): Pair<Float, Float> {
        val cols = 12
        val rows = 12
        val stepX = (bmp.width / cols).coerceAtLeast(1)
        val stepY = (bmp.height / rows).coerceAtLeast(1)
        var weightedX = 0.0
        var weightedY = 0.0
        var totalWeight = 0.0

        for (r in 1 until rows - 1) {
            for (c in 1 until cols - 1) {
                val x = c * stepX
                val y = r * stepY
                val pCenter = bmp.getPixel(x, y)
                val pRight = bmp.getPixel((x + stepX).coerceAtMost(bmp.width - 1), y)
                val pDown = bmp.getPixel(x, (y + stepY).coerceAtMost(bmp.height - 1))
                val lumC = 0.299 * Color.red(pCenter) + 0.587 * Color.green(pCenter) + 0.114 * Color.blue(pCenter)
                val lumR = 0.299 * Color.red(pRight) + 0.587 * Color.green(pRight) + 0.114 * Color.blue(pRight)
                val lumD = 0.299 * Color.red(pDown) + 0.587 * Color.green(pDown) + 0.114 * Color.blue(pDown)
                val edge = abs(lumC - lumR) + abs(lumC - lumD)
                val centerBias = 1.0 - (abs(c / cols.toDouble() - 0.5) + abs(r / rows.toDouble() - 0.5)) * 0.4
                val weight = edge * centerBias
                weightedX += (c + 0.5) / cols.toDouble() * weight
                weightedY += (r + 0.5) / rows.toDouble() * weight
                totalWeight += weight
            }
        }
        if (totalWeight <= 1.0) return 0.5f to 0.46f
        return Pair(
            (weightedX / totalWeight).toFloat().coerceIn(0.2f, 0.8f),
            (weightedY / totalWeight).toFloat().coerceIn(0.2f, 0.8f)
        )
    }

    private fun loadSampledBitmap(context: Context, clip: VideoClip, maxDim: Int): Bitmap? {
        return try {
            val uri = Uri.parse(clip.uriString)
            val raw = if (clip.isPhoto) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(clip.trimStartMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    runCatching { retriever.release() }
                }
            } ?: return null

            val scale = (maxDim.toFloat() / maxOf(raw.width, raw.height).coerceAtLeast(1)).coerceAtMost(1f)
            if (scale < 1f) {
                val scaled = Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt().coerceAtLeast(16),
                    (raw.height * scale).toInt().coerceAtLeast(16),
                    true
                )
                raw.recycle()
                scaled
            } else raw
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates beat markers spaced rhythmically across totalDurationMs.
     */
    fun generateBeatMarkers(
        totalDurationMs: Long,
        bpm: Int = 124,
        barsPerMarker: Int = 2
    ): List<TimelineMarker> {
        val safeTotal = totalDurationMs.coerceAtLeast(1000L)
        val safeBpm = bpm.coerceIn(60, 180)
        val beatMs = (60_000L / safeBpm) * barsPerMarker.coerceIn(1, 4)
        val markers = mutableListOf<TimelineMarker>()
        var t = beatMs
        var beatIndex = 1
        while (t < safeTotal - 250L && markers.size < 24) {
            markers.add(
                TimelineMarker(
                    timeMs = t,
                    label = "♪ Beat $beatIndex",
                    colorHex = if (beatIndex % 2 == 1) 0xFF00E5FF else 0xFFFF2E88
                )
            )
            t += beatMs
            beatIndex++
        }
        return markers
    }

    /**
     * Trims / aligns clips to rhythmic beat intervals while preserving clip order.
     */
    fun applyRhythmCutToClips(
        clips: List<VideoClip>,
        bpm: Int = 124,
        beatsPerClip: Int = 4
    ): List<VideoClip> {
        if (clips.isEmpty()) return clips
        val beatLenMs = ((60_000L / bpm.coerceIn(60, 180)) * beatsPerClip.coerceIn(1, 8)).coerceIn(800L, 4500L)
        return clips.map { clip ->
            val maxAvail = clip.totalDurationMs.coerceAtLeast(900L)
            val targetRawSpan = (beatLenMs * clip.speed).toLong().coerceIn(700L, maxAvail)
            val safeEnd = (clip.trimStartMs + targetRawSpan).coerceAtMost(maxAvail)
            clip.copy(
                trimEndMs = safeEnd.coerceAtLeast((clip.trimStartMs + 500L).coerceAtMost(maxAvail))
            )
        }
    }

    /**
     * Batch processes multiple photos with filter preset, target aspect crop, and optional s.s watermark,
     * saving outputs cleanly to Android Gallery (Pictures/s.s_Batch) via MediaStore.
     */
    suspend fun processBatchPhotos(
        context: Context,
        uris: List<Uri>,
        filterPreset: FilterPreset,
        targetRatio: AspectRatioOption,
        addWatermark: Boolean,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BatchPhotoResult = withContext(Dispatchers.IO) {
        val saved = mutableListOf<Uri>()
        val typeface = ResourcesCompat.getFont(context, R.font.space_grotesk) ?: Typeface.DEFAULT_BOLD

        uris.forEachIndexed { idx, uri ->
            onProgress(idx + 1, uris.size)
            val src = context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, opts)
                val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
                var sample = 1
                while (maxDim / sample > 1600) sample *= 2
                context.contentResolver.openInputStream(uri)?.use { s2 ->
                    BitmapFactory.decodeStream(s2, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            } ?: return@forEachIndexed

            val outW = targetRatio.exportWidth
            val outH = targetRatio.exportHeight
            val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBmp)

            // Compute center-crop rect
            val srcRatio = src.width.toFloat() / src.height.coerceAtLeast(1)
            val dstRatio = outW.toFloat() / outH.toFloat()
            val cropRect = if (srcRatio > dstRatio) {
                val cropW = (src.height * dstRatio).toInt().coerceAtLeast(1)
                val x = ((src.width - cropW) / 2).coerceAtLeast(0)
                Rect(x, 0, x + cropW, src.height)
            } else {
                val cropH = (src.width / dstRatio).toInt().coerceAtLeast(1)
                val y = ((src.height - cropH) / 2).coerceAtLeast(0)
                Rect(0, y, src.width, y + cropH)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (filterPreset != FilterPreset.ORIGINAL) {
                val cm = ColorMatrix().apply { setSaturation(filterPreset.saturation) }
                val bShift = filterPreset.brightness * 255f
                val cScale = 1f + filterPreset.contrast
                val cTrans = (-0.5f * cScale + 0.5f) * 255f + bShift
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            filterPreset.redScale * cScale, 0f, 0f, 0f, cTrans,
                            0f, filterPreset.greenScale * cScale, 0f, 0f, cTrans,
                            0f, 0f, filterPreset.blueScale * cScale, 0f, cTrans,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
            }

            canvas.drawBitmap(src, cropRect, Rect(0, 0, outW, outH), paint)
            src.recycle()

            if (addWatermark) {
                drawWatermarkBadge(canvas, outW, outH, typeface)
            }

            val savedUri = saveBitmapToGallery(
                context = context,
                bitmap = outBmp,
                displayName = "ss_batch_${System.currentTimeMillis()}_${idx + 1}.jpg"
            )
            outBmp.recycle()
            if (savedUri != null) saved.add(savedUri)
        }

        BatchPhotoResult(
            processedCount = saved.size,
            savedUris = saved,
            outputFolderLabel = "Pictures/s.s_Batch"
        )
    }

    fun drawWatermarkBadge(canvas: Canvas, width: Int, height: Int, typeface: Typeface) {
        val text = "s.s STUDIO"
        val textSize = (width * 0.032f).coerceIn(22f, 44f)
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            this.textSize = textSize
            this.typeface = typeface
        }
        val tw = txtPaint.measureText(text)
        val padX = textSize * 0.65f
        val padY = textSize * 0.40f
        val right = width - width * 0.045f
        val bottom = height - height * 0.045f
        val rect = RectF(right - tw - padX * 2f, bottom - textSize - padY * 1.6f, right, bottom)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 10, 14, 22)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(200, 0, 229, 255)
        }
        canvas.drawRoundRect(rect, 18f, 18f, bgPaint)
        canvas.drawRoundRect(rect, 18f, 18f, borderPaint)
        canvas.drawText(text, rect.left + padX, rect.bottom - padY * 0.7f, txtPaint)
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/s.s_Batch")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(context.filesDir, "batch_photos").apply { mkdirs() }
                val file = File(dir, displayName)
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out) }
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveVideoToGallery(context: Context, file: File, projectTitle: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext Result.failure(IllegalStateException("Exported MP4 not found"))
            try {
                val safeName = projectTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "ss_video" }
                val displayName = "${safeName}_${System.currentTimeMillis()}.mp4"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/s.s_Studio")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure(IllegalStateException("MediaStore insert failed"))
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(file).use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Result.success(uri)
                } else {
                    Result.success(Uri.fromFile(file))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun shareVideoShortcut(
        context: Context,
        file: File,
        targetPackage: String? = null,
        chooserTitle: String = "Share s.s Video"
    ): Boolean {
        if (!file.exists()) return false
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!targetPackage.isNullOrBlank()) {
                    val installed = context.packageManager.getLaunchIntentForPackage(targetPackage) != null
                    if (installed) setPackage(targetPackage)
                }
            }
            if (intent.`package` != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, chooserTitle))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
