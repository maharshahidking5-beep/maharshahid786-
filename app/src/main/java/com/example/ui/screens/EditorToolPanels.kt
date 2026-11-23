package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiCaptionPack
import com.example.ai.AiEnhanceRecommendation
import com.example.ai.AiLabUiState
import com.example.ai.AiTranslatedCaptionPack
import com.example.media.AudioSynthLibrary
import com.example.media.StudioCatalog
import com.example.media.WorkflowHelpersCatalog
import com.example.model.AiRemoverMode
import com.example.model.AiRemoverZone
import com.example.model.AspectRatioOption
import com.example.model.CanvasStyle
import com.example.model.ClipAnimationType
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.FilterPreset
import com.example.model.PipOverlayItem
import com.example.model.PipShape
import com.example.model.ProjectData
import com.example.model.RemoveBgMode
import com.example.model.SpeedCurvePreset
import com.example.model.StickerCatalogItem
import com.example.model.StickerOverlayItem
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.model.VoiceEnhancePreset
import com.example.model.VoiceEnhanceSettings
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioFontCatalog
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

private data class ToolDockItem(
    val tab: EditorToolTab,
    val label: String,
    val icon: ImageVector
)

private val rootDockItems = listOf(
    ToolDockItem(EditorToolTab.EDIT_CLIP, "Edit", Icons.Filled.ContentCut),
    ToolDockItem(EditorToolTab.TRANSFORM, "Transform", Icons.Filled.Transform),
    ToolDockItem(EditorToolTab.CROP, "Crop", Icons.Filled.Crop),
    ToolDockItem(EditorToolTab.EFFECTS, "Effects", Icons.Filled.AutoFixHigh),
    ToolDockItem(EditorToolTab.FILTERS, "Filters", Icons.Filled.FilterVintage),
    ToolDockItem(EditorToolTab.ANIMATE, "Animate", Icons.Filled.Animation),
    ToolDockItem(EditorToolTab.REMOVE_BG, "Remove BG", Icons.Filled.LayersClear),
    ToolDockItem(EditorToolTab.AI_REMOVER, "AI Eraser", Icons.Filled.CleaningServices),
    ToolDockItem(EditorToolTab.CAPTIONS, "Translate", Icons.Filled.Translate),
    ToolDockItem(EditorToolTab.AUDIO, "Audio", Icons.Filled.MusicNote),
    ToolDockItem(EditorToolTab.TEXT, "Text", Icons.Filled.Title),
    ToolDockItem(EditorToolTab.STICKERS, "Stickers", Icons.Filled.EmojiEmotions),
    ToolDockItem(EditorToolTab.OVERLAY, "Overlay", Icons.Filled.Layers),
    ToolDockItem(EditorToolTab.VOICE_FX, "Voice FX", Icons.Filled.GraphicEq),
    ToolDockItem(EditorToolTab.AI_LAB, "AI Lab", Icons.Filled.AutoAwesome),
    ToolDockItem(EditorToolTab.ADJUST, "Adjust", Icons.Filled.Tune),
    ToolDockItem(EditorToolTab.RATIO, "Ratio", Icons.Filled.AspectRatio),
    ToolDockItem(EditorToolTab.CANVAS, "Canvas", Icons.Filled.Palette)
)

