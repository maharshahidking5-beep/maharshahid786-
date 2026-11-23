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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiLabUiState
import com.example.ai.AiTranslatedCaptionPack
import com.example.model.AiRemoverMode
import com.example.model.AiRemoverZone
import com.example.model.RemoveBgMode
import com.example.model.VideoClip
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@Composable
fun TransformPanel(
    clip: VideoClip?,
    onUpdateTransform: (String, Float, Float, Float, Float) -> Unit,
    onRotate90: (String) -> Unit,
    onFlipHorizontal: (String) -> Unit,
    onFlipVertical: (String) -> Unit,
    onApplyZoomOnOff: (String, Boolean) -> Unit,
    onToggleKeyframe: () -> Unit,
    onReset: (String) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to transform.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick CapCut Pro actions row: Zoom On, Zoom Off, Keyframe, Rotate, Mirror H, Reflect V, Reset
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ActionChip(
                    label = "Zoom On",
                    icon = Icons.Filled.ZoomIn,
                    tint = ElectricCyan,
                    tag = "transform_zoom_on"
                ) { onApplyZoomOnOff(clip.id, true) }
            }
            item {
                ActionChip(
                    label = "Zoom Off",
                    icon = Icons.Filled.ZoomOut,
                    tint = NeonRose,
                    tag = "transform_zoom_off"
                ) { onApplyZoomOnOff(clip.id, false) }
            }
            item {
                ActionChip(
                    label = "◆ Keyframe (${clip.keyframes.size})",
                    icon = Icons.Filled.AutoFixHigh,
                    tint = StudioAmber,
                    tag = "transform_keyframe"
                ) { onToggleKeyframe() }
            }
            item {
                ActionChip(
                    label = "Rotate 90°",
                    icon = Icons.Filled.RotateRight,
                    tint = TextPrimary,
                    tag = "transform_rotate90"
                ) { onRotate90(clip.id) }
            }
            item {
                ActionChip(
                    label = if (clip.flipHorizontal) "Mirrored H" else "Mirror H",
                    icon = Icons.Filled.Flip,
                    tint = if (clip.flipHorizontal) ElectricCyan else TextPrimary,
                    tag = "transform_flip_h"
                ) { onFlipHorizontal(clip.id) }
            }
            item {
                ActionChip(
                    label = if (clip.flipVertical) "Reflected V" else "Reflect V",
                    icon = Icons.Filled.Flip,
                    tint = if (clip.flipVertical) VioletPulse else TextPrimary,
                    tag = "transform_flip_v"
                ) { onFlipVertical(clip.id) }
            }
            item {
                ActionChip(
                    label = "Reset",
                    icon = Icons.Filled.RestartAlt,
                    tint = TextSecondary,
                    tag = "transform_reset"
                ) { onReset(clip.id) }
            }
        }

        // Scale & Free Rotation sliders (plus user can pinch/drag directly on canvas!)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Scale ${String.format("%.2fx", clip.zoomScale)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.width(86.dp)
            )
            Slider(
                value = clip.zoomScale,
                onValueChange = { onUpdateTransform(clip.id, it, clip.panX, clip.panY, clip.freeRotation) },
                valueRange = 0.35f..3.0f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                modifier = Modifier.weight(1f).height(28.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Angle ${clip.freeRotation.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.width(86.dp)
            )
            Slider(
                value = clip.freeRotation,
                onValueChange = { onUpdateTransform(clip.id, clip.zoomScale, clip.panX, clip.panY, it) },
                valueRange = -180f..180f,
                colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose),
                modifier = Modifier.weight(1f).height(28.dp)
            )
        }
        Text(
            text = "Tip: Drag or pinch with 2 fingers directly on the preview to zoom, pan & rotate.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
fun CropPanel(
    clip: VideoClip?,
    onUpdateCrop: (String, Float, Float, Float, Float) -> Unit,
    onResetCrop: (String) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to crop.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Crop, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Text("Drag frame handles on video to crop", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }
            TextButton(onClick = { onResetCrop(clip.id) }) {
                Text("Reset", color = ElectricCyan)
            }
        }

        val presets = listOf(
            "Free (Full)" to listOf(0f, 0f, 1f, 1f),
            "Center 80%" to listOf(0.10f, 0.10f, 0.90f, 0.90f),
            "9:16 Tall" to listOf(0.22f, 0f, 0.78f, 1f),
            "1:1 Square" to listOf(0f, 0.22f, 1f, 0.78f),
            "16:9 Cinema" to listOf(0f, 0.28f, 1f, 0.72f),
            "4:3 Classic" to listOf(0.08f, 0.14f, 0.92f, 0.86f)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { (label, box) ->
                Surface(
                    modifier = Modifier
                        .testTag("crop_preset_${label.lowercase()}")
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateCrop(clip.id, box[0], box[1], box[2], box[3]) },
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RemoveBgPanel(
    clip: VideoClip?,
    onUpdateRemoveBg: (String, RemoveBgMode, Long?, Float?, Long?) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to remove background.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val keyColors = listOf(0xFF10B981L, 0xFF00E5FFL, 0xFF2563EBL, 0xFF0A0B0EL, 0xFFFFFFFFL)
    val backdropColors = listOf(0xFF090D16L, 0xFF062B38L, 0xFF2D0A31L, 0xFF1E293BL, 0xFF14532DL, 0xFF000000L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RemoveBgMode.entries.forEach { mode ->
                val selected = clip.removeBgMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("removebg_${mode.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onUpdateRemoveBg(clip.id, mode, null, null, null) },
                    color = if (selected) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LayersClear,
                            null,
                            tint = if (selected) Color(0xFF041418) else TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color(0xFF041418) else TextPrimary
                        )
                    }
                }
            }
        }

        if (clip.removeBgMode != RemoveBgMode.NONE) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (clip.removeBgMode == RemoveBgMode.CHROMA_KEY) "Intensity" else "Feather",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(
                    value = clip.chromaSensitivity,
                    onValueChange = { onUpdateRemoveBg(clip.id, clip.removeBgMode, null, it, null) },
                    valueRange = 0.1f..0.95f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.weight(1f).height(26.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backdrop", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                backdropColors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (clip.customBgHex == hex) 2.dp else 1.dp,
                                color = if (clip.customBgHex == hex) ElectricCyan else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { onUpdateRemoveBg(clip.id, clip.removeBgMode, null, null, hex) }
                    )
                }
                if (clip.removeBgMode == RemoveBgMode.CHROMA_KEY) {
                    Text("Key", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    keyColors.take(3).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (clip.chromaColorHex == hex) 2.dp else 1.dp,
                                    color = if (clip.chromaColorHex == hex) Color.White else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateRemoveBg(clip.id, clip.removeBgMode, hex, null, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiRemoverPanel(
    clip: VideoClip?,
    selectedZoneId: String?,
    onAddZone: (String, AiRemoverMode) -> Unit,
    onUpdateZone: (String, AiRemoverZone) -> Unit,
    onDeleteZone: (String, String) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to use AI Object Remover.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    val selectedZone = clip.aiRemoverZones.find { it.id == selectedZoneId } ?: clip.aiRemoverZones.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onAddZone(clip.id, AiRemoverMode.SMART_PATCH) },
                modifier = Modifier.testTag("ai_remover_add_box"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Eraser Box", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                text = "${clip.aiRemoverZones.size} active · Drag & resize on video",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (selectedZone != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiRemoverMode.entries.forEach { mode ->
                    val active = selectedZone.mode == mode
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateZone(clip.id, selectedZone.copy(mode = mode)) },
                        color = if (active) ElectricCyan else StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) Color(0xFF041418) else TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onDeleteZone(clip.id, selectedZone.id) }) {
                    Icon(Icons.Filled.DeleteOutline, "Delete remover box", tint = NeonRose)
                }
            }
        }
    }
}

