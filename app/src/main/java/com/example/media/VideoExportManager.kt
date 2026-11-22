package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.ExportEntity
import com.example.model.ProjectData
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ExportUiState {
    data object Idle : ExportUiState()
    data class Exporting(val progressPercent: Int, val stageLabel: String) : ExportUiState()
    data class Completed(val exportEntity: ExportEntity) : ExportUiState()
    data class Failed(val message: String) : ExportUiState()
}

@UnstableApi
class VideoExportManager(private val context: Context) {

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null

    fun startExport(
        project: ProjectData,
        onExportCompleted: (ExportEntity) -> Unit
    ) {
        if (project.clips.isEmpty()) {
            _exportState.value = ExportUiState.Failed("Add at least one video or photo clip before exporting.")
            return
        }

        cancelExport()

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeTitle = project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "ss_edit" }
        val outputFile = File(exportDir, "${safeTitle}_${System.currentTimeMillis()}.mp4")
        val outputPath = outputFile.absolutePath

        _exportState.value = ExportUiState.Exporting(2, "Preparing timeline tracks...")

        val videoEditedItems = project.clips.map { clip ->
            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(clip.uriString))
            if (clip.isPhoto) {
                mediaItemBuilder.setImageDurationMs(clip.effectiveDurationMs)
            } else {
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 300L))
                        .build()
                )
            }
            val mediaItem = mediaItemBuilder.build()

            val videoEffects = MediaEffectBuilder.buildClipVideoEffects(
                project = project,
                clip = clip,
                includeExportPresentation = true,
                context = context
            )
            val audioProcessors = mutableListOf<AudioProcessor>()
            if (!clip.isPhoto && kotlin.math.abs(clip.speed - 1.0f) > 0.05f) {
                audioProcessors.add(SonicAudioProcessor().apply { setSpeed(clip.speed) })
            }

            val builder = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(audioProcessors, videoEffects))
                .setRemoveAudio(clip.isPhoto || project.isOriginalAudioMuted || clip.volume <= 0.02f)

            if (clip.isPhoto) {
                builder.setFrameRate(project.fps.coerceIn(24, 60))
                builder.setDurationUs(clip.effectiveDurationMs * 1000L)
            } else {
                builder.setDurationUs(clip.totalDurationMs * 1000L)
            }
            builder.build()
        }

        val sequences = mutableListOf<EditedMediaItemSequence>()
        sequences.add(EditedMediaItemSequence.Builder(videoEditedItems).build())

        val musicTrack = project.audioTrack
        if (musicTrack != null && !musicTrack.isMuted && musicTrack.volume > 0.02f) {
            val totalMs = project.totalDurationMs.coerceAtLeast(1000L)
            val audioClipEnd = totalMs.coerceAtMost(musicTrack.durationMs)
            val audioMediaItem = MediaItem.Builder()
                .setUri(Uri.parse(musicTrack.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0L)
                        .setEndPositionMs(audioClipEnd)
                        .build()
                )
                .build()
            val editedAudio = EditedMediaItem.Builder(audioMediaItem)
                .setRemoveVideo(true)
                .setDurationUs(musicTrack.durationMs * 1000L)
                .build()
            sequences.add(EditedMediaItemSequence.Builder(listOf(editedAudio)).build())
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    stopProgressPolling()
                    val entity = ExportEntity(
                        id = UUID.randomUUID().toString(),
                        projectTitle = project.title,
                        filePath = outputPath,
                        durationMs = project.totalDurationMs,
                        resolutionLabel = "${project.resolution.label} · ${project.fps}fps",
                        fileSizeBytes = outputFile.length(),
                        createdAtMs = System.currentTimeMillis()
                    )
                    _exportState.value = ExportUiState.Completed(entity)
                    onExportCompleted(entity)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    stopProgressPolling()
                    runCatching { outputFile.delete() }
                    _exportState.value = ExportUiState.Failed(
                        exportException.localizedMessage ?: "Export failed on this device codec."
                    )
                }
            })
            .build()

        activeTransformer = transformer
        transformer.start(composition, outputPath)
        startProgressPolling(transformer)
    }

    private fun startProgressPolling(transformer: Transformer) {
        val progressHolder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val pct = progressHolder.progress.coerceIn(1, 99)
                    val label = when {
                        pct < 30 -> "Applying effects & color grading..."
                        pct < 75 -> "Rendering frames (${pct}%)..."
                        else -> "Muxing audio & finalizing MP4..."
                    }
                    _exportState.value = ExportUiState.Exporting(pct, label)
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 160L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 160L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun cancelExport() {
        stopProgressPolling()
        activeTransformer?.cancel()
        activeTransformer = null
    }

    fun resetState() {
        _exportState.value = ExportUiState.Idle
    }
}

object MediaInspector {

    suspend fun inspectPickedClip(context: Context, uri: Uri): VideoClip = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: ""
        val isPhoto = mime.startsWith("image/")
        val name = queryDisplayName(context, uri) ?: if (isPhoto) "Photo" else "Video Clip"

        if (isPhoto) {
            return@withContext VideoClip(
                uriString = uri.toString(),
                isPhoto = true,
                displayName = name,
                totalDurationMs = 4000L,
                trimStartMs = 0L,
                trimEndMs = 4000L
            )
        }

        val durationMs = extractDurationMs(context, uri).coerceAtLeast(1000L)
        VideoClip(
            uriString = uri.toString(),
            isPhoto = false,
            displayName = name,
            totalDurationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = durationMs
        )
    }

    suspend fun extractThumbnail(context: Context, clip: VideoClip): Bitmap? = withContext(Dispatchers.IO) {
        if (clip.isPhoto) return@withContext null
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(context, Uri.parse(clip.uriString))
            retriever.getFrameAtTime(clip.trimStartMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull() ?: 5000L
        } catch (_: Exception) {
            5000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }
}
