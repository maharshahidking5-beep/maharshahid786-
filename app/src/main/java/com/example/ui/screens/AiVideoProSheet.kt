package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ai.AiGeneratedVideoResult
import com.example.ai.AiVideoGenUiState
import com.example.ai.AiVideoVisualStyle
import com.example.model.AspectRatioOption
import com.example.ui.components.formatShortDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse

private val starterPrompts = listOf(
    "Neon sports car drifting in rainy Karachi midnight streets",
    "لاہور بادشاہی مسجد پر سنہری غروب آفتاب کا ڈرون ویڈیو",
    "Luxury royal oud perfume bottle with floating gold mist 3D ad",
    "Pakistan cricket stadium victory fireworks slow-motion reel"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVideoProSheet(
    genState: AiVideoGenUiState,
    history: List<AiGeneratedVideoResult>,
    hasProAccess: Boolean,
    trialUsedCount: Int,
    isAdminUser: Boolean,
    onGenerate: (prompt: String, style: AiVideoVisualStyle, aspectRatio: AspectRatioOption) -> Unit,
    onResetState: () -> Unit,
    onOpenInEditor: (AiGeneratedVideoResult) -> Unit,
    onOpenProPackages: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var promptText by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(AiVideoVisualStyle.CINEMATIC) }
    var selectedAspect by remember { mutableStateOf(AspectRatioOption.RATIO_9_16) }

    val trialExhausted = !hasProAccess && trialUsedCount >= 1
    val isGenerating = genState is AiVideoGenUiState.Generating

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ElectricCyan, VioletPulse))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(21.dp))
                    }
                    Column {
                        Text(
                            text = "AI Video Gen Pro",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Prompt to multi-scene reel in 1080P+",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = when {
                        hasProAccess -> StudioEmerald.copy(alpha = 0.18f)
                        trialExhausted -> NeonRose.copy(alpha = 0.18f)
                        else -> StudioAmber.copy(alpha = 0.18f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            hasProAccess -> StudioEmerald
                            trialExhausted -> NeonRose
                            else -> StudioAmber
                        }
                    ),
                    modifier = Modifier.clickable { onOpenProPackages() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasProAccess) Icons.Filled.WorkspacePremium else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = when {
                                hasProAccess -> StudioEmerald
                                trialExhausted -> NeonRose
                                else -> StudioAmber
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when {
                                hasProAccess -> "PRO ACTIVE"
                                trialExhausted -> "GET PRO"
                                else -> "1 FREE TRIAL"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = when {
                                hasProAccess -> StudioEmerald
                                trialExhausted -> NeonRose
                                else -> StudioAmber
                            }
                        )
                    }
                }
            }

            // Admin Quick Shortcut (Visible strictly for designated Admin Gmail)
            if (isAdminUser) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_sheet_admin_shortcut")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAdminConsole() },
                    color = VioletPulse.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VioletPulse.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, null, tint = VioletPulse, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Admin Console: JazzCash / EasyPaisa Numbers & Approvals",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Prompt Text Input
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("Describe your AI video (Urdu, Hindi or English)") },
                placeholder = { Text("e.g. Rainy cyberpunk Lahore car chase with golden Urdu captions...") },
                minLines = 3,
                maxLines = 5,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_video_prompt_input")
            )

            // Quick Prompt Ideas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                starterPrompts.forEachIndexed { idx, idea ->
                    Surface(
                        modifier = Modifier
                            .testTag("ai_prompt_chip_$idx")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !isGenerating) { promptText = idea },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = idea.take(36) + if (idea.length > 36) "…" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Visual Style Selector
            Text("Visual Style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiVideoVisualStyle.entries.forEach { style ->
                    val active = selectedStyle == style
                    Surface(
                        modifier = Modifier
                            .testTag("ai_style_${style.name.lowercase()}")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isGenerating) { selectedStyle = style },
                        color = if (active) ElectricCyan.copy(alpha = 0.18f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (active) ElectricCyan else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Aspect Ratio Selector
            Text("Video Size / Aspect Ratio", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AspectRatioOption.RATIO_9_16 to "9:16 Reels/TikTok",
                    AspectRatioOption.RATIO_16_9 to "16:9 YouTube",
                    AspectRatioOption.RATIO_1_1 to "1:1 Square"
                ).forEach { (ratio, label) ->
                    val active = selectedAspect == ratio
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isGenerating) { selectedAspect = ratio },
                        color = if (active) VioletPulse.copy(alpha = 0.22f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (active) VioletPulse else StudioBorder)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color.White else TextSecondary,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Primary Action CTA
            if (trialExhausted) {
                Button(
                    onClick = onOpenProPackages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("ai_video_upgrade_cta"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAmber,
                        contentColor = Color(0xFF140D02)
                    )
                ) {
                    Icon(Icons.Filled.WorkspacePremium, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy AI Pro Package (JazzCash / EasyPaisa)", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onGenerate(promptText, selectedStyle, selectedAspect) },
                        enabled = !isGenerating && promptText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ai_video_generate_cta"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041418)
                        )
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGenerating) "Generating AI Video..." else "Generate AI Pro Video",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (!hasProAccess) {
                        OutlinedButton(
                            onClick = onOpenProPackages,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("ai_video_packages_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f))
                        ) {
                            Text("Pro Plans", color = StudioAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Generation Progress / Result / Error Card
            when (genState) {
                is AiVideoGenUiState.Idle -> Unit

                is AiVideoGenUiState.Generating -> {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = genState.stageLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ElectricCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(genState.progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { genState.progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ElectricCyan,
                                trackColor = StudioSurface
                            )
                            Text(
                                text = "Generating storyboard, multi-scene visuals, beat markers & captions...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is AiVideoGenUiState.Error -> {
                    Surface(
                        color = NeonRose.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = NeonRose)
                                Text(
                                    text = genState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onGenerate(promptText, selectedStyle, selectedAspect) },
                                    border = BorderStroke(1.dp, ElectricCyan)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry", color = ElectricCyan)
                                }
                                if (!hasProAccess) {
                                    TextButton(onClick = onOpenProPackages) {
                                        Text("View JazzCash / EasyPaisa Plans", color = StudioAmber)
                                    }
                                }
                            }
                        }
                    }
                }

                is AiVideoGenUiState.Ready -> {
                    val result = genState.result
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_video_ready_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = StudioEmerald, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(
                                            text = result.videoTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${result.scenes.size} AI Scenes · ${formatShortDuration(result.project.totalDurationMs)} · ${result.aspectRatio.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioEmerald
                                        )
                                    }
                                }
                            }

                            // 3-Scene Visual Strip Preview
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(result.scenes, key = { it.sceneIndex }) { scene ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 112.dp, height = 148.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(StudioSurface)
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(scene.imageUriString),
                                            contentDescription = "AI Scene ${scene.sceneIndex}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                                    )
                                                )
                                        )
                                        Text(
                                            text = "S${scene.sceneIndex} · ${(scene.durationMs / 1000f)}s",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = ElectricCyan,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                        )
                                        if (scene.captionText.isNotBlank()) {
                                            Text(
                                                text = scene.captionText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onOpenInEditor(result)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("ai_video_open_editor_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioEmerald,
                                        contentColor = Color(0xFF041418)
                                    )
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play & Export in Editor", fontWeight = FontWeight.ExtraBold)
                                }
                                OutlinedButton(
                                    onClick = { onResetState() },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, StudioBorder)
                                ) {
                                    Text("New", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Recent AI Generated Videos
            if (history.isNotEmpty()) {
                Text(
                    text = "Recent AI Videos (${history.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history, key = { it.generationId }) { item ->
                        Surface(
                            modifier = Modifier
                                .width(172.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onOpenInEditor(item)
                                    onDismiss()
                                },
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.MovieCreation, null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.videoTitle,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.style.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
