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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.ExportEntity
import com.example.media.ExportUiState
import com.example.model.ExportResolution
import com.example.model.ProjectData
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatShortDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    project: ProjectData,
    exportState: ExportUiState,
    onUpdateExportSettings: (ExportResolution, Int) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewCompletedItem by remember { mutableStateOf<ExportEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportUiState.Exporting) onDismiss()
        },
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

                    Text("Resolution", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportResolution.entries.forEach { res ->
                            val selected = project.resolution == res
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateExportSettings(res, project.fps) },
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
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
                                        color = TextMuted
                                    )
                                }
                            }
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
                                    val file = File(entity.filePath)
                                    if (file.exists()) {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share s.s Video"))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF041418)
                                )
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
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
