package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Title
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.AudioSynthLibrary
import com.example.media.StudioCatalog
import com.example.model.AspectRatioOption
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.FilterPreset
import com.example.model.PipOverlayItem
import com.example.model.ProjectData
import com.example.model.StickerCatalogItem
import com.example.model.StickerOverlayItem
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
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
    ToolDockItem(EditorToolTab.AUDIO, "Audio", Icons.Filled.MusicNote),
    ToolDockItem(EditorToolTab.TEXT, "Text", Icons.Filled.Title),
    ToolDockItem(EditorToolTab.STICKERS, "Stickers", Icons.Filled.EmojiEmotions),
    ToolDockItem(EditorToolTab.OVERLAY, "Overlay", Icons.Filled.Layers),
    ToolDockItem(EditorToolTab.EFFECTS, "Effects", Icons.Filled.AutoFixHigh),
    ToolDockItem(EditorToolTab.FILTERS, "Filters", Icons.Filled.FilterVintage),
    ToolDockItem(EditorToolTab.ADJUST, "Adjust", Icons.Filled.Tune),
    ToolDockItem(EditorToolTab.RATIO, "Ratio", Icons.Filled.AspectRatio),
    ToolDockItem(EditorToolTab.CANVAS, "Canvas", Icons.Filled.Palette)
)

