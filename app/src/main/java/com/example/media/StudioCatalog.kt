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
        "For You", "Beat Sync", "Cinematic", "Vlog", "Neon", "Travel"
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
            usesCountLabel = "184K uses"
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
            usesCountLabel = "142K uses"
        )
    )

    val stickers: List<StickerCatalogItem> = listOf(
        StickerCatalogItem("rec_live", "REC Live", "● REC", Color(0xFFFF2E88)),
        StickerCatalogItem("uhd_4k", "4K Cinema", "4K UHD", Color(0xFF00E5FF)),
        StickerCatalogItem("new_drop", "New Drop", "NEW", Color(0xFFF59E0B)),
        StickerCatalogItem("fire_vibe", "Trending", "🔥 VIBE", Color(0xFFFF6B35)),
        StickerCatalogItem("follow_cta", "Follow", "+ FOLLOW", Color(0xFF8B5CF6)),
        StickerCatalogItem("star_rating", "Highlight", "★ FEATURED", Color(0xFFFDE047)),
        StickerCatalogItem("beat_sync", "Beat Sync", "♪ SYNC", Color(0xFF10B981)),
        StickerCatalogItem("ss_badge", "Studio Stamp", "s.s PRO", Color(0xFF00E5FF))
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
