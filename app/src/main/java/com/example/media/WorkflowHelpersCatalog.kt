package com.example.media

import com.example.model.AspectRatioOption
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.FilterPreset
import com.example.model.VideoEffectPreset
import com.example.model.VoiceEnhancePreset

data class QuickPresetPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val accentHex: Long,
    val filterPreset: FilterPreset,
    val effectPreset: VideoEffectPreset,
    val adjustments: ColorAdjustments,
    val voicePreset: VoiceEnhancePreset,
    val audioPresetId: String,
    val aspectRatio: AspectRatioOption,
    val defaultCaption: String
)

data class MiniGuideItem(
    val id: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val targetTab: EditorToolTab? = null,
    val assistantPrompt: String
)

data class BilingualPhraseSample(
    val languageTag: String,
    val label: String,
    val text: String,
    val fontKey: String,
    val isRtl: Boolean
)

object WorkflowHelpersCatalog {

    val quickPresetPacks: List<QuickPresetPack> = listOf(
        QuickPresetPack(
            id = "pack_vlog_ready",
            title = "Vlog Ready",
            subtitle = "Warm film grade + Studio Mic voice + 1080P+ clarity",
            badge = "DAILY VLOG",
            accentHex = 0xFF00E5FF,
            filterPreset = FilterPreset.KODAK_GOLD,
            effectPreset = VideoEffectPreset.NONE,
            adjustments = ColorAdjustments(
                brightness = 0.04f,
                contrast = 0.14f,
                saturation = 1.14f,
                warmth = 0.12f,
                clarity = 0.55f,
                autoEnhanced = true
            ),
            voicePreset = VoiceEnhancePreset.STUDIO_VOICE,
            audioPresetId = "lofi_sunset",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            defaultCaption = "DAILY VLOG"
        ),
        QuickPresetPack(
            id = "pack_wedding_glow",
            title = "Wedding Glow",
            subtitle = "Dreamy silk tones + soft halo + warm highlights",
            badge = "ROMANTIC",
            accentHex = 0xFFFF2E88,
            filterPreset = FilterPreset.A1_SILK,
            effectPreset = VideoEffectPreset.SOFT_HALO,
            adjustments = ColorAdjustments(
                brightness = 0.06f,
                contrast = 0.08f,
                saturation = 1.08f,
                warmth = 0.18f,
                clarity = 0.35f,
                highlights = 0.14f,
                autoEnhanced = true
            ),
            voicePreset = VoiceEnhancePreset.PODCAST_WARM,
            audioPresetId = "morning_acoustic",
            aspectRatio = AspectRatioOption.RATIO_4_5,
            defaultCaption = "Forever Moments"
        ),
        QuickPresetPack(
            id = "pack_gaming_punch",
            title = "Gaming Punch",
            subtitle = "High-contrast neon + RGB energy + punchy voice",
            badge = "60FPS HYPR",
            accentHex = 0xFF8B5CF6,
            filterPreset = FilterPreset.TOKYO_NIGHT,
            effectPreset = VideoEffectPreset.ZOOM_PULSE,
            adjustments = ColorAdjustments(
                brightness = 0.02f,
                contrast = 0.28f,
                saturation = 1.32f,
                warmth = -0.08f,
                clarity = 0.72f,
                autoEnhanced = true
            ),
            voicePreset = VoiceEnhancePreset.CRISP_REEL,
            audioPresetId = "trap_bounce",
            aspectRatio = AspectRatioOption.RATIO_16_9,
            defaultCaption = "CLUTCH MODE"
        ),
        QuickPresetPack(
            id = "pack_product_ad",
            title = "Product Ad",
            subtitle = "Ultra sharp studio contrast + clean commercial pop",
            badge = "BRAND AD",
            accentHex = 0xFF10B981,
            filterPreset = FilterPreset.A1_STUDIO,
            effectPreset = VideoEffectPreset.VIGNETTE_CINEMA,
            adjustments = ColorAdjustments(
                brightness = 0.03f,
                contrast = 0.24f,
                saturation = 1.22f,
                warmth = 0.02f,
                clarity = 0.85f,
                vignette = 0.18f,
                autoEnhanced = true
            ),
            voicePreset = VoiceEnhancePreset.NOISE_CLEAN,
            audioPresetId = "neon_pulse",
            aspectRatio = AspectRatioOption.RATIO_1_1,
            defaultCaption = "NEW DROP · 100% PRO"
        )
    )

