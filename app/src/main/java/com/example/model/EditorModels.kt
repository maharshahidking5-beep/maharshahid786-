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
enum class SafeZoneMode(val label: String) {
    NONE("Off"),
    TIKTOK("TikTok"),
    REELS("Reels"),
    SHORTS("Shorts")
}

@Serializable
enum class ExportResolution(
    val label: String,
    val scaleFactor: Float,
    val bitrateLabel: String,
    val targetBitrateBps: Int
) {
    RES_360P("360P", 0.45f, "3 Mbps · Saver", 3_000_000),
    RES_480P("480P", 0.56f, "5 Mbps", 5_000_000),
    RES_720P("720P", 0.67f, "10 Mbps", 10_000_000),
    RES_1080P("1080P", 1.0f, "18 Mbps", 18_000_000),
    RES_1080P_PLUS("1080P+", 1.18f, "26 Mbps · Ultra", 26_000_000),
    RES_2K("2K/4K", 1.5f, "38 Mbps · UHD", 38_000_000)
}

@Serializable
enum class FilterCategory(val label: String) {
    ALL("All"),
    A_ONE("A-One Pro"),
    CINEMA("Cinema"),
    MOOD("Mood"),
    VIBRANT("Vibrant"),
    FILM("Film")
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
    val blueScale: Float,
    val category: FilterCategory = FilterCategory.CINEMA
) {
    ORIGINAL("Original", 0xFF64748B, 0f, 0f, 1f, 1f, 1f, 1f, FilterCategory.ALL),
    A1_STUDIO("A1 Studio", 0xFF00E5FF, 0.04f, 0.24f, 1.24f, 1.06f, 1.04f, 1.08f, FilterCategory.A_ONE),
    A1_ULTRA_GLOW("A1 Ultra", 0xFFFF2E88, 0.06f, 0.22f, 1.30f, 1.14f, 1.02f, 1.05f, FilterCategory.A_ONE),
    A1_VELVET("A1 Velvet", 0xFFA855F7, -0.02f, 0.28f, 1.18f, 1.08f, 0.96f, 1.16f, FilterCategory.A_ONE),
    A1_CHROME("A1 Chrome", 0xFF38BDF8, 0.03f, 0.32f, 1.12f, 0.96f, 1.04f, 1.14f, FilterCategory.A_ONE),
    A1_SILK("A1 Silk", 0xFFFDA4AF, 0.08f, 0.08f, 1.06f, 1.09f, 1.02f, 0.97f, FilterCategory.A_ONE),
    TEAL_ORANGE("Teal & Orange", 0xFF00E5FF, 0.02f, 0.18f, 1.18f, 1.12f, 1.0f, 1.08f, FilterCategory.CINEMA),
    KODAK_GOLD("Kodak Gold", 0xFFF59E0B, 0.05f, 0.12f, 1.15f, 1.14f, 1.04f, 0.88f, FilterCategory.FILM),
    TOKYO_NIGHT("Tokyo Night", 0xFF8B5CF6, -0.04f, 0.24f, 1.25f, 0.94f, 0.98f, 1.18f, FilterCategory.MOOD),
    MOODY_DARK("Moody", 0xFF334155, -0.08f, 0.28f, 0.82f, 0.96f, 0.98f, 1.04f, FilterCategory.MOOD),
    PASTEL_POP("Pastel Pop", 0xFFFF2E88, 0.08f, -0.06f, 1.22f, 1.06f, 1.02f, 1.06f, FilterCategory.VIBRANT),
    CRISP_HDR("Crisp HDR", 0xFF10B981, 0.03f, 0.30f, 1.28f, 1.03f, 1.03f, 1.03f, FilterCategory.CINEMA),
    BW_NOIR("B&W Noir", 0xFFE2E8F0, 0.0f, 0.35f, 0.0f, 1f, 1f, 1f, FilterCategory.MOOD),
    CINEMA_AMBER("Amber 4K", 0xFFD97706, 0.03f, 0.22f, 1.16f, 1.18f, 1.02f, 0.84f, FilterCategory.CINEMA),
    CYBER_TEAL("Cyberpunk", 0xFF06B6D4, -0.02f, 0.26f, 1.32f, 0.88f, 1.06f, 1.22f, FilterCategory.VIBRANT),
    FUJI_PRO("Fuji Film", 0xFF34D399, 0.04f, 0.10f, 1.08f, 0.98f, 1.08f, 0.96f, FilterCategory.FILM),
    SUMMER_VIBE("Summer Pop", 0xFFFB7185, 0.06f, 0.14f, 1.35f, 1.10f, 1.04f, 0.94f, FilterCategory.VIBRANT),
    PARIS_SOFT("Parisian", 0xFFF472B6, 0.09f, -0.04f, 0.94f, 1.06f, 1.01f, 1.02f, FilterCategory.FILM),
    NIGHT_NEON("Neon Night", 0xFFA855F7, -0.05f, 0.30f, 1.38f, 1.12f, 0.90f, 1.24f, FilterCategory.MOOD),
    SUNSET_GLOW("Golden Hour", 0xFFFBBF24, 0.05f, 0.14f, 1.20f, 1.22f, 1.03f, 0.80f, FilterCategory.CINEMA),
    ICE_MINT("Arctic Mint", 0xFF67E8F9, 0.06f, 0.12f, 0.96f, 0.86f, 1.05f, 1.18f, FilterCategory.MOOD),
    EMERALD_CINE("Emerald 4K", 0xFF059669, 0.01f, 0.20f, 1.14f, 0.90f, 1.12f, 0.98f, FilterCategory.A_ONE),
    OBSIDIAN("Obsidian Pro", 0xFF1E293B, -0.06f, 0.34f, 0.90f, 0.98f, 1.00f, 1.08f, FilterCategory.A_ONE),
    MOCHA_WARM("Mocha Matte", 0xFF92400E, 0.04f, 0.06f, 0.95f, 1.16f, 1.02f, 0.86f, FilterCategory.FILM)
}

