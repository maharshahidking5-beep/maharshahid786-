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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.ClipAnimationType
import com.example.model.EffectCategory
import com.example.model.FilterCategory
import com.example.model.FilterPreset
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

@Composable
fun EffectsPanel(
    selected: VideoEffectPreset,
    intensity: Float,
    onSelect: (VideoEffectPreset, Float) -> Unit
) {
    var activeCategory by remember { mutableStateOf(EffectCategory.ALL) }
    val filteredEffects = remember(activeCategory) {
        if (activeCategory == EffectCategory.ALL) VideoEffectPreset.entries
        else listOf(VideoEffectPreset.NONE) + VideoEffectPreset.entries.filter { it.category == activeCategory }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        // Category Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EffectCategory.entries, key = { it.name }) { cat ->
                val isSelected = cat == activeCategory
                Surface(
                    modifier = Modifier
                        .testTag("effect_cat_${cat.name.lowercase()}")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activeCategory = cat },
                    color = if (isSelected) NeonRose else StudioSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSelected) NeonRose else StudioBorder)
                ) {
                    Text(
                        text = cat.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Intensity slider when an effect is active
        if (selected != VideoEffectPreset.NONE) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intensity ${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = intensity,
                    onValueChange = { onSelect(selected, it) },
                    valueRange = 0.15f..1f,
                    colors = SliderDefaults.colors(thumbColor = NeonRose, activeTrackColor = NeonRose),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Effect Preset Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredEffects, key = { it.name }) { fx ->
                val isSelected = fx == selected
                val badgeTint = when (fx.category) {
                    EffectCategory.NIGHTCLUB -> NeonRose
                    EffectCategory.RETRO -> StudioAmber
                    EffectCategory.LENS -> ElectricCyan
                    EffectCategory.LIGHT -> StudioEmerald
                    EffectCategory.GLITCH -> VioletPulse
                    EffectCategory.ZOOM_FX -> NeonRose
                    EffectCategory.TRENDING, EffectCategory.ALL -> ElectricCyan
                }
                Surface(
                    modifier = Modifier
                        .testTag("effect_preset_${fx.name.lowercase()}")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(fx, intensity) },
                    color = if (isSelected) badgeTint.copy(alpha = 0.20f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, if (isSelected) badgeTint else StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = if (isSelected) badgeTint else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = fx.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) badgeTint else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(fx.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(
    selected: FilterPreset,
    intensity: Float,
    onSelect: (FilterPreset, Float) -> Unit
) {
    var activeCategory by remember { mutableStateOf(FilterCategory.ALL) }
    val filteredPresets = remember(activeCategory) {
        if (activeCategory == FilterCategory.ALL) FilterPreset.entries
        else listOf(FilterPreset.ORIGINAL) + FilterPreset.entries.filter { it.category == activeCategory }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterCategory.entries, key = { it.name }) { cat ->
                val isSelected = cat == activeCategory
                Surface(
                    modifier = Modifier
                        .testTag("filter_cat_${cat.name.lowercase()}")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activeCategory = cat },
                    color = if (isSelected) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSelected) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = cat.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color(0xFF041418) else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

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
            items(filteredPresets, key = { it.name }) { preset ->
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

@Composable
fun AnimateClipPanel(
    clip: VideoClip?,
    onSelectAnimation: (String, ClipAnimationType, Long) -> Unit
) {
    if (clip == null) {
        Text(
            text = "Select a clip on the timeline to animate.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Animation, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
            Text(
                text = "In / Combo Animation · ${clip.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
        if (clip.animation != ClipAnimationType.NONE) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration ${String.format("%.1fs", clip.animationDurationMs / 1000f)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Slider(
                    value = clip.animationDurationMs.toFloat(),
                    onValueChange = { onSelectAnimation(clip.id, clip.animation, it.toLong()) },
                    valueRange = 300f..2500f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ClipAnimationType.entries, key = { it.name }) { anim ->
                val isSelected = clip.animation == anim
                Surface(
                    modifier = Modifier
                        .testTag("clip_anim_${anim.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectAnimation(clip.id, anim, clip.animationDurationMs) },
                    color = if (isSelected) ElectricCyan.copy(alpha = 0.20f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isSelected) ElectricCyan else StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(anim.tag, style = MaterialTheme.typography.labelSmall, color = if (isSelected) ElectricCyan else TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(anim.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}