@Composable
fun EditorBottomToolbar(
    activeTab: EditorToolTab,
    project: ProjectData,
    selectedClip: VideoClip?,
    selectedText: TextOverlayItem?,
    selectedSticker: StickerOverlayItem?,
    selectedPip: PipOverlayItem?,
    onSelectTab: (EditorToolTab) -> Unit,
    onSplitClip: () -> Unit,
    onUpdateTrim: (String, Long, Long) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateClipVolume: (String, Float) -> Unit,
    onRotateClip: (String) -> Unit,
    onFlipClip: (String) -> Unit,
    onUpdateClipZoom: (String, Float) -> Unit,
    onMoveClip: (String, Int) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onSelectMusicPreset: (String) -> Unit,
    onImportAudioClick: () -> Unit,
    onUpdateAudioVolume: (Float) -> Unit,
    onToggleAudioMute: () -> Unit,
    onRemoveAudio: () -> Unit,
    onAddText: (String, TextStylePreset, TextAnimationType) -> Unit,
    onGenerateAutoCaptions: () -> Unit,
    onUpdateText: (TextOverlayItem) -> Unit,
    onDeleteText: (String) -> Unit,
    onAddSticker: (StickerCatalogItem) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onAddPipClick: () -> Unit,
    onUpdatePip: (PipOverlayItem) -> Unit,
    onDeletePip: (String) -> Unit,
    onSelectEffect: (VideoEffectPreset) -> Unit,
    onSelectFilter: (FilterPreset, Float) -> Unit,
    onUpdateAdjustments: (ColorAdjustments) -> Unit,
    onResetAdjustments: () -> Unit,
    onSelectRatio: (AspectRatioOption) -> Unit,
    onSelectCanvasColor: (Long) -> Unit,
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
                        ToolIconTile(
                            label = item.label,
                            icon = item.icon,
                            onClick = { onSelectTab(item.tab) },
                            testTag = "tool_dock_${item.tab.name.lowercase()}"
                        )
                    }
                }
            } else {
                // Sub-panel header with Back chevron
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
                    EditorToolTab.EDIT_CLIP -> EditClipPanel(
                        clip = selectedClip ?: project.clips.firstOrNull(),
                        onSplitClip = onSplitClip,
                        onUpdateTrim = onUpdateTrim,
                        onUpdateSpeed = onUpdateSpeed,
                        onUpdateClipVolume = onUpdateClipVolume,
                        onRotateClip = onRotateClip,
                        onFlipClip = onFlipClip,
                        onUpdateClipZoom = onUpdateClipZoom,
                        onMoveClip = onMoveClip,
                        onDuplicateClip = onDuplicateClip,
                        onDeleteClip = onDeleteClip
                    )
                    EditorToolTab.AUDIO -> AudioPanel(
                        project = project,
                        onSelectPreset = onSelectMusicPreset,
                        onImportAudioClick = onImportAudioClick,
                        onUpdateVolume = onUpdateAudioVolume,
                        onToggleMute = onToggleAudioMute,
                        onRemove = onRemoveAudio
                    )
                    EditorToolTab.TEXT -> TextPanel(
                        project = project,
                        selectedText = selectedText,
                        onAddText = onAddText,
                        onGenerateAutoCaptions = onGenerateAutoCaptions,
                        onUpdateText = onUpdateText,
                        onDeleteText = onDeleteText
                    )
                    EditorToolTab.STICKERS -> StickersPanel(
                        selectedSticker = selectedSticker,
                        onAddSticker = onAddSticker,
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
                        onSelect = onSelectEffect
                    )
                    EditorToolTab.FILTERS -> FiltersPanel(
                        selected = project.filterPreset,
                        intensity = project.filterIntensity,
                        onSelect = onSelectFilter
                    )
                    EditorToolTab.ADJUST -> AdjustPanel(
                        adjustments = project.adjustments,
                        onUpdate = onUpdateAdjustments,
                        onReset = onResetAdjustments
                    )
                    EditorToolTab.RATIO -> RatioPanel(
                        selected = project.aspectRatio,
                        onSelect = onSelectRatio
                    )
                    EditorToolTab.CANVAS -> CanvasPanel(
                        selectedColor = project.canvasColorHex,
                        onSelect = onSelectCanvasColor
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

private enum class ClipSubMode { ACTIONS, TRIM, SPEED, VOLUME, ZOOM }

@Composable
private fun EditClipPanel(
    clip: VideoClip?,
    onSplitClip: () -> Unit,
    onUpdateTrim: (String, Long, Long) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateClipVolume: (String, Float) -> Unit,
    onRotateClip: (String) -> Unit,
    onFlipClip: (String) -> Unit,
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
                        ClipSubMode.SPEED -> "Speed (${String.format("%.2fx", clip.speed)})"
                        ClipSubMode.VOLUME -> "Clip Volume (${(clip.volume * 100).toInt()}%)"
                        ClipSubMode.ZOOM -> "Zoom Scale (${String.format("%.2fx", clip.zoomScale)})"
                        ClipSubMode.ACTIONS -> ""
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                TextButton(onClick = { subMode = ClipSubMode.ACTIONS }) {
                    Text("Done", color = ElectricCyan)
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
                            val selected = kotlin.math.abs(clip.speed - spd) < 0.05f
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
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        ToolIconTile("Trim", Icons.Filled.Tune, { subMode = ClipSubMode.TRIM }, "clip_trim_btn")
                    }
                    item {
                        ToolIconTile("Speed", Icons.Filled.Speed, { subMode = ClipSubMode.SPEED }, "clip_speed_btn")
                    }
                    item {
                        ToolIconTile("Volume", Icons.AutoMirrored.Filled.VolumeUp, { subMode = ClipSubMode.VOLUME }, "clip_vol_btn")
                    }
                    item {
                        ToolIconTile("Rotate", Icons.Filled.RotateRight, { onRotateClip(clip.id) }, "clip_rotate_btn")
                    }
                    item {
                        ToolIconTile("Flip", Icons.Filled.Flip, { onFlipClip(clip.id) }, "clip_flip_btn")
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

@Composable
private fun AudioPanel(
    project: ProjectData,
    onSelectPreset: (String) -> Unit,
    onImportAudioClick: () -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        project.audioTrack?.let { current ->
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(current.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Slider(
                            value = current.volume,
                            onValueChange = onUpdateVolume,
                            valueRange = 0f..1.2f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                            modifier = Modifier.height(28.dp)
                        )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("s.s Sound Library", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            OutlinedButton(
                onClick = onImportAudioClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Device Audio", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AudioSynthLibrary.presets, key = { it.id }) { preset ->
                val isSelected = project.audioTrack?.id == preset.id
                Surface(
                    modifier = Modifier
                        .testTag("audio_preset_${preset.id}")
                        .width(156.dp)
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
    }
}

@Composable
private fun TextPanel(
    project: ProjectData,
    selectedText: TextOverlayItem?,
    onAddText: (String, TextStylePreset, TextAnimationType) -> Unit,
    onGenerateAutoCaptions: () -> Unit,
    onUpdateText: (TextOverlayItem) -> Unit,
    onDeleteText: (String) -> Unit
) {
    var showComposer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { showComposer = true },
                modifier = Modifier
                    .testTag("add_text_button")
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
            ) {
                Icon(Icons.Filled.Title, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add text", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onGenerateAutoCaptions,
                modifier = Modifier
                    .testTag("auto_captions_button")
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Subtitles, null, tint = NeonRose, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto captions", color = TextPrimary)
            }
        }

        if (selectedText != null) {
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editing: \"${selectedText.text}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteText(selectedText.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete text", tint = NeonRose)
                        }
                    }
                    Text("Scale", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Slider(
                        value = selectedText.scale,
                        onValueChange = { onUpdateText(selectedText.copy(scale = it)) },
                        valueRange = 0.6f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose)
                    )
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
                text = "Tap any text layer on the preview or timeline to customize or drag it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }

    if (showComposer) {
        var input by remember { mutableStateOf("") }
        var style by remember { mutableStateOf(TextStylePreset.CYBER_NEON) }
        var anim by remember { mutableStateOf(TextAnimationType.POP) }

        AlertDialog(
            onDismissRequest = { showComposer = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Add Text Overlay", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Enter title or caption...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    onAddText(input, style, anim)
                    showComposer = false
                }) {
                    Text("Add", color = ElectricCyan, fontWeight = FontWeight.Bold)
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
private fun StickersPanel(
    selectedSticker: StickerOverlayItem?,
    onAddSticker: (StickerCatalogItem) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedSticker != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Selected: ${selectedSticker.label}", style = MaterialTheme.typography.labelLarge, color = StudioAmber)
                TextButton(onClick = { onDeleteSticker(selectedSticker.id) }) {
                    Text("Remove Sticker", color = NeonRose)
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
                Text("PiP Opacity & Size (${project.pipOverlays.size})", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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

@Composable
private fun EffectsPanel(
    selected: VideoEffectPreset,
    onSelect: (VideoEffectPreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(VideoEffectPreset.entries, key = { it.name }) { fx ->
            val isSelected = fx == selected
            Surface(
                modifier = Modifier
                    .testTag("effect_preset_${fx.name.lowercase()}")
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(fx) },
                color = if (isSelected) NeonRose.copy(alpha = 0.2f) else StudioSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, if (isSelected) NeonRose else StudioBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(fx.tag, style = MaterialTheme.typography.labelSmall, color = if (isSelected) NeonRose else TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(fx.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun FiltersPanel(
    selected: FilterPreset,
    intensity: Float,
    onSelect: (FilterPreset, Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        if (selected != FilterPreset.ORIGINAL) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intensity ${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = intensity,
                    onValueChange = { onSelect(selected, it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(FilterPreset.entries, key = { it.name }) { preset ->
                val isSelected = preset == selected
                Column(
                    modifier = Modifier
                        .testTag("filter_preset_${preset.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(preset, intensity) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(preset.badgeColorHex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) ElectricCyan else StudioBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.label.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) ElectricCyan else TextSecondary
                    )
                }
            }
        }
    }
}

private enum class AdjustKey(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    WARMTH("Temp"),
    VIGNETTE("Vignette")
}

@Composable
private fun AdjustPanel(
    adjustments: ColorAdjustments,
    onUpdate: (ColorAdjustments) -> Unit,
    onReset: () -> Unit
) {
    var activeKey by remember { mutableStateOf(AdjustKey.BRIGHTNESS) }

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
            Text(activeKey.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            TextButton(onClick = onReset) {
                Icon(Icons.Filled.RestartAlt, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = ElectricCyan)
            }
        }

        when (activeKey) {
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
    onSelect: (AspectRatioOption) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(AspectRatioOption.entries, key = { it.name }) { ratio ->
            val isSelected = ratio == selected
            Surface(
                modifier = Modifier
                    .testTag("ratio_option_${ratio.label}")
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(ratio) },
                color = if (isSelected) ElectricCyan else StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StudioBorder)
            ) {
                Text(
                    text = ratio.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) Color(0xFF041418) else TextPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun CanvasPanel(
    selectedColor: Long,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(StudioCatalog.canvasColors) { hex ->
            val isSelected = hex == selectedColor
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