@Serializable
enum class EffectCategory(val label: String) {
    ALL("All"),
    TRENDING("Trending"),
    ZOOM_FX("Zoom & Split"),
    NIGHTCLUB("Nightclub"),
    RETRO("Retro"),
    LENS("Lens"),
    LIGHT("Light"),
    GLITCH("Glitch")
}

@Serializable
enum class VideoEffectPreset(
    val label: String,
    val tag: String,
    val category: EffectCategory = EffectCategory.TRENDING
) {
    NONE("None", "Off", EffectCategory.ALL),
    ZOOM_ON_BURST("Zoom On", "Pro", EffectCategory.ZOOM_FX),
    ZOOM_OFF_PULL("Zoom Off", "Pro", EffectCategory.ZOOM_FX),
    MIRROR_REFLECT("Reflected", "Mirror", EffectCategory.ZOOM_FX),
    SPLIT_4_GRID("4-Split Grid", "Split", EffectCategory.ZOOM_FX),
    NEON_GLOW("Neon Glow", "Cyber", EffectCategory.TRENDING),
    ZOOM_PULSE("Zoom Pulse", "Beat", EffectCategory.TRENDING),
    CHROMATIC_PULSE("Chromatic", "Prism", EffectCategory.TRENDING),
    HEARTBEAT_ZOOM("Heartbeat", "Pulse", EffectCategory.TRENDING),
    THERMAL("Thermal", "Heat", EffectCategory.TRENDING),
    DIAMOND_BLINK("Diamond", "Bling", EffectCategory.TRENDING),
    MOTION_BLUR("Motion Blur", "Speed", EffectCategory.LENS),
    SHAKE_BEAT("Camera Shake", "Shake", EffectCategory.NIGHTCLUB),
    STROBE_FLASH("Strobe Flash", "Club", EffectCategory.NIGHTCLUB),
    CLUB_LASER("Club Laser", "Beam", EffectCategory.NIGHTCLUB),
    BLACK_FLASH("Black Flash", "Flash", EffectCategory.NIGHTCLUB),
    VHS_RETRO("VHS Retro", "90s", EffectCategory.RETRO),
    DV_CAM_98("DV Cam 98", "REC", EffectCategory.RETRO),
    FILM_GRAIN("35mm Film", "Grain", EffectCategory.RETRO),
    SEPIA("Vintage Sepia", "Film", EffectCategory.RETRO),
    VIGNETTE_CINEMA("Cinema Vignette", "Pro", EffectCategory.LENS),
    ANAMORPHIC_BARS("Cinema Bars", "2.39:1", EffectCategory.LENS),
    FISHEYE_POP("Wide Pop", "Lens", EffectCategory.LENS),
    SOFT_HALO("Dream Halo", "Glow", EffectCategory.LIGHT),
    GOLDEN_SUNSET("Sun Flare", "Warm", EffectCategory.LIGHT),
    PRISM_RAINBOW("Prism Leak", "Leak", EffectCategory.LIGHT),
    SPARKLE_BLING("Bling Sparkle", "Stars", EffectCategory.LIGHT),
    GLITCH_RGB("RGB Split", "Glitch", EffectCategory.GLITCH),
    TV_STATIC("CRT Static", "CRT", EffectCategory.GLITCH),
    CYBER_SCAN("Cyber Scan", "HUD", EffectCategory.GLITCH)
}