    val miniGuides: List<MiniGuideItem> = listOf(
        MiniGuideItem(
            id = "guide_remove_bg",
            title = "Remove Video Background",
            summary = "Isolate people or green screens in two taps.",
            steps = listOf(
                "Select your clip on the timeline.",
                "Tap Remove BG in the bottom tool dock.",
                "Choose Smart Cutout or Chroma Key and pick a backdrop color or slider feather."
            ),
            targetTab = EditorToolTab.REMOVE_BG,
            assistantPrompt = "Give me quick tips to get the cleanest background cutout or green screen removal."
        ),
        MiniGuideItem(
            id = "guide_shaky_clips",
            title = "Fix Shaky Clips & Smooth Framing",
            summary = "Keep the main subject steady and framed for any social app.",
            steps = listOf(
                "Open Ratio and tap Smart Auto-Crop Subject to lock onto the person or focal area.",
                "Open Transform to slightly zoom in (1.08x) and use Keyframes for smooth camera glides.",
                "Avoid Shake Beat FX and pick Smooth Fade or Mix transitions between clips."
            ),
            targetTab = EditorToolTab.RATIO,
            assistantPrompt = "How can I make shaky handheld footage look smoother and keep the subject centered?"
        ),
        MiniGuideItem(
            id = "guide_4k_export",
            title = "Why 2K/4K Export Takes Longer",
            summary = "Understand resolution tradeoffs and speed up exports.",
            steps = listOf(
                "2K/4K renders up to 38 Mbps hardware-encoded frames with full overlay compositing.",
                "Switch to 1080P+ (26 Mbps) for Reels/TikTok — nearly identical sharpness at twice the speed.",
                "Enable Battery Saver Mode in Settings to reduce live preview load on low-end devices."
            ),
            targetTab = null,
            assistantPrompt = "Why does 4K/2K video export take longer and what settings should I pick for TikTok and YouTube?"
        ),
        MiniGuideItem(
            id = "guide_bilingual_text",
            title = "Urdu, Hindi & 3D Neon Text",
            summary = "Write RTL Urdu Nastaliq, Hindi Devanagari, and curved titles.",
            steps = listOf(
                "Open Text -> Add text and pick UR (اردو), HI (हिंदी), or EN.",
                "Choose Nastaliq Urdu, Hindi Devanagari, Brush Script, or Poster 3D fonts.",
                "Toggle Curved, Neon Glow, or 3D Shadow pills and drag/pinch text right on the preview."
            ),
            targetTab = EditorToolTab.TEXT,
            assistantPrompt = "How do I style Urdu Nastaliq or Hindi titles with 3D shadow and curved arc text?"
        ),
        MiniGuideItem(
            id = "guide_beat_sync",
            title = "Auto-Beat Marker & Rhythm Cut",
            summary = "Sync clips cleanly to music beats without manual counting.",
            steps = listOf(
                "Open Audio in the editor tool dock and pick a music track or import your own.",
                "Tap Add Beat Marks to drop timed rhythm markers across the timeline.",
                "Tap Rhythm Cut Clips to automatically trim clips to match the beat!"
            ),
            targetTab = EditorToolTab.AUDIO,
            assistantPrompt = "Explain how to make a viral beat-synced edit using Auto-Beat Markers and Rhythm Cut."
        ),
        MiniGuideItem(
            id = "guide_safe_zone",
            title = "TikTok, Reels & Shorts Safe Zones",
            summary = "Keep captions away from app buttons and bottom descriptions.",
            steps = listOf(
                "Tap the Safe-Zone badge above the timeline to cycle TikTok, Reels, and Shorts guides.",
                "Keep all important faces and captions inside the glowing center frame.",
                "Tap again to turn the guide off — safe-zone lines never appear in your exported MP4."
            ),
            targetTab = EditorToolTab.RATIO,
            assistantPrompt = "Where should I place captions so TikTok and Reels UI icons don't cover them?"
        )
    )

    val bilingualStarterPhrases: List<BilingualPhraseSample> = listOf(
        BilingualPhraseSample("EN", "Viral Hook", "WATCH UNTIL THE END", "display", false),
        BilingualPhraseSample("EN", "Pro Title", "S.S STUDIO PRO", "bold3d", false),
        BilingualPhraseSample("UR", "اردو سلام", "خوش آمدید · نیا ولاگ", "nastaliq", true),
        BilingualPhraseSample("UR", "اردو کیپشن", "آخر تک ضرور دیکھیں", "nastaliq", true),
        BilingualPhraseSample("UR", "شکریہ", "سبسکرائب اور لائک کریں", "nastaliq", true),
        BilingualPhraseSample("HI", "हिंदी स्वागत", "नया व्लॉग · स्वागत है", "hindi", false),
        BilingualPhraseSample("HI", "हिंदी हुक", "अंत तक ज़रूर देखें", "hindi", false),
        BilingualPhraseSample("HI", "सब्सक्राइब", "लाइक और शेयर करें", "hindi", false)
    )
}