@Composable
fun CaptionsTranslatePanel(
    aiLabState: AiLabUiState,
    onGenerateAutoCaptions: () -> Unit,
    onTranslateVideo: (String, String) -> Unit,
    onApplyTranslation: (AiTranslatedCaptionPack) -> Unit,
    onResetAiState: () -> Unit
) {
    var sourceDialogue by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("Urdu") }
    val languages = listOf("Urdu", "English", "Arabic", "Hindi", "Turkish", "Spanish", "French")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onGenerateAutoCaptions,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Subtitles, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto Captions", color = TextPrimary)
            }
            Button(
                onClick = { onTranslateVideo(sourceDialogue, targetLanguage) },
                modifier = Modifier.weight(1f).testTag("video_translate_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPulse, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Translate, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Translate Video ($targetLanguage)")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(languages) { lang ->
                val active = targetLanguage == lang
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { targetLanguage = lang },
                    color = if (active) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Color(0xFF041418) else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = sourceDialogue,
            onValueChange = { sourceDialogue = it },
            placeholder = { Text("Enter dialogue/text to translate (or leave blank for AI scene captions)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        when (aiLabState) {
            is AiLabUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricCyan, strokeWidth = 2.dp)
                    Text(aiLabState.taskLabel, style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                }
            }
            is AiLabUiState.TranslatedReady -> {
                Surface(
                    color = StudioSurfaceHighlight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${aiLabState.translation.targetLanguage} Subtitles Ready", style = MaterialTheme.typography.labelMedium, color = StudioEmerald)
                            Text(aiLabState.translation.line1, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onApplyTranslation(aiLabState.translation)
                                onResetAiState()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color(0xFF041418)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Add to Video", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            is AiLabUiState.Error -> {
                Text(aiLabState.message, style = MaterialTheme.typography.labelSmall, color = NeonRose)
            }
            else -> Unit
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}