@Serializable
enum class TransitionType(val label: String, val durationMs: Long) {
    NONE("None", 0L),
    FADE("Mix", 500L),
    BLACK_FADE("Black Fade", 550L),
    SLIDE("Slide Left", 450L),
    WIPE_RIGHT("Wipe Right", 450L),
    ZOOM("Zoom In", 500L),
    PULL_OUT("Pull Out", 500L),
    FLASH("White Flash", 350L),
    SPIN("Spin", 500L),
    SWIRL("Whip Pan", 450L),
    GLITCH_CUT("Glitch Cut", 380L),
    BURN("Light Burn", 480L)
}

@Serializable
enum class ClipAnimationType(val label: String, val tag: String) {
    NONE("None", "Off"),
    FADE_IN("Fade In", "In"),
    ZOOM_IN("Zoom On", "In"),
    ZOOM_OUT("Zoom Off", "Out"),
    PAN_LEFT("Pan Left", "Combo"),
    PAN_RIGHT("Pan Right", "Combo"),
    SLIDE_UP("Slide Up", "In"),
    BOUNCE("Pop Bounce", "Combo"),
    SPIN_IN("Spin In", "In"),
    SHAKE_POP("Shake Pop", "Combo")
}

@Serializable
enum class SpeedCurvePreset(val label: String, val effectiveMultiplier: Float, val points: List<Float>) {
    NONE("Normal", 1.0f, listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)),
    MONTAGE("Montage", 1.35f, listOf(0.3f, 0.8f, 0.4f, 0.9f, 0.35f)),
    HERO("Hero Moment", 0.75f, listOf(0.85f, 0.25f, 0.2f, 0.3f, 0.85f)),
    BULLET("Bullet", 1.65f, listOf(0.9f, 0.95f, 0.25f, 0.9f, 0.95f)),
    JUMPER("Jump Cut", 1.45f, listOf(0.4f, 0.95f, 0.45f, 0.95f, 0.45f)),
    FLASH_IN("Flash In", 1.25f, listOf(0.95f, 0.7f, 0.5f, 0.45f, 0.45f))
}

@Serializable
enum class VoiceEnhancePreset(
    val label: String,
    val tag: String,
    val clarityBoost: Float,
    val noiseReduction: Float,
    val loudnessGain: Float,
    val warmthBass: Float,
    val pitchShift: Float
) {
    NATURAL("Natural", "Off", 0f, 0f, 1.0f, 0f, 1.0f),
    STUDIO_VOICE("Studio Mic", "Pro", 0.72f, 0.65f, 1.35f, 0.25f, 1.0f),
    NOISE_CLEAN("De-Noise", "Clean", 0.45f, 0.92f, 1.25f, 0.0f, 1.0f),
    PODCAST_WARM("Podcast Warm", "Rich", 0.50f, 0.55f, 1.30f, 0.65f, 0.96f),
    CRISP_REEL("Crisp Reel", "Punch", 0.88f, 0.60f, 1.42f, 0.18f, 1.02f),
    DEEP_CINEMA("Deep Narrator", "Bass", 0.42f, 0.50f, 1.35f, 0.78f, 0.88f)
}

