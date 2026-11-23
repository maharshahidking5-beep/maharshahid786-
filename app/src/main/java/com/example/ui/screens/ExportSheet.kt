package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.ExportEntity
import com.example.media.ExportUiState
import com.example.media.SmartMediaHelpers
import com.example.model.ExportResolution
import com.example.model.ProjectData
import com.example.ui.components.formatFileSize
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
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    project: ProjectData,
    exportState: ExportUiState,
    isBatterySaver: Boolean = false,
    onUpdateExportSettings: (ExportResolution, Int) -> Unit,
    onToggleWatermark: (Boolean) -> Unit = {},
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onPublishToTikTok: ((ExportEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewCompletedItem by remember { mutableStateOf<ExportEntity?>(null) }
    var galleryStatusText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportUiState.Exporting) onDismiss()
        },
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (exportState) {
                is ExportUiState.Idle -> {
                    Text(
                        text = "Export Video",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${project.clips.size} clips · ${formatShortDuration(project.totalDurationMs)} · ${project.aspectRatio.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Text("Resolution & Quality (360P – 4K)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportResolution.entries.chunked(3).forEach { rowRes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowRes.forEach { res ->
                                    val selected = project.resolution == res
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_res_${res.name.lowercase()}")
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onUpdateExportSettings(res, project.fps) },
                                        color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = res.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) ElectricCyan else TextPrimary
                                            )
                                            Text(
                                                text = res.bitrateLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isBatterySaver && project.resolution == ExportResolution.RES_2K) {
                        Surface(
                            color = StudioAmber.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Battery Saver ON: 1080P or 720P renders much faster with cooler battery.",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Text("Frame Rate", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(24, 30, 60).forEach { fps ->
                            val selected = project.fps == fps
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateExportSettings(project.resolution, fps) },
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                            ) {
                                Text(
                                    text = "$fps fps",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ElectricCyan else TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("s.s Studio Watermark", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Text(
                                    text = if (project.addWatermark) "Included in bottom-right corner" else "Clean export · Zero watermark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = project.addWatermark,
                                onCheckedChange = onToggleWatermark,
                                modifier = Modifier.testTag("export_watermark_switch")
                            )
                        }
                    }

                    if (project.adjustments.autoEnhanced || project.voiceEnhance.isActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (project.adjustments.autoEnhanced) {
                                Surface(
                                    color = StudioEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "1080P+ Auto HD On",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (project.voiceEnhance.isActive) {
                                Surface(
                                    color = ElectricCyan.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Voice: ${project.voiceEnhance.preset.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onStartExport,
                        modifier = Modifier
                            .testTag("confirm_start_export_button")
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041418)
                        )
                    ) {
                        Text(
                            text = "Export MP4 (${project.resolution.label})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                is ExportUiState.Exporting -> {
                    Text(
                        text = "Exporting ${project.title}...",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = exportState.stageLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                    LinearProgressIndicator(
                        progress = { exportState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = ElectricCyan,
                        trackColor = StudioSurfaceElevated
                    )
                    Text(
                        text = "${exportState.progressPercent}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    OutlinedButton(
                        onClick = onCancelExport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonRose)
                    ) {
                        Text("Cancel Export", color = NeonRose)
                    }
                }

                is ExportUiState.Completed -> {
                    val entity = exportState.exportEntity
                    val exportFile = remember(entity.filePath) { File(entity.filePath) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StudioEmerald.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Export completed",
                                tint = StudioEmerald,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text("Ready to Share!", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = "${entity.resolutionLabel} · ${formatFileSize(entity.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        galleryStatusText?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.labelSmall, color = StudioEmerald)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { previewCompletedItem = entity },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioBorder)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Watch", color = TextPrimary)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = SmartMediaHelpers.saveVideoToGallery(context, exportFile, entity.projectTitle)
                                        galleryStatusText = if (res.isSuccess) "Saved to Movies/s.s_Studio!" else "Could not save to Gallery"
                                    }
                                },
                                modifier = Modifier
                                    .testTag("export_save_gallery_button")
                                    .weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioEmerald,
                                    contentColor = Color(0xFF041418)
                                )
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Instant Social Share Shortcuts: WhatsApp, Instagram, YouTube, System Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShareShortcutChip("WhatsApp", ElectricCyan, Modifier.weight(1f)) {
                                SmartMediaHelpers.shareVideoShortcut(context, exportFile, "com.whatsapp", "Share to WhatsApp")
                            }
                            ShareShortcutChip("Instagram", NeonRose, Modifier.weight(1f)) {
                                SmartMediaHelpers.shareVideoShortcut(context, exportFile, "com.instagram.android", "Share to Instagram")
                            }
                            ShareShortcutChip("YouTube", StudioAmber, Modifier.weight(1f)) {
                                SmartMediaHelpers.shareVideoShortcut(context, exportFile, "com.google.android.youtube", "Share to YouTube")
                            }
                            ShareShortcutChip("More", TextPrimary, Modifier.weight(0.9f)) {
                                if (exportFile.exists()) {
                                    val shareUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        exportFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share s.s Video"))
                                }
                            }
                        }

                        if (onPublishToTikTok != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onPublishToTikTok(entity)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRose,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Publish, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post to TikTok Feed", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", color = TextSecondary)
                        }
                    }
                }

                is ExportUiState.Failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = "Export error",
                            tint = NeonRose,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("Export Failed", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onStartExport,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
                        ) {
                            Text("Retry Export")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    previewCompletedItem?.let { item ->
        ExportVideoPreviewDialog(
            exportItem = item,
            onDismiss = { previewCompletedItem = null }
        )
    }
}

@Composable
private fun ShareShortcutChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Share, contentDescription = label, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1)
        }
    }
}

