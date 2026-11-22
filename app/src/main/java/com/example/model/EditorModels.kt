package com.example.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectRatioOption(
    val label: String,
    val ratioWidth: Float,
    val ratioHeight: Float,
    val exportWidth: Int,
    val exportHeight: Int
) {
    RATIO_9_16("9:16", 9f, 16f, 1080, 1920),
    RATIO_16_9("16:9", 16f, 9f, 1920, 1080),
    RATIO_1_1("1:1", 1f, 1f, 1080, 1080),
    RATIO_4_5("4:5", 4f, 5f, 1080, 1350),
    RATIO_3_4("3:4", 3f, 4f, 1080, 1440)
}

@Serializable
enum class ExportResolution(val label: String, val scaleFactor: Float, val bitrateLabel: String) {
    RES_720P("720P", 0.67f, "8 Mbps"),
    RES_1080P("1080P", 1.0f, "16 Mbps"),
    RES_2K("2K/4K", 1.5f, "28 Mbps")
}

@Serializable
enum class FilterPreset(
    val label: String,
    val badgeColorHex: Long,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val redScale: Float,
    val greenScale: Float,
    val blueScale: Float
) {
    ORIGINAL("Original", 0xFF64748B, 0f, 0f, 1f, 1f, 1f, 1f),
    TEAL_ORANGE("Teal & Orange", 0xFF00E5FF, 0.02f, 0.18f, 1.18f, 1.12f, 1.0f, 1.08f),
    KODAK_GOLD("Kodak Gold", 0xFFF59E0B, 0.05f, 0.12f, 1.15f, 1.14f, 1.04f, 0.88f),
    TOKYO_NIGHT("Tokyo Night", 0xFF8B5CF6, -0.04f, 0.24f, 1.25f, 0.94f, 0.98f, 1.18f),
    MOODY_DARK("Moody", 0xFF334155, -0.08f, 0.28f, 0.82f, 0.96f, 0.98f, 1.04f),
    PASTEL_POP("Pastel Pop", 0xFFFF2E88, 0.08f, -0.06f, 1.22f, 1.06f, 1.02f, 1.06f),
    CRISP_HDR("Crisp HDR", 0xFF10B981, 0.03f, 0.30f, 1.28f, 1.03f, 1.03f, 1.03f),
    BW_NOIR("B&W Noir", 0xFFE2E8F0, 0.0f, 0.35f, 0.0f, 1f, 1f, 1f)
}

@Serializable
enum class VideoEffectPreset(val label: String, val tag: String) {
    NONE("None", "Off"),
    NEON_GLOW("Neon Glow", "Cyber"),
    VHS_RETRO("VHS Retro", "90s"),
    ZOOM_PULSE("Zoom Pulse", "Beat"),
    THERMAL("Thermal", "FX"),
    SEPIA("Vintage Sepia", "Film"),
    VIGNETTE_CINEMA("Cinema Vignette", "Pro"),
    GLITCH_RGB("RGB Split", "Glitch")
}

@Serializable
enum class TransitionType(val label: String, val durationMs: Long) {
    NONE("None", 0L),
    FADE("Fade", 500L),
    SLIDE("Slide", 450L),
    ZOOM("Zoom", 500L),
    FLASH("Flash", 350L),
    SPIN("Spin", 500L)
}

@Serializable
enum class TextStylePreset(
    val label: String,
    val textColorHex: Long,
    val bgColorHex: Long,
    val hasOutline: Boolean,
    val hasGlow: Boolean
) {
    CLASSIC("Classic", 0xFFFFFFFF, 0x00000000, true, false),
    CYBER_NEON("Cyber Neon", 0xFF00E5FF, 0xCC0A0B0E, false, true),
    ROSE_POP("Rose Pill", 0xFFFFFFFF, 0xFFFF2E88, false, false),
    STUDIO_BADGE("Studio Box", 0xFF0A0B0E, 0xFFF8FAFC, false, false),
    CINEMA_GOLD("Cinema Gold", 0xFFFDE047, 0x99000000, true, true),
    SUBTITLE("Caption Bar", 0xFFFFFFFF, 0xB3000000, false, false)
}

