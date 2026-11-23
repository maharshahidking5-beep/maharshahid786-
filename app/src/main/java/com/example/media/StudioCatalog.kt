package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.model.AspectRatioOption
import com.example.model.FilterPreset
import com.example.model.StickerCatalogItem
import com.example.model.TemplateItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object StudioCatalog {

    val templateCategories = listOf(
        "For You", "Reels", "Beat Sync", "Cinematic", "AI Picks", "Vlog", "Neon", "Travel", "Gaming"
    )

    val templates: List<TemplateItem> = listOf(
        TemplateItem(
            id = "tpl_alpine_cinema",
            title = "Golden Alpine Horizon",
            category = "Cinematic",
            clipCount = 3,
            durationLabel = "00:12",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_cinematic",
            filterPreset = FilterPreset.TEAL_ORANGE,
            effectPreset = VideoEffectPreset.VIGNETTE_CINEMA,
            transitionType = TransitionType.FADE,
            audioPresetId = "cinema_rise",
            defaultCaption = "GOLDEN HOUR",
            secondaryCaption = "Shot in 1080P+ Ultra",
            badgeTag = "1080P+ HDR",
            speedPreset = 0.9f,
            autoEnhance = true,
            usesCountLabel = "248K uses"
        ),
        TemplateItem(
            id = "tpl_neon_velocity",
            title = "Cyber Beat Velocity",
            category = "Beat Sync",
            clipCount = 4,
            durationLabel = "00:14",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_neon",
            filterPreset = FilterPreset.TOKYO_NIGHT,
            effectPreset = VideoEffectPreset.NEON_GLOW,
            transitionType = TransitionType.FLASH,
            audioPresetId = "neon_pulse",
            defaultCaption = "NIGHT VELOCITY",
            secondaryCaption = "Beat Drop Sync",
            badgeTag = "BEAT SYNC",
            speedPreset = 1.25f,
            autoEnhance = true,
            usesCountLabel = "512K uses"
        ),
        TemplateItem(
            id = "tpl_morning_vlog",
            title = "Sunlit Morning Reel",
            category = "Vlog",
            clipCount = 3,
            durationLabel = "00:11",
            aspectRatio = AspectRatioOption.RATIO_4_5,
            previewDrawableName = "img_template_vlog",
            filterPreset = FilterPreset.KODAK_GOLD,
            effectPreset = VideoEffectPreset.NONE,
            transitionType = TransitionType.SLIDE,
            audioPresetId = "lofi_sunset",
            defaultCaption = "slow mornings",
            secondaryCaption = "Daily Aesthetic",
            badgeTag = "VOICE READY",
            speedPreset = 1.0f,
            autoEnhance = true,
            usesCountLabel = "184K uses"
        ),
        TemplateItem(
            id = "tpl_viral_reels_hook",
            title = "Viral 3-Sec Hook",
            category = "Reels",
            clipCount = 3,
            durationLabel = "00:10",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_studio_banner",
            filterPreset = FilterPreset.CRISP_HDR,
            effectPreset = VideoEffectPreset.ZOOM_PULSE,
            transitionType = TransitionType.ZOOM,
            audioPresetId = "trap_bounce",
            defaultCaption = "WAIT FOR IT...",
            secondaryCaption = "Watch the transformation",
            badgeTag = "VIRAL REEL",
            speedPreset = 1.15f,
            autoEnhance = true,
            usesCountLabel = "690K uses"
        ),
        TemplateItem(
            id = "tpl_ai_auto_grade",
            title = "AI Ultra Clarity Cut",
            category = "AI Picks",
            clipCount = 3,
            durationLabel = "00:12",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_cinematic",
            filterPreset = FilterPreset.CRISP_HDR,
            effectPreset = VideoEffectPreset.VIGNETTE_CINEMA,
            transitionType = TransitionType.FADE,
            audioPresetId = "cinema_rise",
            defaultCaption = "AI ENHANCED",
            secondaryCaption = "1080P+ Crisp Color",
            badgeTag = "AI GRADE",
            speedPreset = 1.0f,
            autoEnhance = true,
            usesCountLabel = "405K uses"
        ),
        TemplateItem(
            id = "tpl_prism_intro",
            title = "Prism Studio Intro",
            category = "Neon",
            clipCount = 3,
            durationLabel = "00:10",
            aspectRatio = AspectRatioOption.RATIO_16_9,
            previewDrawableName = "img_studio_banner",
            filterPreset = FilterPreset.CRISP_HDR,
            effectPreset = VideoEffectPreset.GLITCH_RGB,
            transitionType = TransitionType.ZOOM,
            audioPresetId = "trap_bounce",
            defaultCaption = "S.S STUDIO CUT",
            secondaryCaption = "60fps Smooth Intro",
            badgeTag = "1080P+ 60FPS",
            speedPreset = 1.1f,
            autoEnhance = true,
            usesCountLabel = "319K uses"
        ),
        TemplateItem(
            id = "tpl_noir_travel",
            title = "Moody Travel Cut",
            category = "Travel",
            clipCount = 3,
            durationLabel = "00:12",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_cinematic",
            filterPreset = FilterPreset.MOODY_DARK,
            effectPreset = VideoEffectPreset.VHS_RETRO,
            transitionType = TransitionType.FADE,
            audioPresetId = "morning_acoustic",
            defaultCaption = "WANDERLUST",
            secondaryCaption = "Miles away from home",
            badgeTag = "FILM LUT",
            speedPreset = 0.85f,
            autoEnhance = true,
            usesCountLabel = "142K uses"
        ),
        TemplateItem(
            id = "tpl_gaming_montage",
            title = "RGB Clutch Montage",
            category = "Gaming",
            clipCount = 4,
            durationLabel = "00:11",
            aspectRatio = AspectRatioOption.RATIO_16_9,
            previewDrawableName = "img_template_neon",
            filterPreset = FilterPreset.TOKYO_NIGHT,
            effectPreset = VideoEffectPreset.GLITCH_RGB,
            transitionType = TransitionType.SPIN,
            audioPresetId = "trap_bounce",
            defaultCaption = "CLUTCH MOMENT",
            secondaryCaption = "60fps High Octane",
            badgeTag = "60FPS PRO",
            speedPreset = 1.3f,
            autoEnhance = true,
            usesCountLabel = "428K uses"
        ),
        TemplateItem(
            id = "tpl_podcast_clip",
            title = "Podcast Studio Clip",
            category = "Reels",
            clipCount = 2,
            durationLabel = "00:15",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_vlog",
            filterPreset = FilterPreset.TEAL_ORANGE,
            effectPreset = VideoEffectPreset.NONE,
            transitionType = TransitionType.FADE,
            audioPresetId = "lofi_sunset",
            defaultCaption = "KEY TAKEAWAY",
            secondaryCaption = "Voice Enhanced",
            badgeTag = "VOICE CLEAR",
            speedPreset = 1.0f,
            autoEnhance = true,
            usesCountLabel = "290K uses"
        ),
        TemplateItem(
            id = "tpl_pastel_story",
            title = "Pastel Lifestyle Pop",
            category = "Vlog",
            clipCount = 3,
            durationLabel = "00:09",
            aspectRatio = AspectRatioOption.RATIO_3_4,
            previewDrawableName = "img_template_vlog",
            filterPreset = FilterPreset.PASTEL_POP,
            effectPreset = VideoEffectPreset.NONE,
            transitionType = TransitionType.SLIDE,
            audioPresetId = "morning_acoustic",
            defaultCaption = "WEEKEND VIBES",
            secondaryCaption = "Soft Film Look",
            badgeTag = "READYMADE",
            speedPreset = 1.0f,
            autoEnhance = true,
            usesCountLabel = "215K uses"
        ),
        TemplateItem(
            id = "tpl_cyber_phonk",
            title = "Phonk Speed Ramp",
            category = "Beat Sync",
            clipCount = 4,
            durationLabel = "00:10",
            aspectRatio = AspectRatioOption.RATIO_9_16,
            previewDrawableName = "img_template_neon",
            filterPreset = FilterPreset.MOODY_DARK,
            effectPreset = VideoEffectPreset.ZOOM_PULSE,
            transitionType = TransitionType.FLASH,
            audioPresetId = "neon_pulse",
            defaultCaption = "NO BRAKES",
            secondaryCaption = "128 BPM Velocity",
            badgeTag = "SPEED RAMP",
            speedPreset = 1.4f,
            autoEnhance = true,
            usesCountLabel = "611K uses"
        ),
        TemplateItem(
            id = "tpl_bw_editorial",
            title = "Noir Editorial Reel",
            category = "AI Picks",
            clipCount = 3,
            durationLabel = "00:12",
            aspectRatio = AspectRatioOption.RATIO_4_5,
            previewDrawableName = "img_template_cinematic",
            filterPreset = FilterPreset.BW_NOIR,
            effectPreset = VideoEffectPreset.VIGNETTE_CINEMA,
            transitionType = TransitionType.FADE,
            audioPresetId = "cinema_rise",
            defaultCaption = "TIMELESS",
            secondaryCaption = "Monochrome 1080P+",
            badgeTag = "CLASSIC",
            speedPreset = 0.9f,
            autoEnhance = false,
            usesCountLabel = "178K uses"
        )
    )

    val stickers: List<StickerCatalogItem> = listOf(
        StickerCatalogItem("rec_live", "REC Live", "● REC", Color(0xFFFF2E88)),
        StickerCatalogItem("uhd_4k", "1080P+ HD", "1080P+ HD", Color(0xFF00E5FF)),
        StickerCatalogItem("new_drop", "New Drop", "NEW", Color(0xFFF59E0B)),
        StickerCatalogItem("fire_vibe", "Trending", "🔥 VIBE", Color(0xFFFF6B35)),
        StickerCatalogItem("follow_cta", "Follow", "+ FOLLOW", Color(0xFF8B5CF6)),
        StickerCatalogItem("star_rating", "Highlight", "★ FEATURED", Color(0xFFFDE047)),
        StickerCatalogItem("beat_sync", "Beat Sync", "♪ SYNC", Color(0xFF10B981)),
        StickerCatalogItem("ss_badge", "Studio Stamp", "s.s PRO", Color(0xFF00E5FF)),
        StickerCatalogItem("sub_cta", "Subscribe", "SUBSCRIBE", Color(0xFFEF4444)),
        StickerCatalogItem("wait_for_it", "Hook Tag", "WAIT FOR IT", Color(0xFFEC4899)),
        StickerCatalogItem("real_100", "Authentic", "100% REAL", Color(0xFF22C55E)),
        StickerCatalogItem("top_pick", "Top Pick", "TOP PICK", Color(0xFFF97316)),
        StickerCatalogItem("cinema_239", "Cinema", "2.39:1 CINE", Color(0xFF0EA5E9)),
        StickerCatalogItem("beat_drop", "Beat Drop", "⚡ DROP", Color(0xFFA855F7)),
        StickerCatalogItem("part_two", "Part 2", "PART 2 →", Color(0xFF14B8A6)),
        StickerCatalogItem("link_bio", "Link Tag", "LINK IN BIO", Color(0xFFE11D48))
    )

    val canvasColors: List<Long> = listOf(
        0xFF0A0B0E,
        0xFF000000,
        0xFFFFFFFF,
        0xFF0F172A,
        0xFF1E1B4B,
        0xFF3B0764,
        0xFF042F2E,
        0xFF450A0A
    )

    fun drawableResForName(name: String): Int {
        return when (name) {
            "img_template_cinematic" -> R.drawable.img_template_cinematic
            "img_template_neon" -> R.drawable.img_template_neon
            "img_template_vlog" -> R.drawable.img_template_vlog
            "img_studio_banner" -> R.drawable.img_studio_banner
            else -> R.drawable.img_template_cinematic
        }
    }

    suspend fun createSampleClipsForProject(
        context: Context,
        primaryDrawableName: String = "img_template_cinematic"
    ): List<VideoClip> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "sample_media").apply { mkdirs() }
        val sources = listOf(
            primaryDrawableName to "Scene 01",
            "img_template_neon" to "Scene 02",
            "img_template_vlog" to "Scene 03"
        ).distinctBy { it.first }

        sources.mapIndexed { idx, (resName, label) ->
            val file = File(dir, "$resName.jpg")
            if (!file.exists()) {
                val bmp = BitmapFactory.decodeResource(context.resources, drawableResForName(resName))
                if (bmp != null) {
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    bmp.recycle()
                }
            }
            VideoClip(
                uriString = Uri.fromFile(file).toString(),
                isPhoto = true,
                displayName = label,
                totalDurationMs = 4000L,
                trimStartMs = 0L,
                trimEndMs = 4000L,
                transitionAfter = if (idx < sources.lastIndex) TransitionType.FADE else TransitionType.NONE
            )
        }
    }
}