@Composable
fun EditorBottomToolbar(
    activeTab: EditorToolTab,
    project: ProjectData,
    aiLabState: AiLabUiState,
    selectedClip: VideoClip?,
    selectedText: TextOverlayItem?,
    selectedSticker: StickerOverlayItem?,
    selectedPip: PipOverlayItem?,
    selectedRemoverZoneId: String?,
    onSelectTab: (EditorToolTab) -> Unit,
    onToggleAutoEnhance: () -> Unit,
    onRunAiVideoEnhance: () -> Unit,
    onApplyAiEnhance: (AiEnhanceRecommendation) -> Unit,
    onRunAiCaptions: (String) -> Unit,
    onApplyAiCaptions: (AiCaptionPack) -> Unit,
    onStreamAiScript: (String, String) -> Unit,
    onTranslateVideo: (String, String) -> Unit,
    onApplyTranslation: (AiTranslatedCaptionPack) -> Unit,
    onResetAiState: () -> Unit,
    onSelectVoicePreset: (VoiceEnhancePreset) -> Unit,
    onUpdateVoiceSettings: (VoiceEnhanceSettings) -> Unit,
    onSplitClip: () -> Unit,
    onFreezeFrame: () -> Unit,
    onReplaceClipClick: () -> Unit,
    onUpdateTrim: (String, Long, Long) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateSpeedCurve: (String, SpeedCurvePreset) -> Unit,
    onUpdateClipAnimation: (String, ClipAnimationType, Long) -> Unit,
    onUpdateClipVolume: (String, Float) -> Unit,
    onRotateClip: (String) -> Unit,
    onFlipClip: (String) -> Unit,
    onFlipClipVertical: (String) -> Unit,
    onUpdateClipZoom: (String, Float) -> Unit,
    onUpdateClipTransform: (String, Float, Float, Float, Float) -> Unit,
    onResetClipTransform: (String) -> Unit,
    onApplyZoomOnOff: (String, Boolean) -> Unit,
    onToggleKeyframe: () -> Unit,
    onUpdateClipCrop: (String, Float, Float, Float, Float) -> Unit,
    onResetClipCrop: (String) -> Unit,
    onUpdateRemoveBg: (String, RemoveBgMode, Long?, Float?, Long?) -> Unit,
    onAddRemoverZone: (String, AiRemoverMode) -> Unit,
    onUpdateRemoverZone: (String, AiRemoverZone) -> Unit,
    onDeleteRemoverZone: (String, String) -> Unit,
    onMoveClip: (String, Int) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onSelectMusicPreset: (String) -> Unit,
    onImportAudioClick: () -> Unit,
    onUpdateAudioVolume: (Float) -> Unit,
    onToggleAudioMute: () -> Unit,
    onToggleAudioFades: (Boolean) -> Unit,
    onRemoveAudio: () -> Unit,
    onAddSfx: (String) -> Unit,
    onDeleteSfx: (String) -> Unit,
    onAutoBeatMarkers: (Int, Boolean) -> Unit,
    onAddText: (String, TextStylePreset, TextAnimationType, String, Boolean, Boolean, Boolean, Boolean, String) -> Unit,
    onGenerateAutoCaptions: () -> Unit,
    onUpdateText: (TextOverlayItem) -> Unit,
    onDuplicateText: (String) -> Unit,
    onDeleteText: (String) -> Unit,
    onAddSticker: (StickerCatalogItem) -> Unit,
    onUpdateSticker: (StickerOverlayItem) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onAddPipClick: () -> Unit,
    onUpdatePip: (PipOverlayItem) -> Unit,
    onDeletePip: (String) -> Unit,
    onSelectEffect: (VideoEffectPreset, Float) -> Unit,
    onSelectFilter: (FilterPreset, Float) -> Unit,
    onUpdateAdjustments: (ColorAdjustments) -> Unit,
    onResetAdjustments: () -> Unit,
    onSmartAutoCropRatio: (AspectRatioOption, Boolean) -> Unit,
    onSelectCanvasColor: (Long) -> Unit,
    onSelectCanvasStyle: (CanvasStyle) -> Unit,
    onOpenAiVideoGen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StudioBorder.copy(alpha = 0.65f))
            )

            if (activeTab == EditorToolTab.ROOT) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rootDockItems, key = { it.tab.name }) { item ->
                        val activeBadge = when (item.tab) {
                            EditorToolTab.EFFECTS -> project.effectPreset != VideoEffectPreset.NONE
                            EditorToolTab.FILTERS -> project.filterPreset != FilterPreset.ORIGINAL
                            EditorToolTab.VOICE_FX -> project.voiceEnhance.isActive
                            EditorToolTab.AUDIO -> project.audioTrack != null || project.sfxOverlays.isNotEmpty()
                            EditorToolTab.ANIMATE -> (selectedClip?.animation ?: ClipAnimationType.NONE) != ClipAnimationType.NONE
                            EditorToolTab.CROP -> selectedClip?.hasCrop == true
                            EditorToolTab.REMOVE_BG -> (selectedClip?.removeBgMode ?: RemoveBgMode.NONE) != RemoveBgMode.NONE
                            EditorToolTab.AI_REMOVER -> (selectedClip?.aiRemoverZones?.isNotEmpty() == true)
                            EditorToolTab.TRANSFORM -> (selectedClip?.keyframes?.isNotEmpty() == true) ||
                                kotlin.math.abs((selectedClip?.zoomScale ?: 1f) - 1f) > 0.02f
                            else -> false
                        }
                        ToolIconTile(
                            label = item.label,
                            icon = item.icon,
                            onClick = { onSelectTab(item.tab) },
                            testTag = "tool_dock_${item.tab.name.lowercase()}",
                            active = activeBadge
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSelectTab(EditorToolTab.ROOT) },
                        modifier = Modifier
                            .testTag("tool_subpanel_back")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to tools",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = activeTab.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                }

                when (activeTab) {
                    EditorToolTab.AI_LAB -> AiLabPanel(
                        project = project,
                        aiLabState = aiLabState,
                        onToggleAutoEnhance = onToggleAutoEnhance,
                        onRunAiVideoEnhance = onRunAiVideoEnhance,
                        onApplyAiEnhance = onApplyAiEnhance,
                        onRunAiCaptions = onRunAiCaptions,
                        onApplyAiCaptions = onApplyAiCaptions,
                        onStreamAiScript = onStreamAiScript,
                        onResetAiState = onResetAiState,
                        onOpenAiVideoGen = onOpenAiVideoGen
                    )
                    EditorToolTab.VOICE_FX -> VoiceEnhancerPanel(
                        voiceSettings = project.voiceEnhance,
                        onSelectPreset = onSelectVoicePreset,
                        onUpdateSettings = onUpdateVoiceSettings
                    )
                    EditorToolTab.EDIT_CLIP -> EditClipPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onSplitClip = onSplitClip,
                        onFreezeFrame = onFreezeFrame,
                        onReplaceClipClick = onReplaceClipClick,
                        onOpenTab = onSelectTab,
                        onUpdateTrim = onUpdateTrim,
                        onUpdateSpeed = onUpdateSpeed,
                        onUpdateSpeedCurve = onUpdateSpeedCurve,
                        onUpdateClipVolume = onUpdateClipVolume,
                        onRotateClip = onRotateClip,
                        onFlipClip = onFlipClip,
                        onFlipClipVertical = onFlipClipVertical,
                        onUpdateClipZoom = onUpdateClipZoom,
                        onMoveClip = onMoveClip,
                        onDuplicateClip = onDuplicateClip,
                        onDeleteClip = onDeleteClip
                    )
                    EditorToolTab.TRANSFORM -> TransformPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onUpdateTransform = onUpdateClipTransform,
                        onRotate90 = onRotateClip,
                        onFlipHorizontal = onFlipClip,
                        onFlipVertical = onFlipClipVertical,
                        onApplyZoomOnOff = onApplyZoomOnOff,
                        onToggleKeyframe = onToggleKeyframe,
                        onReset = onResetClipTransform
                    )
                    EditorToolTab.CROP -> CropPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onUpdateCrop = onUpdateClipCrop,
                        onResetCrop = onResetClipCrop
                    )
                    EditorToolTab.REMOVE_BG -> RemoveBgPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onUpdateRemoveBg = onUpdateRemoveBg
                    )
                    EditorToolTab.AI_REMOVER -> AiRemoverPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        selectedZoneId = selectedRemoverZoneId,
                        onAddZone = onAddRemoverZone,
                        onUpdateZone = onUpdateRemoverZone,
                        onDeleteZone = onDeleteRemoverZone
                    )
                    EditorToolTab.CAPTIONS -> CaptionsTranslatePanel(
                        aiLabState = aiLabState,
                        onGenerateAutoCaptions = onGenerateAutoCaptions,
                        onTranslateVideo = onTranslateVideo,
                        onApplyTranslation = onApplyTranslation,
                        onResetAiState = onResetAiState
                    )
                    EditorToolTab.ANIMATE -> AnimateClipPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onSelectAnimation = onUpdateClipAnimation
                    )
                    EditorToolTab.AUDIO -> AudioPanel(
                        project = project,
                        onSelectPreset = onSelectMusicPreset,
                        onImportAudioClick = onImportAudioClick,
                        onUpdateVolume = onUpdateAudioVolume,
                        onToggleMute = onToggleAudioMute,
                        onToggleFades = onToggleAudioFades,
                        onRemove = onRemoveAudio,
                        onAddSfx = onAddSfx,
                        onDeleteSfx = onDeleteSfx,
                        onAutoBeatMarkers = onAutoBeatMarkers
                    )
                    EditorToolTab.TEXT -> TextPanel(
                        project = project,
                        selectedText = selectedText,
                        onAddText = onAddText,
                        onGenerateAutoCaptions = onGenerateAutoCaptions,
                        onUpdateText = onUpdateText,
                        onDuplicateText = onDuplicateText,
                        onDeleteText = onDeleteText
                    )
                    EditorToolTab.STICKERS -> StickersPanel(
                        selectedSticker = selectedSticker,
                        onAddSticker = onAddSticker,
                        onUpdateSticker = onUpdateSticker,
                        onDeleteSticker = onDeleteSticker
                    )
                    EditorToolTab.OVERLAY -> OverlayPipPanel(
                        project = project,
                        selectedPip = selectedPip ?: project.pipOverlays.firstOrNull(),
                        onAddPipClick = onAddPipClick,
                        onUpdatePip = onUpdatePip,
                        onDeletePip = onDeletePip
                    )
                    EditorToolTab.EFFECTS -> EffectsPanel(
                        selected = project.effectPreset,
                        intensity = project.effectIntensity,
                        onSelect = onSelectEffect
                    )
                    EditorToolTab.FILTERS -> FiltersPanel(
                        selected = project.filterPreset,
                        intensity = project.filterIntensity,
                        onSelect = onSelectFilter
                    )
                    EditorToolTab.ADJUST -> AdjustPanel(
                        adjustments = project.adjustments,
                        onToggleAutoEnhance = onToggleAutoEnhance,
                        onUpdate = onUpdateAdjustments,
                        onReset = onResetAdjustments
                    )
                    EditorToolTab.RATIO -> RatioPanel(
                        selected = project.aspectRatio,
                        onSelectRatio = onSmartAutoCropRatio
                    )
                    EditorToolTab.CANVAS -> CanvasPanel(
                        selectedColor = project.canvasColorHex,
                        selectedStyle = project.canvasStyle,
                        onSelectColor = onSelectCanvasColor,
                        onSelectStyle = onSelectCanvasStyle
                    )
                    EditorToolTab.ROOT -> Unit
                }
            }
        }
    }
}