@Serializable
enum class TextAnimationType(val label: String) {
    NONE("Static"),
    FADE("Fade In"),
    POP("Pop Bounce"),
    SLIDE_UP("Slide Up"),
    TYPEWRITER("Typewriter")
}

@Serializable
data class ColorAdjustments(
    val brightness: Float = 0f,    // -0.5f .. 0.5f
    val contrast: Float = 0f,      // -0.5f .. 0.5f
    val saturation: Float = 1f,    // 0f .. 2f
    val warmth: Float = 0f,        // -0.5f .. 0.5f
    val vignette: Float = 0f       // 0f .. 1f
)

@Serializable
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val isPhoto: Boolean = false,
    val displayName: String,
    val totalDurationMs: Long = 4000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 4000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val zoomScale: Float = 1.0f,
    val transitionAfter: TransitionType = TransitionType.NONE
) {
    val effectiveDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(300L) / speed.coerceIn(0.25f, 4.0f)).toLong()
            .coerceAtLeast(300L)
}

@Serializable
data class AudioTrackItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String = "s.s Audio Lab",
    val uriString: String,
    val durationMs: Long = 16000L,
    val volume: Float = 0.85f,
    val fadeInMs: Long = 300L,
    val fadeOutMs: Long = 500L,
    val isMuted: Boolean = false
)

@Serializable
data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val stylePreset: TextStylePreset = TextStylePreset.CYBER_NEON,
    val colorHex: Long = 0xFFFFFFFF,
    val bgHex: Long = 0x99000000,
    val fontFamilyKey: String = "display",
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val normX: Float = 0.5f,
    val normY: Float = 0.72f,
    val scale: Float = 1.0f,
    val animationType: TextAnimationType = TextAnimationType.POP
)

@Serializable
data class StickerOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val stickerId: String,
    val label: String,
    val badgeText: String,
    val accentHex: Long,
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val normX: Float = 0.5f,
    val normY: Float = 0.25f,
    val scale: Float = 1.0f
)

@Serializable
data class PipOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val isPhoto: Boolean = true,
    val startMs: Long = 0L,
    val endMs: Long = 4000L,
    val normX: Float = 0.76f,
    val normY: Float = 0.24f,
    val scale: Float = 0.34f,
    val opacity: Float = 0.95f
)

@Serializable
data class ProjectData(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Edit",
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val fps: Int = 30,
    val clips: List<VideoClip> = emptyList(),
    val audioTrack: AudioTrackItem? = null,
    val isOriginalAudioMuted: Boolean = false,
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val stickerOverlays: List<StickerOverlayItem> = emptyList(),
    val pipOverlays: List<PipOverlayItem> = emptyList(),
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f,
    val effectPreset: VideoEffectPreset = VideoEffectPreset.NONE,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val canvasColorHex: Long = 0xFF0A0B0E,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }.coerceAtLeast(0L)
}

data class TemplateItem(
    val id: String,
    val title: String,
    val category: String,
    val clipCount: Int,
    val durationLabel: String,
    val aspectRatio: AspectRatioOption,
    val previewDrawableName: String,
    val filterPreset: FilterPreset,
    val effectPreset: VideoEffectPreset,
    val transitionType: TransitionType,
    val audioPresetId: String,
    val defaultCaption: String,
    val usesCountLabel: String
)

data class StickerCatalogItem(
    val id: String,
    val label: String,
    val badgeText: String,
    val accentColor: Color
)

enum class EditorToolTab(val label: String) {
    ROOT("Tools"),
    EDIT_CLIP("Edit"),
    AUDIO("Audio"),
    TEXT("Text"),
    STICKERS("Stickers"),
    OVERLAY("Overlay"),
    EFFECTS("Effects"),
    FILTERS("Filters"),
    ADJUST("Adjust"),
    RATIO("Ratio"),
    CANVAS("Canvas")
}

enum class MainNavTab(val label: String) {
    EDIT("Edit"),
    TEMPLATES("Templates"),
    EXPORTS("Exports")
}