@Serializable
data class VoiceEnhanceSettings(
    val preset: VoiceEnhancePreset = VoiceEnhancePreset.NATURAL,
    val clarityBoost: Float = 0f,
    val noiseReduction: Float = 0f,
    val loudnessGain: Float = 1.0f,
    val warmthBass: Float = 0f,
    val pitchShift: Float = 1.0f
) {
    val isActive: Boolean
        get() = preset != VoiceEnhancePreset.NATURAL ||
            clarityBoost > 0.02f ||
            noiseReduction > 0.02f ||
            warmthBass > 0.02f ||
            kotlin.math.abs(loudnessGain - 1.0f) > 0.03f ||
            kotlin.math.abs(pitchShift - 1.0f) > 0.02f
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
    SUBTITLE("Caption Bar", 0xFFFFFFFF, 0xB3000000, false, false),
    TYPEWRITER_MONO("Mono Tag", 0xFF10B981, 0xDD06100E, false, false),
    HOLO_BLUE("Holo Blue", 0xFFE0F2FE, 0xCC1D4ED8, false, true),
    SUNSET_GLOW("Sunset Pop", 0xFFFFEDD5, 0xCCEA580C, false, false),
    STREET_BOLD("Street Bold", 0xFFFACC15, 0xE618181B, true, false)
}

@Serializable
enum class TextAnimationType(val label: String) {
    NONE("Static"),
    FADE("Fade In"),
    POP("Pop Bounce"),
    SLIDE_UP("Slide Up"),
    TYPEWRITER("Typewriter"),
    BOUNCE_IN("Elastic In"),
    NEON_FLICKER("Neon Pulse")
}

@Serializable
enum class PipShape(val label: String) {
    RECTANGLE("Rect"),
    ROUNDED("Rounded"),
    CIRCLE("Circle")
}

@Serializable
enum class CanvasStyle(val label: String) {
    SOLID("Solid"),
    NEON_GRADIENT("Neon Glow"),
    SUNSET_GRADIENT("Sunset"),
    BLUR_BACKDROP("Studio Blur")
}

@Serializable
enum class RemoveBgMode(val label: String) {
    NONE("Original"),
    AUTO_CUTOUT("Smart Cutout"),
    CHROMA_KEY("Chroma Key")
}

@Serializable
enum class AiRemoverMode(val label: String) {
    SMART_PATCH("Smart Patch"),
    SOFT_BLUR("Feathered Blur"),
    MOSAIC("Privacy Mosaic")
}

@Serializable
data class AiRemoverZone(
    val id: String = UUID.randomUUID().toString(),
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val normWidth: Float = 0.24f,
    val normHeight: Float = 0.14f,
    val mode: AiRemoverMode = AiRemoverMode.SMART_PATCH
)

@Serializable
data class ClipKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val localMs: Long = 0L,
    val zoomScale: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f
)

@Serializable
data class TimelineMarker(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val label: String = "Mark",
    val colorHex: Long = 0xFFF59E0B
)

@Serializable
data class VideoCoverConfig(
    val coverImageUri: String? = null,
    val coverTimestampMs: Long = 0L,
    val coverTitle: String = "",
    val coverBadge: String = "1080P+ PRO",
    val accentHex: Long = 0xFF00E5FF
)

