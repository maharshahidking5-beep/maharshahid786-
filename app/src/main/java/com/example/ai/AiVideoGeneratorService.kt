package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.media.AudioSynthLibrary
import com.example.media.SmartMediaHelpers
import com.example.model.AspectRatioOption
import com.example.model.ClipAnimationType
import com.example.model.ColorAdjustments
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.ProjectData
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoCoverConfig
import com.example.model.VideoEffectPreset
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "AiVideoGenerator"

enum class AiVideoVisualStyle(val label: String, val promptDirective: String) {
    CINEMATIC("Cinematic 8K", "cinematic anamorphic film photography, dramatic volumetric lighting, ultra sharp 8k detail"),
    NEON_CYBER("Cyber Neon", "futuristic neon cyberpunk atmosphere, reflective wet streets, glowing cyan and magenta lights"),
    LUXURY_GOLD("Luxury Gold", "royal luxury commercial shoot, warm golden bokeh, crisp product macro lighting"),
    PRODUCT_AD("3D Product Ad", "sleek studio 3D commercial render, floating dynamic elements, high-contrast rim light"),
    TRAVEL_REEL("Scenic Travel", "golden hour drone cinematography, vivid natural scenery, dreamy depth of field"),
    ANIME_DREAM("Anime Pro", "makoto shinkai style vibrant anime cinema art, luminous skies, richly painted frames")
}

data class AiGeneratedScene(
    val sceneIndex: Int,
    val visualPrompt: String,
    val captionText: String,
    val imageUriString: String,
    val durationMs: Long,
    val animation: ClipAnimationType,
    val transitionAfter: TransitionType
)

data class AiGeneratedVideoResult(
    val generationId: String = UUID.randomUUID().toString(),
    val userPrompt: String,
    val videoTitle: String,
    val style: AiVideoVisualStyle,
    val aspectRatio: AspectRatioOption,
    val scenes: List<AiGeneratedScene>,
    val project: ProjectData
)

sealed interface AiVideoGenUiState {
    data object Idle : AiVideoGenUiState
    data class Generating(val stageLabel: String, val progressFraction: Float) : AiVideoGenUiState
    data class Ready(val result: AiGeneratedVideoResult) : AiVideoGenUiState
    data class Error(val message: String) : AiVideoGenUiState
}

class AiVideoGeneratorService {

    private val sceneItemSchema = Schema.obj(
        properties = mapOf(
            "visualPrompt" to Schema.string(description = "Rich detailed English visual prompt for AI image generation matching the user prompt"),
            "captionText" to Schema.string(description = "Short punchy on-screen video caption in the same language as user prompt (2-6 words)"),
            "durationMs" to Schema.integer(description = "Scene duration in milliseconds from 2600 to 3800"),
            "animation" to Schema.enumeration(
                values = listOf(
                    ClipAnimationType.ZOOM_IN.name,
                    ClipAnimationType.ZOOM_OUT.name,
                    ClipAnimationType.BOUNCE.name,
                    ClipAnimationType.PAN_LEFT.name,
                    ClipAnimationType.FADE_IN.name
                ),
                description = "Camera motion animation for this scene"
            ),
            "transition" to Schema.enumeration(
                values = listOf(
                    TransitionType.ZOOM.name,
                    TransitionType.BLACK_FADE.name,
                    TransitionType.SLIDE.name,
                    TransitionType.GLITCH_CUT.name,
                    TransitionType.FLASH.name
                ),
                description = "Transition after this scene"
            )
        )
    )

    private val storyboardSchema = Schema.obj(
        properties = mapOf(
            "videoTitle" to Schema.string(description = "Catchy short 2-4 word project title"),
            "filterPreset" to Schema.enumeration(
                values = listOf(
                    FilterPreset.CRISP_HDR.name,
                    FilterPreset.TEAL_ORANGE.name,
                    FilterPreset.CYBER_TEAL.name,
                    FilterPreset.SUNSET_GLOW.name,
                    FilterPreset.FUJI_PRO.name,
                    FilterPreset.EMERALD_CINE.name
                ),
                description = "Color grade filter preset"
            ),
            "effectPreset" to Schema.enumeration(
                values = listOf(
                    VideoEffectPreset.NONE.name,
                    VideoEffectPreset.GOLDEN_SUNSET.name,
                    VideoEffectPreset.SOFT_HALO.name,
                    VideoEffectPreset.CHROMATIC_PULSE.name,
                    VideoEffectPreset.SPARKLE_BLING.name,
                    VideoEffectPreset.VHS_RETRO.name
                ),
                description = "Visual FX preset"
            ),
            "audioPresetId" to Schema.enumeration(
                values = AudioSynthLibrary.presets.map { it.id },
                description = "Best matching background music track ID"
            ),
            "scenes" to Schema.array(
                items = sceneItemSchema,
                description = "Exactly 3 sequential visual scenes telling the story of the prompt"
            )
        )
    )

