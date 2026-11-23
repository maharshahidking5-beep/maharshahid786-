package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.ai.AiCaptionPack
import com.example.ai.AiEnhanceRecommendation
import com.example.ai.AiLabUiState
import com.example.model.ProjectData
import com.example.model.VoiceEnhancePreset
import com.example.model.VoiceEnhanceSettings
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

private enum class AiLabMode(val label: String) {
    ENHANCE("AI Enhance"),
    CAPTIONS("AI Captions"),
    SCRIPT("AI Script")
}

@Composable
fun AiLabPanel(
    project: ProjectData,
    aiLabState: AiLabUiState,
    onToggleAutoEnhance: () -> Unit,
    onRunAiVideoEnhance: () -> Unit,
    onApplyAiEnhance: (AiEnhanceRecommendation) -> Unit,
    onRunAiCaptions: (String) -> Unit,
    onApplyAiCaptions: (AiCaptionPack) -> Unit,
    onStreamAiScript: (String, String) -> Unit,
    onResetAiState: () -> Unit,
    onOpenAiVideoGen: () -> Unit = {}
) {
    var mode by remember { mutableStateOf(AiLabMode.ENHANCE) }
    var promptInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Hype") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onOpenAiVideoGen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ailab_open_ai_video_gen"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = Color(0xFF041418)
            )
        ) {
            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI Video Gen Pro (Prompt to Video)", fontWeight = FontWeight.ExtraBold)
        }

        // Quick 1080P+ Auto Enhance Banner + Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .testTag("ai_lab_auto_enhance_btn")
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleAutoEnhance),
                color = if (project.adjustments.autoEnhanced) ElectricCyan else StudioSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (project.adjustments.autoEnhanced) ElectricCyan else StudioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Hd,
                        contentDescription = null,
                        tint = if (project.adjustments.autoEnhanced) Color(0xFF041418) else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (project.adjustments.autoEnhanced) "1080P+ Auto ON" else "Auto Enhance",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (project.adjustments.autoEnhanced) Color(0xFF041418) else TextPrimary
                    )
                }
            }

            AiLabMode.entries.forEach { m ->
                val selected = mode == m
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_mode_${m.name.lowercase()}")
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            mode = m
                            onResetAiState()
                        },
                    color = if (selected) VioletPulse.copy(alpha = 0.25f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) VioletPulse else StudioBorder)
                ) {
                    Text(
                        text = m.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        when (mode) {
            AiLabMode.ENHANCE -> {
                Button(
                    onClick = onRunAiVideoEnhance,
                    modifier = Modifier
                        .testTag("run_ai_enhance_button")
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPulse, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze Frame with AI (1080P+ Colorist)", fontWeight = FontWeight.Bold)
                }
            }

            AiLabMode.CAPTIONS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Topic (optional)...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onRunAiCaptions(promptInput) },
                        modifier = Modifier.testTag("run_ai_captions_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Subtitles, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate")
                    }
                }
            }

            AiLabMode.SCRIPT -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Hype", "Cinematic", "Tutorial", "Chill").forEach { tone ->
                        val active = tone == selectedTone
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTone = tone },
                            color = if (active) ElectricCyan else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tone,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color(0xFF041418) else TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Video idea or topic...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onStreamAiScript(promptInput, selectedTone) },
                        modifier = Modifier.testTag("run_ai_script_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
                    ) {
                        Text("Write", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Render AI Lab output state
        when (aiLabState) {
            is AiLabUiState.Idle -> Unit

            is AiLabUiState.Loading -> {
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(aiLabState.taskLabel, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            is AiLabUiState.EnhanceReady -> {
                val rec = aiLabState.recommendation
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(rec.summary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "Filter: ${rec.filterPreset.label} · Clarity ${(rec.clarity * 100).toInt()}% · Hook: \"${rec.hookCaption}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Button(
                            onClick = { onApplyAiEnhance(rec) },
                            modifier = Modifier
                                .testTag("apply_ai_enhance_button")
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
                        ) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply AI 1080P+ Grade", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AiLabUiState.CaptionsReady -> {
                val pack = aiLabState.captions
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("1. ${pack.hookTitle}", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        Text("2. ${pack.middleCaption}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("3. ${pack.outroCta}", style = MaterialTheme.typography.labelMedium, color = NeonRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onApplyAiCaptions(pack) },
                            modifier = Modifier
                                .testTag("apply_ai_captions_button")
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White)
                        ) {
                            Text("Insert All 3 onto Timeline", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is AiLabUiState.StreamingScript -> {
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiLabState.scriptText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            is AiLabUiState.ScriptReady -> {
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiLabState.scriptText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            is AiLabUiState.TranslatedReady -> {
                val tPack = aiLabState.translation
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${tPack.targetLanguage}: ${tPack.line1}", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        Text(tPack.line2, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }

            is AiLabUiState.Error -> {
                Surface(
                    color = NeonRose.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = NeonRose)
                        Text(
                            text = aiLabState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private enum class VoiceSliderKey(val label: String) {
    CLARITY("Voice Clarity"),
    DENOISE("Noise Reduce"),
    GAIN("Loudness"),
    WARMTH("Warm Bass"),
    PITCH("Pitch Shift")
}

@Composable
fun VoiceEnhancerPanel(
    voiceSettings: VoiceEnhanceSettings,
    onSelectPreset: (VoiceEnhancePreset) -> Unit,
    onUpdateSettings: (VoiceEnhanceSettings) -> Unit
) {
    var activeSlider by remember { mutableStateOf(VoiceSliderKey.CLARITY) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GraphicEq, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (voiceSettings.isActive) "Active: ${voiceSettings.preset.label}" else "Voice Enhancer Off",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
            TextButton(onClick = { onSelectPreset(VoiceEnhancePreset.NATURAL) }) {
                Icon(Icons.Filled.RestartAlt, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = ElectricCyan)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VoiceEnhancePreset.entries, key = { it.name }) { preset ->
                val selected = voiceSettings.preset == preset
                Surface(
                    modifier = Modifier
                        .testTag("voice_preset_${preset.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) },
                    color = if (selected) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(preset.tag, style = MaterialTheme.typography.labelSmall, color = if (selected) ElectricCyan else TextMuted)
                        Text(preset.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
            }
        }

        // Fine-tune DSP Sliders
        val valueLabel = when (activeSlider) {
            VoiceSliderKey.CLARITY -> "${(voiceSettings.clarityBoost * 100).toInt()}%"
            VoiceSliderKey.DENOISE -> "${(voiceSettings.noiseReduction * 100).toInt()}%"
            VoiceSliderKey.GAIN -> "${(voiceSettings.loudnessGain * 100).toInt()}%"
            VoiceSliderKey.WARMTH -> "${(voiceSettings.warmthBass * 100).toInt()}%"
            VoiceSliderKey.PITCH -> String.format("%.2fx", voiceSettings.pitchShift)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(activeSlider.label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        }

        when (activeSlider) {
            VoiceSliderKey.CLARITY -> Slider(
                value = voiceSettings.clarityBoost,
                onValueChange = { onUpdateSettings(voiceSettings.copy(clarityBoost = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            VoiceSliderKey.DENOISE -> Slider(
                value = voiceSettings.noiseReduction,
                onValueChange = { onUpdateSettings(voiceSettings.copy(noiseReduction = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            VoiceSliderKey.GAIN -> Slider(
                value = voiceSettings.loudnessGain,
                onValueChange = { onUpdateSettings(voiceSettings.copy(loudnessGain = it)) },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            VoiceSliderKey.WARMTH -> Slider(
                value = voiceSettings.warmthBass,
                onValueChange = { onUpdateSettings(voiceSettings.copy(warmthBass = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
            VoiceSliderKey.PITCH -> Slider(
                value = voiceSettings.pitchShift,
                onValueChange = { onUpdateSettings(voiceSettings.copy(pitchShift = it)) },
                valueRange = 0.75f..1.35f,
                colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VoiceSliderKey.entries) { key ->
                val selected = key == activeSlider
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeSlider = key },
                    color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = key.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ElectricCyan else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
