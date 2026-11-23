package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.model.FilterPreset
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiLabService"

data class AiEnhanceRecommendation(
    val summary: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val clarity: Float,
    val filterPreset: FilterPreset,
    val effectPreset: VideoEffectPreset,
    val hookCaption: String
)

data class AiCaptionPack(
    val hookTitle: String,
    val middleCaption: String,
    val outroCta: String
)

data class AiTranslatedCaptionPack(
    val targetLanguage: String,
    val line1: String,
    val line2: String,
    val line3: String
)

sealed interface AiLabUiState {
    data object Idle : AiLabUiState
    data class Loading(val taskLabel: String) : AiLabUiState
    data class StreamingScript(val scriptText: String) : AiLabUiState
    data class EnhanceReady(val recommendation: AiEnhanceRecommendation) : AiLabUiState
    data class CaptionsReady(val captions: AiCaptionPack) : AiLabUiState
    data class TranslatedReady(val translation: AiTranslatedCaptionPack) : AiLabUiState
    data class ScriptReady(val scriptText: String) : AiLabUiState
    data class Error(val message: String) : AiLabUiState
}

class AiLabService {

    private val enhanceSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Brief 1-2 sentence visual quality assessment and enhancement rationale"),
            "brightness" to Schema.double(description = "Brightness adjustment from -0.25 to 0.25"),
            "contrast" to Schema.double(description = "Contrast adjustment from -0.15 to 0.35"),
            "saturation" to Schema.double(description = "Saturation multiplier from 0.7 to 1.5"),
            "warmth" to Schema.double(description = "Color temperature warmth from -0.3 to 0.3"),
            "clarity" to Schema.double(description = "HD clarity boost from 0.1 to 0.9"),
            "filter" to Schema.enumeration(
                values = FilterPreset.entries.map { it.name },
                description = "Recommended filter preset"
            ),
            "effect" to Schema.enumeration(
                values = VideoEffectPreset.entries.map { it.name },
                description = "Recommended subtle video effect"
            ),
            "hookCaption" to Schema.string(description = "Short punchy 2-4 word title hook for this video")
        )
    )

    private val captionSchema = Schema.obj(
        properties = mapOf(
            "hookTitle" to Schema.string(description = "Opening 3-5 word viral hook text"),
            "middleCaption" to Schema.string(description = "Engaging mid-video caption line"),
            "outroCta" to Schema.string(description = "Short closing call-to-action caption")
        )
    )

    private val translationSchema = Schema.obj(
        properties = mapOf(
            "line1" to Schema.string(description = "First translated subtitle segment"),
            "line2" to Schema.string(description = "Second translated subtitle segment"),
            "line3" to Schema.string(description = "Third translated subtitle segment")
        )
    )

    private val enhanceModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = enhanceSchema
            }
        )
    }

    private val captionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = captionSchema
            }
        )
    }

    private val translationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
            }
        )
    }

    private val scriptModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun analyzeAndRecommendEnhancement(
        context: Context,
        clip: VideoClip?,
        projectTitle: String
    ): AiEnhanceRecommendation {
        val frameBitmap = clip?.let { extractSampledBitmap(context, it) }
        return try {
            val response = withTimeout(60_000L) {
                if (frameBitmap != null) {
                    enhanceModel.generateContent(
                        content {
                            image(frameBitmap)
                            text(
                                "You are a pro mobile video colorist in s.s Studio. " +
                                    "Analyze this video frame from project '$projectTitle' and return " +
                                    "ideal 1080P+ color-grading, clarity, filter, effect, and title hook settings."
                            )
                        }
                    )
                } else {
                    enhanceModel.generateContent(
                        "You are a pro mobile video colorist in s.s Studio. " +
                            "For a video project titled '$projectTitle', return balanced 1080P+ " +
                            "color-grading, clarity, filter, effect, and title hook settings."
                    )
                }
            }
            val jsonStr = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val obj = JSONObject(jsonStr)
            val filterName = obj.optString("filter", FilterPreset.CRISP_HDR.name)
            val effectName = obj.optString("effect", VideoEffectPreset.NONE.name)

            AiEnhanceRecommendation(
                summary = obj.optString("summary", "Balanced 1080P+ clarity and dynamic contrast applied."),
                brightness = obj.optDouble("brightness", 0.04).toFloat().coerceIn(-0.35f, 0.35f),
                contrast = obj.optDouble("contrast", 0.18).toFloat().coerceIn(-0.25f, 0.40f),
                saturation = obj.optDouble("saturation", 1.18).toFloat().coerceIn(0.4f, 1.8f),
                warmth = obj.optDouble("warmth", 0.04).toFloat().coerceIn(-0.4f, 0.4f),
                clarity = obj.optDouble("clarity", 0.65).toFloat().coerceIn(0f, 1f),
                filterPreset = runCatching { FilterPreset.valueOf(filterName) }.getOrDefault(FilterPreset.CRISP_HDR),
                effectPreset = runCatching { VideoEffectPreset.valueOf(effectName) }.getOrDefault(VideoEffectPreset.NONE),
                hookCaption = obj.optString("hookCaption", "ULTRA HD CUT")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI enhancement analysis timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI enhancement analysis failed", e)
            throw e
        } finally {
            frameBitmap?.recycle()
        }
    }

    suspend fun generateSmartCaptions(
        context: Context,
        clip: VideoClip?,
        topicPrompt: String
    ): AiCaptionPack {
        val frameBitmap = clip?.let { extractSampledBitmap(context, it) }
        return try {
            val promptText = if (topicPrompt.isNotBlank()) {
                "Create 3 short, high-retention CapCut-style video overlay captions for topic: '$topicPrompt'."
            } else {
                "Analyze this visual frame and create 3 short, high-retention social reel overlay captions."
            }
            val response = withTimeout(60_000L) {
                if (frameBitmap != null) {
                    captionModel.generateContent(
                        content {
                            image(frameBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    captionModel.generateContent(promptText)
                }
            }
            val jsonStr = response.text ?: throw IllegalStateException("AI returned empty captions.")
            val obj = JSONObject(jsonStr)
            AiCaptionPack(
                hookTitle = obj.optString("hookTitle", "WAIT FOR THIS"),
                middleCaption = obj.optString("middleCaption", "Every frame tells a story"),
                outroCta = obj.optString("outroCta", "Follow for more")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI caption generation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI caption generation failed", e)
            throw e
        } finally {
            frameBitmap?.recycle()
        }
    }

    fun streamCreatorScript(topic: String, tone: String): Flow<String> = flow {
        val cleanTopic = topic.trim().ifBlank { "cinematic mobile edit showcase" }
        val prompt = """
            Write a concise 20-second short-form video voiceover script + shot pacing cues for:
            Topic: $cleanTopic
            Tone: $tone
            Format cleanly with Hook (0-3s), Body (3-15s), and Outro CTA (15-20s). Keep it punchy and ready to read aloud.
        """.trimIndent()

        val acc = StringBuilder()
        withTimeout(60_000L) {
            scriptModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    acc.append(part)
                    emit(acc.toString())
                }
            }
        }
        if (acc.isBlank()) {
            throw IllegalStateException("AI returned an empty script.")
        }
    }

    suspend fun translateAndCaptionVideo(
        context: Context,
        clip: VideoClip?,
        sourceDialogue: String,
        targetLanguage: String
    ): AiTranslatedCaptionPack {
        val frameBitmap = clip?.let { extractSampledBitmap(context, it) }
        return try {
            val promptText = if (sourceDialogue.isNotBlank()) {
                "Translate the following video captions or spoken dialogue naturally into $targetLanguage, split into 3 punchy timed subtitle lines: \"$sourceDialogue\"."
            } else {
                "Inspect this video frame and create 3 natural, punchy short-form video subtitle lines written in $targetLanguage."
            }
            val response = withTimeout(60_000L) {
                if (frameBitmap != null) {
                    translationModel.generateContent(
                        content {
                            image(frameBitmap)
                            text(promptText)
                        }
                    )
                } else {
                    translationModel.generateContent(promptText)
                }
            }
            val jsonStr = response.text ?: throw IllegalStateException("AI returned empty translation.")
            val obj = JSONObject(jsonStr)
            AiTranslatedCaptionPack(
                targetLanguage = targetLanguage,
                line1 = obj.optString("line1", ""),
                line2 = obj.optString("line2", ""),
                line3 = obj.optString("line3", "")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Video translation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video translation failed", e)
            throw e
        } finally {
            frameBitmap?.recycle()
        }
    }

    fun streamAssistantHelp(
        question: String,
        projectContextSummary: String,
        recentHistory: List<Pair<String, String>>
    ): Flow<String> = flow {
        val cleanQuestion = question.trim()
        require(cleanQuestion.isNotBlank()) { "Ask a question first." }
        val historyBlock = if (recentHistory.isEmpty()) {
            ""
        } else {
            recentHistory.takeLast(6).joinToString("\n") { (role, msg) -> "$role: $msg" } + "\n"
        }
        val prompt = """
            You are SS Assistant, the friendly built-in editing helper inside s.s Studio Pro (mobile CapCut + Canva video editor).
            Answer clearly in short, actionable steps (3-5 bullet points max). Match the user's language (English, Urdu, or Hindi) if they write/ask in it.
            Current studio context: $projectContextSummary
            $historyBlock
            User question: $cleanQuestion
        """.trimIndent()

        val acc = StringBuilder()
        withTimeout(60_000L) {
            scriptModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    acc.append(part)
                    emit(acc.toString())
                }
            }
        }
        if (acc.isBlank()) {
            throw IllegalStateException("SS Assistant returned an empty response.")
        }
    }

    private suspend fun extractSampledBitmap(context: Context, clip: VideoClip): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(clip.uriString)
                val raw = if (clip.isPhoto) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        retriever.getFrameAtTime(
                            clip.trimStartMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    } finally {
                        runCatching { retriever.release() }
                    }
                } ?: return@withContext null

                // Scale down to <= 768px max dimension to stay well under payload limits
                val maxDim = 768
                val scale = (maxDim.toFloat() / maxOf(raw.width, raw.height)).coerceAtMost(1f)
                if (scale < 1f) {
                    val scaled = Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * scale).toInt().coerceAtLeast(1),
                        (raw.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                    raw.recycle()
                    scaled
                } else {
                    raw
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract frame for AI analysis", e)
                null
            }
        }
}