    private val directorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    private val portraitImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.PORTRAIT_9x16)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    private val landscapeImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    private val squareImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateAiVideo(
        context: Context,
        prompt: String,
        style: AiVideoVisualStyle,
        aspectRatio: AspectRatioOption,
        addWatermark: Boolean,
        targetResolution: ExportResolution,
        targetFps: Int,
        onProgress: (stage: String, fraction: Float) -> Unit
    ): AiGeneratedVideoResult {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotBlank()) { "Please enter a video prompt first." }

        val generationId = UUID.randomUUID().toString().take(12)
        onProgress("Step 1/4 · Directing AI storyboard from prompt...", 0.14f)

        val storyboardResponse = try {
            withTimeout(60_000L) {
                directorModel.generateContent(
                    """
                    You are an elite AI Video Director inside s.s Studio Pro.
                    Turn this user prompt into a dynamic 3-scene short video storyboard:
                    User Prompt: "$cleanPrompt"
                    Visual Style: "${style.label} (${style.promptDirective})"
                    Aspect Ratio: "${aspectRatio.label}"
                    
                    Rules:
                    - Produce EXACTLY 3 scenes in "scenes".
                    - Keep every scene's visualPrompt crisp, photorealistic/cinematic, cohesive with the story, and free of watermarks or cluttered text.
                    - Match captionText language to the user's prompt language (Urdu script if Urdu prompt, Hindi if Hindi, English if English).
                    """.trimIndent()
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Storyboard director timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Storyboard director failed", e)
            throw e
        }

        val jsonStr = storyboardResponse.text ?: throw IllegalStateException("AI director returned empty storyboard.")
        val root = JSONObject(jsonStr)
        val videoTitle = root.optString("videoTitle", "AI Pro Reel").take(48).ifBlank { "AI Pro Reel" }
        val filterPreset = runCatching { FilterPreset.valueOf(root.optString("filterPreset")) }
            .getOrDefault(FilterPreset.CRISP_HDR)
        val effectPreset = runCatching { VideoEffectPreset.valueOf(root.optString("effectPreset")) }
            .getOrDefault(VideoEffectPreset.SOFT_HALO)
        val audioId = root.optString("audioPresetId", "cinema_rise")
        val rawScenes = root.optJSONArray("scenes")
            ?: throw IllegalStateException("AI storyboard did not contain scenes.")

        val sceneCount = rawScenes.length().coerceIn(1, 3)
        val imageModel = when (aspectRatio) {
            AspectRatioOption.RATIO_16_9 -> landscapeImageModel
            AspectRatioOption.RATIO_1_1 -> squareImageModel
            else -> portraitImageModel
        }

        val outputDir = File(context.filesDir, "ai_generated_scenes/$generationId").apply { mkdirs() }
        val generatedScenes = mutableListOf<AiGeneratedScene>()

        for (index in 0 until sceneCount) {
            val sObj = rawScenes.getJSONObject(index)
            val visualPrompt = sObj.optString("visualPrompt", cleanPrompt).ifBlank { cleanPrompt }
            val captionText = sObj.optString("captionText", "").take(64)
            val durationMs = sObj.optLong("durationMs", 3000L).coerceIn(2400L, 4200L)
            val animation = runCatching { ClipAnimationType.valueOf(sObj.optString("animation")) }
                .getOrDefault(if (index % 2 == 0) ClipAnimationType.ZOOM_IN else ClipAnimationType.ZOOM_OUT)
            val transition = runCatching { TransitionType.valueOf(sObj.optString("transition")) }
                .getOrDefault(TransitionType.ZOOM)

            val stepFraction = 0.25f + (index.toFloat() / sceneCount.toFloat()) * 0.65f
            onProgress("Step ${index + 2}/4 · Synthesizing AI Scene ${index + 1}/$sceneCount...", stepFraction)

            val fullImagePrompt = "$visualPrompt, ${style.promptDirective}, cohesive scene ${index + 1} of $sceneCount for video concept: $cleanPrompt"
            val bitmap = try {
                withTimeout(60_000L) {
                    val imgResponse = imageModel.generateContent(fullImagePrompt)
                    imgResponse.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI scene $index image generation timed out", e)
                throw e
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Quota exceeded during AI scene generation", e)
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image model failed for scene ${index + 1}", e)
                throw e
            }

            if (bitmap == null) {
                throw IllegalStateException("AI visual model returned no frame for scene ${index + 1}. Please retry with a clearer prompt.")
            }

            val sceneFile = File(outputDir, "scene_${index + 1}.png")
            withContext(Dispatchers.IO) {
                FileOutputStream(sceneFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                bitmap.recycle()
            }

            generatedScenes.add(
                AiGeneratedScene(
                    sceneIndex = index + 1,
                    visualPrompt = visualPrompt,
                    captionText = captionText,
                    imageUriString = Uri.fromFile(sceneFile).toString(),
                    durationMs = durationMs,
                    animation = animation,
                    transitionAfter = if (index < sceneCount - 1) transition else TransitionType.NONE
                )
            )
        }

        onProgress("Finalizing 1080P+ timeline & soundtrack...", 0.94f)
        val audioTrack = AudioSynthLibrary.ensureTrackFile(context, audioId)
        val clips = generatedScenes.map { scene ->
            VideoClip(
                uriString = scene.imageUriString,
                isPhoto = true,
                displayName = "AI Scene ${scene.sceneIndex}",
                totalDurationMs = scene.durationMs,
                trimStartMs = 0L,
                trimEndMs = scene.durationMs,
                speed = 1.0f,
                animation = scene.animation,
                transitionAfter = scene.transitionAfter,
                zoomScale = 1.04f
            )
        }

        var cursorMs = 0L
        val overlays = generatedScenes.mapIndexedNotNull { idx, scene ->
            val start = cursorMs
            val end = cursorMs + scene.durationMs
            cursorMs = end
            if (scene.captionText.isBlank()) null
            else {
                val isRtlText = scene.captionText.any { ch ->
                    ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F'
                }
                TextOverlayItem(
                    text = scene.captionText,
                    stylePreset = if (idx == 0) TextStylePreset.CYBER_NEON else TextStylePreset.SUBTITLE,
                    startMs = start,
                    endMs = end,
                    normX = 0.5f,
                    normY = if (idx == 0) 0.72f else 0.78f,
                    animationType = TextAnimationType.POP,
                    isNeonGlow = idx == 0,
                    isRtl = isRtlText,
                    languageTag = if (isRtlText) "UR" else "EN"
                )
            }
        }

        val totalMs = clips.sumOf { it.effectiveDurationMs }
        val project = ProjectData(
            title = videoTitle,
            aspectRatio = aspectRatio,
            resolution = targetResolution,
            fps = targetFps,
            clips = clips,
            audioTrack = audioTrack,
            textOverlays = overlays,
            markers = SmartMediaHelpers.generateBeatMarkers(totalMs, bpm = 120),
            coverConfig = VideoCoverConfig(
                coverImageUri = generatedScenes.firstOrNull()?.imageUriString,
                coverTitle = videoTitle,
                coverBadge = "AI VIDEO PRO"
            ),
            filterPreset = filterPreset,
            effectPreset = effectPreset,
            adjustments = ColorAdjustments(contrast = 0.12f, saturation = 1.14f, clarity = 0.45f, autoEnhanced = true),
            addWatermark = addWatermark,
            updatedAtMs = System.currentTimeMillis()
        )

        return AiGeneratedVideoResult(
            generationId = generationId,
            userPrompt = cleanPrompt,
            videoTitle = videoTitle,
            style = style,
            aspectRatio = aspectRatio,
            scenes = generatedScenes,
            project = project
        )
    }
}
