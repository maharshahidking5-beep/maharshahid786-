package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.ExportEntity
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatShortDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportsScreen(
    exports: List<ExportEntity>,
    onDeleteExport: (ExportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playingExport by remember { mutableStateOf<ExportEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "Exported Videos",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Rendered MP4 files ready to play or share",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (exports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No exported videos yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open any project and tap Export in the top-right corner to render an MP4.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(exports, key = { it.id }) { item ->
                    val dateLabel = remember(item.createdAtMs) {
                        SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(item.createdAtMs))
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_item_${item.id}")
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { playingExport = item },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StudioSurfaceHighlight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play exported video",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.projectTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatShortDuration(item.durationMs)} · ${item.resolutionLabel} · ${formatFileSize(item.fileSizeBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            IconButton(
                                onClick = {
                                    val file = File(item.filePath)
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
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = ElectricCyan)
                            }
                            IconButton(
                                onClick = { onDeleteExport(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = NeonRose)
                            }
                        }
                    }
                }
            }
        }
    }

    playingExport?.let { exportItem ->
        ExportVideoPreviewDialog(
            exportItem = exportItem,
            onDismiss = { playingExport = null }
        )
    }
}

@Composable
fun ExportVideoPreviewDialog(
    exportItem: ExportEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(exportItem.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(exportItem.filePath))))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exportItem.projectTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player.setVideoTextureView(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
