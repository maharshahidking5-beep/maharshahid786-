package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.Serializable
import java.util.UUID

// ==========================================
// 1. FIRESTORE ONLINE MODELS (Auth + Social + Cloud Sync)
// ==========================================

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val handle: String = "@creator",
    val bio: String = "Editing with s.s Studio Pro",
    val photoUrl: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateAccountDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudProjectSummaryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val aspectLabel: String = "9:16",
    val resolutionLabel: String = "1080P+",
    val clipCount: Long = 0L,
    val totalDurationMs: Long = 0L,
    val filterLabel: String = "Original",
    val projectJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CanvaDesignDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Poster",
    val aspectLabel: String = "3:4",
    val bgHex: Long = 0xFF0E1420,
    val accentHex: Long = 0xFF00E5FF,
    val headline: String = "",
    val designJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialVideoPostDoc(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatarUrl: String = "",
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val soundTitle: String = "Original Sound - s.s Studio",
    val mediaType: String = "TEMPLATE_REEL", // VIDEO | PHOTO | CANVA_DESIGN | TEMPLATE_REEL
    val mediaUrl: String = "",
    val accentHex: Long = 0xFF00E5FF,
    val filterLabel: String = "A1 Studio",
    val templateId: String = "",
    val visibility: String = "PUBLIC",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val savesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostCommentDoc(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val text: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// ==========================================
// 2. CANVA PRO DESIGN STUDIO MODELS
// ==========================================

@Serializable
enum class CanvaElementType {
    TEXT,
    SHAPE,
    BADGE,
    IMAGE
}

@Serializable
enum class CanvaShapeKind(val label: String) {
    CIRCLE("Circle"),
    ROUNDED_RECT("Card"),
    PILL("Pill"),
    DIAMOND("Diamond"),
    OUTLINE_FRAME("Frame")
}

@Serializable
enum class CanvaBackgroundStyle(val label: String) {
    SOLID("Solid"),
    GRADIENT_DIAGONAL("Gradient"),
    NEON_MESH("Neon Mesh"),
    GRID_STUDIO("Grid Pro")
}

@Serializable
enum class CanvaAspectOption(val label: String, val wRatio: Float, val hRatio: Float) {
    SQUARE_1_1("1:1 Post", 1f, 1f),
    STORY_9_16("9:16 Story", 9f, 16f),
    POSTER_3_4("3:4 Poster", 3f, 4f),
    PORTRAIT_4_5("4:5 Portrait", 4f, 5f),
    BANNER_16_9("16:9 Thumb", 16f, 9f)
}

@Serializable
data class CanvaElementItem(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvaElementType = CanvaElementType.TEXT,
    val text: String = "NEW DESIGN",
    val shapeKind: CanvaShapeKind = CanvaShapeKind.ROUNDED_RECT,
    val imageUri: String = "",
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val widthFrac: Float = 0.58f,
    val heightFrac: Float = 0.16f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val colorHex: Long = 0xFF00E5FF,
    val textColorHex: Long = 0xFFFFFFFF,
    val fontSizeSp: Float = 24f,
    val opacity: Float = 1f,
    val bold: Boolean = true,
    val cornerRadiusDp: Float = 16f
)

@Serializable
data class CanvaProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Design",
    val category: String = "Poster",
    val aspectOption: CanvaAspectOption = CanvaAspectOption.POSTER_3_4,
    val bgStyle: CanvaBackgroundStyle = CanvaBackgroundStyle.GRADIENT_DIAGONAL,
    val bgColorHex: Long = 0xFF0B1120,
    val gradientEndHex: Long = 0xFF280D36,
    val elements: List<CanvaElementItem> = emptyList()
)

data class CanvaBrandKit(
    val id: String,
    val name: String,
    val bgHex: Long,
    val gradientEndHex: Long,
    val primaryAccentHex: Long,
    val secondaryAccentHex: Long,
    val headlineColorHex: Long
)

data class CanvaTemplatePreset(
    val id: String,
    val title: String,
    val category: String,
    val project: CanvaProject
)

object CanvaProCatalog {
    val brandKits = listOf(
        CanvaBrandKit("cyber", "Cyber Pro", 0xFF070C17, 0xFF191138, 0xFF00E5FF, 0xFFFF3B6B, 0xFFFFFFFF),
        CanvaBrandKit("gold", "Luxury Gold", 0xFF100E0A, 0xFF2B2110, 0xFFF59E0B, 0xFFFDE68A, 0xFFFFFBEB),
        CanvaBrandKit("editorial", "Clean Ink", 0xFFF8FAFC, 0xFFE2E8F0, 0xFF0F172A, 0xFF3B82F6, 0xFF0F172A),
        CanvaBrandKit("sunset", "Sunset Pop", 0xFF2A0B1E, 0xFF7C2D12, 0xFFFB7185, 0xFFFBBF24, 0xFFFFFFFF),
        CanvaBrandKit("emerald", "Emerald Bio", 0xFF051B14, 0xFF0F3D2E, 0xFF10B981, 0xFF34D399, 0xFFECFDF5)
    )

    val templates: List<CanvaTemplatePreset> = listOf(
        CanvaTemplatePreset(
            id = "canva_yt_viral",
            title = "Viral YouTube Thumb",
            category = "Thumbnail",
            project = CanvaProject(
                title = "Viral YouTube Thumb",
                category = "Thumbnail",
                aspectOption = CanvaAspectOption.BANNER_16_9,
                bgStyle = CanvaBackgroundStyle.NEON_MESH,
                bgColorHex = 0xFF090D1C,
                gradientEndHex = 0xFF3B0764,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.OUTLINE_FRAME, normX = 0.5f, normY = 0.5f, widthFrac = 0.9f, heightFrac = 0.84f, colorHex = 0xFF00E5FF, opacity = 0.7f),
                    CanvaElementItem(type = CanvaElementType.BADGE, text = "1080P+ PRO", normX = 0.22f, normY = 0.22f, colorHex = 0xFFFF3B6B, textColorHex = 0xFFFFFFFF, fontSizeSp = 13f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "EDIT LIKE A PRO", normX = 0.5f, normY = 0.48f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 30f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "CAPCUT + CANVA WORKFLOW", normX = 0.5f, normY = 0.68f, colorHex = 0xFF00E5FF, textColorHex = 0xFF041418, fontSizeSp = 14f)
                )
            )
        ),
        CanvaTemplatePreset(
            id = "canva_tiktok_cover",
            title = "TikTok Reel Cover",
            category = "Story",
            project = CanvaProject(
                title = "TikTok Reel Cover",
                category = "Story",
                aspectOption = CanvaAspectOption.STORY_9_16,
                bgStyle = CanvaBackgroundStyle.GRADIENT_DIAGONAL,
                bgColorHex = 0xFF05131F,
                gradientEndHex = 0xFF31102F,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.CIRCLE, normX = 0.5f, normY = 0.34f, widthFrac = 0.54f, heightFrac = 0.30f, colorHex = 0xFF00E5FF, opacity = 0.22f),
                    CanvaElementItem(type = CanvaElementType.BADGE, text = "TRENDING #1", normX = 0.5f, normY = 0.22f, colorHex = 0xFFF59E0B, textColorHex = 0xFF041418, fontSizeSp = 13f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "CINEMATIC\nDAY IN MY LIFE", normX = 0.5f, normY = 0.50f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 28f),
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.PILL, normX = 0.5f, normY = 0.72f, widthFrac = 0.58f, heightFrac = 0.08f, colorHex = 0xFFFF3B6B, opacity = 0.92f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "WATCH FULL REEL →", normX = 0.5f, normY = 0.72f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 14f)
                )
            )
        ),
        CanvaTemplatePreset(
            id = "canva_promo_poster",
            title = "Flash Sale Poster",
            category = "Poster",
            project = CanvaProject(
                title = "Flash Sale Poster",
                category = "Poster",
                aspectOption = CanvaAspectOption.POSTER_3_4,
                bgStyle = CanvaBackgroundStyle.GRADIENT_DIAGONAL,
                bgColorHex = 0xFF1F0A1D,
                gradientEndHex = 0xFF6B1D2F,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.BADGE, text = "LIMITED DROP", normX = 0.5f, normY = 0.2f, colorHex = 0xFFFBBF24, textColorHex = 0xFF180A04, fontSizeSp = 13f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "MEGA CREATOR\n50% OFF", normX = 0.5f, normY = 0.45f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 30f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "STUDIO COLLECTION 2026", normX = 0.5f, normY = 0.66f, colorHex = 0xFF10B981, textColorHex = 0xFF041418, fontSizeSp = 14f)
                )
            )
        ),
        CanvaTemplatePreset(
            id = "canva_brand_logo",
            title = "Pro Brand Badge",
            category = "Logo",
            project = CanvaProject(
                title = "Pro Brand Badge",
                category = "Logo",
                aspectOption = CanvaAspectOption.SQUARE_1_1,
                bgStyle = CanvaBackgroundStyle.GRID_STUDIO,
                bgColorHex = 0xFF090D16,
                gradientEndHex = 0xFF111827,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.CIRCLE, normX = 0.5f, normY = 0.45f, widthFrac = 0.56f, heightFrac = 0.56f, colorHex = 0xFF8B5CF6, opacity = 0.35f),
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.DIAMOND, normX = 0.5f, normY = 0.45f, widthFrac = 0.42f, heightFrac = 0.42f, colorHex = 0xFF00E5FF, opacity = 0.85f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "S.S", normX = 0.5f, normY = 0.45f, colorHex = 0x00000000, textColorHex = 0xFF041418, fontSizeSp = 34f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "CREATIVE STUDIO", normX = 0.5f, normY = 0.80f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 15f)
                )
            )
        ),
        CanvaTemplatePreset(
            id = "canva_quote_card",
            title = "Editorial Quote Card",
            category = "Post",
            project = CanvaProject(
                title = "Editorial Quote Card",
                category = "Post",
                aspectOption = CanvaAspectOption.PORTRAIT_4_5,
                bgStyle = CanvaBackgroundStyle.SOLID,
                bgColorHex = 0xFFF8FAFC,
                gradientEndHex = 0xFFE2E8F0,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.OUTLINE_FRAME, normX = 0.5f, normY = 0.5f, widthFrac = 0.84f, heightFrac = 0.84f, colorHex = 0xFF0F172A, opacity = 0.85f),
                    CanvaElementItem(type = CanvaElementType.BADGE, text = "DAILY MINDSET", normX = 0.5f, normY = 0.24f, colorHex = 0xFF0F172A, textColorHex = 0xFFFFFFFF, fontSizeSp = 12f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "CREATE\nWITHOUT\nLIMITS.", normX = 0.5f, normY = 0.50f, colorHex = 0x00000000, textColorHex = 0xFF0F172A, fontSizeSp = 30f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "@ss_studio_pro", normX = 0.5f, normY = 0.77f, colorHex = 0x00000000, textColorHex = 0xFF3B82F6, fontSizeSp = 14f)
                )
            )
        ),
        CanvaTemplatePreset(
            id = "canva_podcast_art",
            title = "Podcast & Music Art",
            category = "Logo",
            project = CanvaProject(
                title = "Podcast & Music Art",
                category = "Logo",
                aspectOption = CanvaAspectOption.SQUARE_1_1,
                bgStyle = CanvaBackgroundStyle.NEON_MESH,
                bgColorHex = 0xFF071B16,
                gradientEndHex = 0xFF0A3A2E,
                elements = listOf(
                    CanvaElementItem(type = CanvaElementType.SHAPE, shapeKind = CanvaShapeKind.ROUNDED_RECT, normX = 0.5f, normY = 0.5f, widthFrac = 0.78f, heightFrac = 0.52f, colorHex = 0xFF10B981, opacity = 0.25f),
                    CanvaElementItem(type = CanvaElementType.BADGE, text = "NEW EPISODE", normX = 0.5f, normY = 0.30f, colorHex = 0xFF10B981, textColorHex = 0xFF041418, fontSizeSp = 13f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "CREATOR\nSESSIONS", normX = 0.5f, normY = 0.52f, colorHex = 0x00000000, textColorHex = 0xFFFFFFFF, fontSizeSp = 28f),
                    CanvaElementItem(type = CanvaElementType.TEXT, text = "VOICE ENHANCED AUDIO", normX = 0.5f, normY = 0.73f, colorHex = 0x00000000, textColorHex = 0xFFA7F3D0, fontSizeSp = 13f)
                )
            )
        )
    )
}