@Composable
private fun ToolIconTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = TextPrimary,
    active: Boolean = false
) {
    Column(
        modifier = Modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) ElectricCyan.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ElectricCyan else tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) ElectricCyan else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private enum class ClipSubMode { ACTIONS, TRIM, SPEED, CURVE, VOLUME, ZOOM }

@Composable
private fun EditClipPanel(
    clip: VideoClip?,
    onSplitClip: () -> Unit,
    onFreezeFrame: () -> Unit,
    onReplaceClipClick: () -> Unit,
    onOpenTab: (EditorToolTab) -> Unit,
    onUpdateTrim: (String, Long, Long) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateSpeedCurve: (String, SpeedCurvePreset) -> Unit,
    onUpdateClipVolume: (String, Float) -> Unit,
    onRotateClip: (String) -> Unit,
    onFlipClip: (String) -> Unit,
    onFlipClipVertical: (String) -> Unit,
    onUpdateClipZoom: (String, Float) -> Unit,
    onMoveClip: (String, Int) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to edit.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    var subMode by remember(clip.id) { mutableStateOf(ClipSubMode.ACTIONS) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        if (subMode != ClipSubMode.ACTIONS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (subMode) {
                        ClipSubMode.TRIM -> "Trim Clip (${String.format("%.1fs", (clip.trimEndMs - clip.trimStartMs) / 1000f)})"
                        ClipSubMode.SPEED -> "Normal Speed (${String.format("%.2fx", clip.speed)})"
                        ClipSubMode.CURVE -> "Speed Curve (${clip.speedCurve.label})"
                        ClipSubMode.VOLUME -> "Clip Volume (${(clip.volume * 100).toInt()}%)"
                        ClipSubMode.ZOOM -> "Zoom Scale (${String.format("%.2fx", clip.zoomScale)})"
                        ClipSubMode.ACTIONS -> ""
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subMode == ClipSubMode.SPEED) {
                        TextButton(onClick = { subMode = ClipSubMode.CURVE }) {
                            Text("Curve", color = NeonRose)
                        }
                    } else if (subMode == ClipSubMode.CURVE) {
                        TextButton(onClick = { subMode = ClipSubMode.SPEED }) {
                            Text("Normal", color = ElectricCyan)
                        }
                    }
                    TextButton(onClick = { subMode = ClipSubMode.ACTIONS }) {
                        Text("Done", color = ElectricCyan)
                    }
                }
            }
        }

        when (subMode) {
            ClipSubMode.TRIM -> {
                val maxDur = clip.totalDurationMs.coerceAtLeast(1000L).toFloat()
                RangeSlider(
                    value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
                    onValueChange = { range ->
                        onUpdateTrim(clip.id, range.start.toLong(), range.endInclusive.toLong())
                    },
                    valueRange = 0f..maxDur,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            ClipSubMode.SPEED -> {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                            val selected = kotlin.math.abs(clip.speed - spd) < 0.05f && clip.speedCurve == SpeedCurvePreset.NONE
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateSpeed(clip.id, spd) },
                                color = if (selected) ElectricCyan else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${spd}x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color(0xFF041418) else TextPrimary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Slider(
                        value = clip.speed,
                        onValueChange = { onUpdateSpeed(clip.id, it) },
                        valueRange = 0.25f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }
            }
            ClipSubMode.CURVE -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SpeedCurvePreset.entries, key = { it.name }) { curve ->
                        val selected = clip.speedCurve == curve
                        Surface(
                            modifier = Modifier
                                .testTag("speed_curve_${curve.name.lowercase()}")
                                .width(104.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdateSpeedCurve(clip.id, curve) },
                            color = if (selected) NeonRose.copy(alpha = 0.18f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) NeonRose else StudioBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                ) {
                                    val path = Path()
                                    val pts = curve.points
                                    pts.forEachIndexed { idx, v ->
                                        val x = (idx / (pts.size - 1).coerceAtLeast(1).toFloat()) * size.width
                                        val y = (1f - v) * size.height
                                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (selected) NeonRose else ElectricCyan,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(curve.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
                                Text("${curve.effectiveMultiplier}x", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
            ClipSubMode.VOLUME -> {
                Slider(
                    value = clip.volume,
                    onValueChange = { onUpdateClipVolume(clip.id, it) },
                    valueRange = 0f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            ClipSubMode.ZOOM -> {
                Slider(
                    value = clip.zoomScale,
                    onValueChange = { onUpdateClipZoom(clip.id, it) },
                    valueRange = 0.6f..2.2f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            ClipSubMode.ACTIONS -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        ToolIconTile("Split", Icons.Filled.ContentCut, onSplitClip, "clip_split_btn", ElectricCyan)
                    }
                    item {
                        ToolIconTile("Transform", Icons.Filled.Transform, { onOpenTab(EditorToolTab.TRANSFORM) }, "clip_transform_btn", ElectricCyan)
                    }
                    item {
                        ToolIconTile("Crop", Icons.Filled.Crop, { onOpenTab(EditorToolTab.CROP) }, "clip_crop_btn")
                    }
                    item {
                        ToolIconTile("Speed", Icons.Filled.Speed, { subMode = ClipSubMode.SPEED }, "clip_speed_btn")
                    }
                    item {
                        ToolIconTile("Curve", Icons.Filled.AutoFixHigh, { subMode = ClipSubMode.CURVE }, "clip_curve_btn", NeonRose)
                    }
                    item {
                        ToolIconTile("Animate", Icons.Filled.Animation, { onOpenTab(EditorToolTab.ANIMATE) }, "clip_animate_btn", ElectricCyan)
                    }
                    item {
                        ToolIconTile("Remove BG", Icons.Filled.LayersClear, { onOpenTab(EditorToolTab.REMOVE_BG) }, "clip_removebg_btn", VioletPulse)
                    }
                    item {
                        ToolIconTile("AI Eraser", Icons.Filled.CleaningServices, { onOpenTab(EditorToolTab.AI_REMOVER) }, "clip_airemover_btn", NeonRose)
                    }
                    item {
                        ToolIconTile("Freeze", Icons.Filled.AcUnit, onFreezeFrame, "clip_freeze_btn", StudioAmber)
                    }
                    item {
                        ToolIconTile("Replace", Icons.Filled.SwapHoriz, onReplaceClipClick, "clip_replace_btn")
                    }
                    item {
                        ToolIconTile("Trim", Icons.Filled.Tune, { subMode = ClipSubMode.TRIM }, "clip_trim_btn")
                    }
                    item {
                        ToolIconTile("Volume", Icons.AutoMirrored.Filled.VolumeUp, { subMode = ClipSubMode.VOLUME }, "clip_vol_btn")
                    }
                    item {
                        ToolIconTile("Rotate", Icons.Filled.RotateRight, { onRotateClip(clip.id) }, "clip_rotate_btn")
                    }
                    item {
                        ToolIconTile("Mirror", Icons.Filled.Flip, { onFlipClip(clip.id) }, "clip_flip_btn")
                    }
                    item {
                        ToolIconTile("Reflect V", Icons.Filled.Flip, { onFlipClipVertical(clip.id) }, "clip_reflect_v_btn")
                    }
                    item {
                        ToolIconTile("Zoom", Icons.Filled.ZoomIn, { subMode = ClipSubMode.ZOOM }, "clip_zoom_btn")
                    }
                    item {
                        ToolIconTile("Move L", Icons.AutoMirrored.Filled.KeyboardArrowLeft, { onMoveClip(clip.id, -1) }, "clip_move_left_btn")
                    }
                    item {
                        ToolIconTile("Move R", Icons.AutoMirrored.Filled.KeyboardArrowRight, { onMoveClip(clip.id, 1) }, "clip_move_right_btn")
                    }
                    item {
                        ToolIconTile("Copy", Icons.Filled.ContentCopy, { onDuplicateClip(clip.id) }, "clip_dup_btn")
                    }
                    item {
                        ToolIconTile("Delete", Icons.Filled.DeleteOutline, { onDeleteClip(clip.id) }, "clip_del_btn", NeonRose)
                    }
                }
            }
        }
    }
}

private enum class AudioSubTab(val label: String) {
    SOUNDS("Sounds"),
    SFX("Sound FX")
}

@Composable
private fun AudioPanel(
    project: ProjectData,
    onSelectPreset: (String) -> Unit,
    onImportAudioClick: () -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleFades: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onAddSfx: (String) -> Unit,
    onDeleteSfx: (String) -> Unit,
    onAutoBeatMarkers: (Int, Boolean) -> Unit
) {
    var audioTab by remember { mutableStateOf(AudioSubTab.SOUNDS) }
    var beatBpm by remember { mutableIntStateOf(124) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioSubTab.entries.forEach { tab ->
                val selected = audioTab == tab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { audioTab = tab },
                    color = if (selected) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) Color(0xFF041418) else TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onImportAudioClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Add, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Device Audio", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
        }

        if (audioTab == AudioSubTab.SOUNDS) {
            project.audioTrack?.let { current ->
                val hasFade = current.fadeInMs > 0L || current.fadeOutMs > 0L
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(current.title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Slider(
                                value = current.volume,
                                onValueChange = onUpdateVolume,
                                valueRange = 0f..1.2f,
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                        TextButton(onClick = { onToggleFades(!hasFade) }) {
                            Text(if (hasFade) "Fade ON" else "Fade OFF", color = if (hasFade) ElectricCyan else TextMuted)
                        }
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                imageVector = if (current.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute music",
                                tint = if (current.isMuted) NeonRose else ElectricCyan
                            )
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove audio", tint = NeonRose)
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioSynthLibrary.presets, key = { it.id }) { preset ->
                    val isSelected = project.audioTrack?.id == preset.id
                    Surface(
                        modifier = Modifier
                            .testTag("audio_preset_${preset.id}")
                            .width(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset.id) },
                        color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StudioBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(preset.title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                            Text("${preset.genre} · ${preset.bpm} BPM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioSynthLibrary.sfxPresets, key = { it.id }) { sfx ->
                    Surface(
                        modifier = Modifier
                            .testTag("sfx_preset_${sfx.id}")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAddSfx(sfx.id) },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(sfx.title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text("${sfx.category} · + Playhead", style = MaterialTheme.typography.labelSmall, color = NeonRose)
                        }
                    }
                }
            }
            if (project.sfxOverlays.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(project.sfxOverlays, key = { it.id }) { sfx ->
                        Surface(
                            color = NeonRose.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { onDeleteSfx(sfx.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${sfx.title} @ ${(sfx.startMs / 1000f).let { String.format("%.1fs", it) }}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Filled.DeleteOutline, "Remove SFX", tint = NeonRose, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Auto-Beat Marker & Rhythm Cut bar
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Beat & Rhythm Cut",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(100, 120, 124, 128).forEach { bpmOption ->
                            val selectedBpm = beatBpm == bpmOption
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { beatBpm = bpmOption },
                                color = if (selectedBpm) ElectricCyan else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$bpmOption",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedBpm) Color(0xFF041418) else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onAutoBeatMarkers(beatBpm, false) },
                        modifier = Modifier
                            .testTag("audio_beat_marks_button")
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Add Beat Marks", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                    }
                    Button(
                        onClick = { onAutoBeatMarkers(beatBpm, true) },
                        modifier = Modifier
                            .testTag("audio_rhythm_cut_button")
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Rhythm Cut", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextPanel(
    project: ProjectData,
    selectedText: TextOverlayItem?,
    onAddText: (String, TextStylePreset, TextAnimationType, String, Boolean, Boolean, Boolean, Boolean, String) -> Unit,
    onGenerateAutoCaptions: () -> Unit,
    onUpdateText: (TextOverlayItem) -> Unit,
    onDuplicateText: (String) -> Unit,
    onDeleteText: (String) -> Unit
) {
    var showComposer by remember { mutableStateOf(false) }
    var editingExisting by remember { mutableStateOf<TextOverlayItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    editingExisting = null
                    showComposer = true
                },
                modifier = Modifier
                    .testTag("add_text_button")
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
            ) {
                Icon(Icons.Filled.Title, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Urdu · Hindi · EN", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onGenerateAutoCaptions,
                modifier = Modifier
                    .testTag("auto_captions_button")
                    .weight(0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Subtitles, null, tint = NeonRose, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Captions", color = TextPrimary)
            }
        }

        if (selectedText != null) {
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\"${selectedText.text}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editingExisting = selectedText
                            showComposer = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit text", tint = ElectricCyan)
                        }
                        IconButton(onClick = { onDuplicateText(selectedText.id) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate text", tint = TextSecondary)
                        }
                        IconButton(onClick = { onDeleteText(selectedText.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete text", tint = NeonRose)
                        }
                    }

                    // Quick toggles: Curved, 3D, Neon, RTL + Font selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            TextFeatureChip("Curved", selectedText.isCurved) {
                                onUpdateText(selectedText.copy(isCurved = !selectedText.isCurved))
                            }
                        }
                        item {
                            TextFeatureChip("3D Depth", selectedText.is3D) {
                                onUpdateText(selectedText.copy(is3D = !selectedText.is3D))
                            }
                        }
                        item {
                            TextFeatureChip("Neon Glow", selectedText.isNeonGlow) {
                                onUpdateText(selectedText.copy(isNeonGlow = !selectedText.isNeonGlow))
                            }
                        }
                        item {
                            TextFeatureChip("RTL اردو", selectedText.isRtl) {
                                onUpdateText(selectedText.copy(isRtl = !selectedText.isRtl))
                            }
                        }
                        items(StudioFontCatalog.options, key = { it.key }) { fontOpt ->
                            val active = selectedText.fontFamilyKey == fontOpt.key
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onUpdateText(
                                            selectedText.copy(
                                                fontFamilyKey = fontOpt.key,
                                                isRtl = if (fontOpt.languageHint == "UR") true else selectedText.isRtl,
                                                languageTag = fontOpt.languageHint
                                            )
                                        )
                                    },
                                color = if (active) VioletPulse else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = fontOpt.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Size", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(46.dp))
                        Slider(
                            value = selectedText.scale,
                            onValueChange = { onUpdateText(selectedText.copy(scale = it)) },
                            valueRange = 0.45f..2.6f,
                            colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose),
                            modifier = Modifier.weight(1f).height(26.dp)
                        )
                        Text("${selectedText.rotationDegrees.toInt()}°", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(34.dp))
                        Slider(
                            value = selectedText.rotationDegrees,
                            onValueChange = { onUpdateText(selectedText.copy(rotationDegrees = it)) },
                            valueRange = -45f..45f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                            modifier = Modifier.weight(0.8f).height(26.dp)
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TextStylePreset.entries) { style ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onUpdateText(
                                            selectedText.copy(
                                                stylePreset = style,
                                                colorHex = style.textColorHex,
                                                bgHex = style.bgColorHex
                                            )
                                        )
                                    },
                                color = if (selectedText.stylePreset == style) ElectricCyan else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedText.stylePreset == style) Color(0xFF041418) else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (project.textOverlays.isNotEmpty()) {
            Text(
                text = "Tap any text layer on the preview or timeline to edit fonts, Curved, 3D, or Neon.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }

    if (showComposer) {
        val target = editingExisting
        var input by remember(target?.id) { mutableStateOf(target?.text ?: "") }
        var langTag by remember(target?.id) { mutableStateOf(target?.languageTag ?: "EN") }
        var fontKey by remember(target?.id) { mutableStateOf(target?.fontFamilyKey ?: "display") }
        var isCurved by remember(target?.id) { mutableStateOf(target?.isCurved ?: false) }
        var is3D by remember(target?.id) { mutableStateOf(target?.is3D ?: false) }
        var isNeonGlow by remember(target?.id) { mutableStateOf(target?.isNeonGlow ?: false) }
        var isRtl by remember(target?.id) { mutableStateOf(target?.isRtl ?: false) }
        var style by remember(target?.id) { mutableStateOf(target?.stylePreset ?: TextStylePreset.CYBER_NEON) }
        var anim by remember(target?.id) { mutableStateOf(target?.animationType ?: TextAnimationType.POP) }

        AlertDialog(
            onDismissRequest = { showComposer = false },
            containerColor = StudioSurfaceElevated,
            title = { Text(if (target == null) "Multilingual Text Studio" else "Edit Text Overlay", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // EN / UR / HI language tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EN" to "English", "UR" to "اردو Urdu", "HI" to "हिंदी Hindi").forEach { (code, label) ->
                            val active = langTag == code
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        langTag = code
                                        fontKey = StudioFontCatalog.defaultKeyForLanguage(code)
                                        isRtl = (code == "UR")
                                    },
                                color = if (active) ElectricCyan else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) Color(0xFF041418) else TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Text(
                                when (langTag) {
                                    "UR" -> "اپنا اردو ٹیکسٹ لکھیں..."
                                    "HI" -> "अपना हिंदी टेक्स्ट लिखें..."
                                    else -> "Enter title or caption..."
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Starter phrases for fast Urdu / Hindi / English creation
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(
                            WorkflowHelpersCatalog.bilingualStarterPhrases.filter { it.languageTag == langTag },
                            key = { it.text }
                        ) { sample ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        input = sample.text
                                        fontKey = sample.fontKey
                                        isRtl = sample.isRtl
                                    },
                                color = StudioSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, StudioBorder)
                            ) {
                                Text(
                                    text = sample.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Text("Font & Effects", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(StudioFontCatalog.options, key = { it.key }) { opt ->
                            val selectedFont = fontKey == opt.key
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        fontKey = opt.key
                                        if (opt.languageHint == "UR") isRtl = true
                                    },
                                color = if (selectedFont) VioletPulse else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextFeatureChip("Curved", isCurved) { isCurved = !isCurved }
                        TextFeatureChip("3D", is3D) { is3D = !is3D }
                        TextFeatureChip("Neon", isNeonGlow) { isNeonGlow = !isNeonGlow }
                        TextFeatureChip("RTL", isRtl) { isRtl = !isRtl }
                    }

                    Text("Style Preset", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TextStylePreset.entries) { preset ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { style = preset },
                                color = if (style == preset) ElectricCyan else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (style == preset) Color(0xFF041418) else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Text("Animation", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TextAnimationType.entries) { a ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { anim = a },
                                color = if (anim == a) NeonRose else StudioSurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    a.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target == null) {
                        onAddText(input, style, anim, fontKey, isCurved, is3D, isNeonGlow, isRtl, langTag)
                    } else {
                        onUpdateText(
                            target.copy(
                                text = input.ifBlank { target.text },
                                stylePreset = style,
                                colorHex = style.textColorHex,
                                bgHex = style.bgColorHex,
                                animationType = anim,
                                fontFamilyKey = fontKey,
                                isCurved = isCurved,
                                is3D = is3D,
                                isNeonGlow = isNeonGlow,
                                isRtl = isRtl,
                                languageTag = langTag
                            )
                        )
                    }
                    showComposer = false
                }) {
                    Text(if (target == null) "Add" else "Save", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showComposer = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun TextFeatureChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (active) NeonRose else StudioSurfaceHighlight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) NeonRose else StudioBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StickersPanel(
    selectedSticker: StickerOverlayItem?,
    onAddSticker: (StickerCatalogItem) -> Unit,
    onUpdateSticker: (StickerOverlayItem) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selectedSticker != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Size ${String.format("%.1fx", selectedSticker.scale)}", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                Slider(
                    value = selectedSticker.scale,
                    onValueChange = { onUpdateSticker(selectedSticker.copy(scale = it)) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(24.dp)
                )
                TextButton(onClick = { onDeleteSticker(selectedSticker.id) }) {
                    Text("Remove", color = NeonRose)
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(StudioCatalog.stickers, key = { it.id }) { item ->
                Surface(
                    modifier = Modifier
                        .testTag("sticker_item_${item.id}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddSticker(item) },
                    color = item.accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.badgeText, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(item.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayPipPanel(
    project: ProjectData,
    selectedPip: PipOverlayItem?,
    onAddPipClick: () -> Unit,
    onUpdatePip: (PipOverlayItem) -> Unit,
    onDeletePip: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAddPipClick,
            modifier = Modifier
                .testTag("add_pip_button")
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VioletPulse, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Layers, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Picture-in-Picture Overlay")
        }

        if (selectedPip != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PipShape.entries.forEach { shape ->
                        val active = selectedPip.shape == shape
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdatePip(selectedPip.copy(shape = shape)) },
                            color = if (active) ElectricCyan else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = shape.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color(0xFF041418) else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdatePip(selectedPip.copy(borderGlow = !selectedPip.borderGlow)) },
                        color = if (selectedPip.borderGlow) NeonRose else StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Glow",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                TextButton(onClick = { onDeletePip(selectedPip.id) }) {
                    Text("Remove", color = NeonRose)
                }
            }
            Slider(
                value = selectedPip.opacity,
                onValueChange = { onUpdatePip(selectedPip.copy(opacity = it)) },
                valueRange = 0.2f..1.0f,
                colors = SliderDefaults.colors(thumbColor = VioletPulse, activeTrackColor = VioletPulse)
            )
            Slider(
                value = selectedPip.scale,
                onValueChange = { onUpdatePip(selectedPip.copy(scale = it)) },
                valueRange = 0.18f..0.65f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
        }
    }
}

private enum class AdjustKey(val label: String) {
    CLARITY("1080P+ Clarity"),
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    HIGHLIGHTS("Highlights"),
    FADE("Fade"),
    WARMTH("Temp"),
    VIGNETTE("Vignette")
}

@Composable
private fun AdjustPanel(
    adjustments: ColorAdjustments,
    onToggleAutoEnhance: () -> Unit,
    onUpdate: (ColorAdjustments) -> Unit,
    onReset: () -> Unit
) {
    var activeKey by remember { mutableStateOf(AdjustKey.CLARITY) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(activeKey.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Surface(
                    modifier = Modifier
                        .testTag("adjust_auto_enhance_chip")
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleAutoEnhance),
                    color = if (adjustments.autoEnhanced) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (adjustments.autoEnhanced) ElectricCyan else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Hd,
                            contentDescription = null,
                            tint = if (adjustments.autoEnhanced) Color(0xFF041418) else ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (adjustments.autoEnhanced) "Auto HD ON" else "Auto Enhance",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (adjustments.autoEnhanced) Color(0xFF041418) else TextPrimary
                        )
                    }
                }
            }
            TextButton(onClick = onReset) {
                Icon(Icons.Filled.RestartAlt, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = ElectricCyan)
            }
        }

        when (activeKey) {
            AdjustKey.CLARITY -> Slider(
                value = adjustments.clarity,
                onValueChange = { onUpdate(adjustments.copy(clarity = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.BRIGHTNESS -> Slider(
                value = adjustments.brightness,
                onValueChange = { onUpdate(adjustments.copy(brightness = it)) },
                valueRange = -0.45f..0.45f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.CONTRAST -> Slider(
                value = adjustments.contrast,
                onValueChange = { onUpdate(adjustments.copy(contrast = it)) },
                valueRange = -0.45f..0.45f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.SATURATION -> Slider(
                value = adjustments.saturation,
                onValueChange = { onUpdate(adjustments.copy(saturation = it)) },
                valueRange = 0f..2f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.HIGHLIGHTS -> Slider(
                value = adjustments.highlights,
                onValueChange = { onUpdate(adjustments.copy(highlights = it)) },
                valueRange = -0.3f..0.3f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.FADE -> Slider(
                value = adjustments.fade,
                onValueChange = { onUpdate(adjustments.copy(fade = it)) },
                valueRange = 0f..0.5f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.WARMTH -> Slider(
                value = adjustments.warmth,
                onValueChange = { onUpdate(adjustments.copy(warmth = it)) },
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            AdjustKey.VIGNETTE -> Slider(
                value = adjustments.vignette,
                onValueChange = { onUpdate(adjustments.copy(vignette = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AdjustKey.entries) { key ->
                val selected = key == activeKey
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeKey = key },
                    color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = key.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) ElectricCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatioPanel(
    selected: AspectRatioOption,
    onSelectRatio: (AspectRatioOption, Boolean) -> Unit
) {
    var smartAutoCropEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart Subject Auto-Crop",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Keeps face or hero object centered when switching ratio",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Switch(
                checked = smartAutoCropEnabled,
                onCheckedChange = { smartAutoCropEnabled = it },
                modifier = Modifier.testTag("ratio_smart_autocrop_switch")
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AspectRatioOption.entries, key = { it.name }) { ratio ->
                val isSelected = ratio == selected
                val platformTag = when (ratio) {
                    AspectRatioOption.RATIO_9_16 -> "Reels · TikTok"
                    AspectRatioOption.RATIO_16_9 -> "YouTube"
                    AspectRatioOption.RATIO_1_1 -> "Square Post"
                    AspectRatioOption.RATIO_4_5 -> "IG Portrait"
                    AspectRatioOption.RATIO_3_4 -> "Feed Card"
                }
                Surface(
                    modifier = Modifier
                        .testTag("ratio_option_${ratio.label}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectRatio(ratio, smartAutoCropEnabled) },
                    color = if (isSelected) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) Color(0xFF041418) else TextPrimary
                        )
                        Text(
                            text = platformTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF041418).copy(alpha = 0.8f) else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasPanel(
    selectedColor: Long,
    selectedStyle: CanvasStyle,
    onSelectColor: (Long) -> Unit,
    onSelectStyle: (CanvasStyle) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CanvasStyle.entries, key = { it.name }) { style ->
                val active = style == selectedStyle
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectStyle(style) },
                    color = if (active) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color(0xFF041418) else TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(StudioCatalog.canvasColors) { hex ->
                val isSelected = hex == selectedColor
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(hex) }
                )
            }
        }
    }
}