@Serializable
data class ColorAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val clarity: Float = 0f,
    val fade: Float = 0f,
    val highlights: Float = 0f,
    val autoEnhanced: Boolean = false
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
    val freeRotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val zoomScale: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val transitionAfter: TransitionType = TransitionType.NONE,
    val animation: ClipAnimationType = ClipAnimationType.NONE,
    val animationDurationMs: Long = 750L,
    val speedCurve: SpeedCurvePreset = SpeedCurvePreset.NONE,
    val keyframes: List<ClipKeyframe> = emptyList(),
    val removeBgMode: RemoveBgMode = RemoveBgMode.NONE,
    val chromaColorHex: Long = 0xFF10B981,
    val chromaSensitivity: Float = 0.45f,
    val customBgHex: Long = 0xFF090D16,
    val aiRemoverZones: List<AiRemoverZone> = emptyList()
) {
    val effectiveDurationMs: Long
        get() = ((trimEndMs - trimStartMs).coerceAtLeast(300L) / speed.coerceIn(0.25f, 4.0f)).toLong()
            .coerceAtLeast(300L)

    val hasCrop: Boolean
        get() = cropLeft > 0.01f || cropTop > 0.01f || cropRight < 0.99f || cropBottom < 0.99f

    val totalRotationDegrees: Float
        get() = rotationDegrees.toFloat() + freeRotation
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
data class SfxOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val sfxId: String,
    val title: String,
    val uriString: String,
    val startMs: Long = 0L,
    val durationMs: Long = 1000L,
    val volume: Float = 0.95f
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
    val rotationDegrees: Float = 0f,
    val animationType: TextAnimationType = TextAnimationType.POP,
    val isCurved: Boolean = false,
    val is3D: Boolean = false,
    val isNeonGlow: Boolean = false,
    val isRtl: Boolean = false,
    val languageTag: String = "EN"
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
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f
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
    val opacity: Float = 0.95f,
    val rotationDegrees: Float = 0f,
    val shape: PipShape = PipShape.ROUNDED,
    val borderGlow: Boolean = true
)

@Serializable
data class ProjectData(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Edit",
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
    val resolution: ExportResolution = ExportResolution.RES_1080P_PLUS,
    val fps: Int = 60,
    val clips: List<VideoClip> = emptyList(),
    val audioTrack: AudioTrackItem? = null,
    val sfxOverlays: List<SfxOverlayItem> = emptyList(),
    val isOriginalAudioMuted: Boolean = false,
    val voiceEnhance: VoiceEnhanceSettings = VoiceEnhanceSettings(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val stickerOverlays: List<StickerOverlayItem> = emptyList(),
    val pipOverlays: List<PipOverlayItem> = emptyList(),
    val markers: List<TimelineMarker> = emptyList(),
    val coverConfig: VideoCoverConfig = VideoCoverConfig(),
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val filterIntensity: Float = 1.0f,
    val effectPreset: VideoEffectPreset = VideoEffectPreset.NONE,
    val effectIntensity: Float = 0.85f,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val canvasColorHex: Long = 0xFF0A0B0E,
    val canvasStyle: CanvasStyle = CanvasStyle.SOLID,
    val addWatermark: Boolean = false,
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
    val secondaryCaption: String = "",
    val badgeTag: String = "1080P+ READY",
    val speedPreset: Float = 1.0f,
    val autoEnhance: Boolean = true,
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
    TRANSFORM("Transform"),
    CROP("Crop"),
    REMOVE_BG("Remove BG"),
    AI_REMOVER("AI Remover"),
    EFFECTS("Effects"),
    FILTERS("Filters"),
    ANIMATE("Animate"),
    CAPTIONS("Captions"),
    AUDIO("Audio"),
    TEXT("Text"),
    STICKERS("Stickers"),
    OVERLAY("Overlay"),
    VOICE_FX("Voice FX"),
    AI_LAB("AI Lab"),
    ADJUST("Adjust"),
    RATIO("Ratio"),
    CANVAS("Canvas")
}

enum class MainNavTab(val label: String) {
    EDIT("Studio"),
    CANVA("Canva Pro"),
    FEED("TikTok"),
    TEMPLATES("Templates"),
    PROFILE("Profile")
}
