package com.example.ui.components

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CloudSyncStatus
import com.example.data.CloudSyncUiState
import com.example.data.NetworkAndPowerObserver
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

@Composable
fun CloudSyncStatusPill(
    syncState: CloudSyncUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        !syncState.isOnline || syncState.status == CloudSyncStatus.OFFLINE_QUEUED -> StudioAmber
        syncState.status == CloudSyncStatus.ERROR -> NeonRose
        syncState.status == CloudSyncStatus.SYNCING -> ElectricCyan
        syncState.status == CloudSyncStatus.SYNCED -> StudioEmerald
        else -> TextSecondary
    }
    val icon = when {
        !syncState.isOnline || syncState.status == CloudSyncStatus.OFFLINE_QUEUED -> Icons.Filled.CloudOff
        syncState.status == CloudSyncStatus.SYNCING -> Icons.Filled.CloudSync
        syncState.status == CloudSyncStatus.ERROR -> Icons.Filled.WarningAmber
        syncState.status == CloudSyncStatus.SYNCED -> Icons.Filled.CloudDone
        else -> Icons.Filled.CloudQueue
    }
    val label = when {
        !syncState.isOnline -> "Offline (${syncState.pendingCount})"
        syncState.status == CloudSyncStatus.SYNCING -> "Syncing..."
        syncState.pendingCount > 0 -> "Queue (${syncState.pendingCount})"
        else -> syncState.status.label
    }

    Surface(
        modifier = modifier
            .testTag("cloud_sync_status_pill")
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Cloud sync status",
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CloudSyncSettingsCard(
    syncState: CloudSyncUiState,
    onSyncNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        !syncState.isOnline || syncState.status == CloudSyncStatus.OFFLINE_QUEUED -> StudioAmber
        syncState.status == CloudSyncStatus.ERROR -> NeonRose
        syncState.status == CloudSyncStatus.SYNCING -> ElectricCyan
        syncState.status == CloudSyncStatus.SYNCED -> StudioEmerald
        else -> ElectricCyan
    }
    val statusIcon = when {
        !syncState.isOnline || syncState.status == CloudSyncStatus.OFFLINE_QUEUED -> Icons.Filled.CloudOff
        syncState.status == CloudSyncStatus.SYNCING -> Icons.Filled.CloudSync
        syncState.status == CloudSyncStatus.ERROR -> Icons.Filled.WarningAmber
        syncState.status == CloudSyncStatus.SYNCED -> Icons.Filled.CloudDone
        else -> Icons.Filled.CloudQueue
    }
    val lastSyncLabel = NetworkAndPowerObserver.formatLastSyncedTime(syncState.lastSyncedMs)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cloud_sync_settings_card"),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Cloud Sync",
                            tint = statusColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cloud Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (syncState.isOnline) StudioEmerald else StudioAmber)
                            )
                            Text(
                                text = if (syncState.isOnline) "Online · ${syncState.status.label}" else "Offline · Saved Locally",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                    }
                }

                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = "${syncState.syncedCloudCount}/${syncState.localDraftCount} drafts",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last synced: $lastSyncLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (syncState.pendingCount > 0) {
                        Text(
                            text = "${syncState.pendingCount} draft(s) queued to upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioAmber
                        )
                    }
                    if (!syncState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = syncState.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonRose,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-sync when online returns",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Automatically backs up offline drafts & edits",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = syncState.autoSyncEnabled,
                    onCheckedChange = onToggleAutoSync,
                    modifier = Modifier.testTag("cloud_sync_auto_switch")
                )
            }

            Button(
                onClick = onSyncNow,
                enabled = syncState.status != CloudSyncStatus.SYNCING,
                modifier = Modifier
                    .testTag("cloud_sync_now_button")
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF041418)
                )
            ) {
                if (syncState.status == CloudSyncStatus.SYNCING) {
                    CircularProgressIndicator(
                        color = Color(0xFF041418),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing Drafts...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (syncState.isOnline) "Sync Now" else "Queue Offline Drafts",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
