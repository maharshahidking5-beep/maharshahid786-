package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CloudSyncUiState
import com.example.data.StudioPreferences
import com.example.media.QuickPresetPack
import com.example.media.WorkflowHelpersCatalog
import com.example.model.EditorToolTab
import com.example.model.ExportResolution
import com.example.model.ProjectData
import com.example.ui.components.CloudSyncSettingsCard
import com.example.ui.components.CloudSyncStatusPill
import com.example.ui.components.SsWordmarkBadge
import com.example.ui.components.formatShortDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickToolShortcut(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHubScreen(
    projects: List<ProjectData>,
    preferences: StudioPreferences,
    cloudSyncState: CloudSyncUiState,
    onSyncNow: () -> Unit,
    onPickNewProject: (autoCut: Boolean, initialTool: EditorToolTab) -> Unit,
    onLaunchDemoProject: (initialTool: EditorToolTab) -> Unit,
    onLaunchQuickPresetPack: (QuickPresetPack) -> Unit,
    onOpenProject: (ProjectData) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (ProjectData) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenExportsTab: () -> Unit,
    onOpenSsAssistant: () -> Unit,
    onOpenMiniGuides: () -> Unit,
    onOpenBatchPhotoTool: () -> Unit,
    onUpdatePreferences: (
        defaultResolution: ExportResolution?,
        defaultFps: Int?,
        addWatermark: Boolean?,
        autoSaveDrafts: Boolean?,
        batterySaverMode: Boolean?,
        autoSyncOnline: Boolean?,
        appThemeMode: String?,
        customBgUri: String?,
        customBgBlur: Float?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectData?>(null) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdatePreferences(null, null, null, null, null, null, "photo", uri.toString(), null)
        }
    }

    val shortcuts = remember(projects, preferences.batterySaverMode) {
        listOf(
            QuickToolShortcut("Effects", Icons.Filled.AutoFixHigh, ElectricCyan) {
                onLaunchDemoProject(EditorToolTab.EFFECTS)
            },
            QuickToolShortcut("Voice FX", Icons.Filled.GraphicEq, NeonRose) {
                onLaunchDemoProject(EditorToolTab.VOICE_FX)
            },
            QuickToolShortcut("Auto HD", Icons.Filled.Hd, StudioEmerald) {
                onLaunchDemoProject(EditorToolTab.ADJUST)
            },
            QuickToolShortcut("AutoCut", Icons.Filled.ContentCut, StudioAmber) {
                onPickNewProject(true, EditorToolTab.ROOT)
            },
            QuickToolShortcut("Batch Img", Icons.Filled.Collections, VioletPulse) {
                onOpenBatchPhotoTool()
            },
            QuickToolShortcut("SS Help", Icons.Filled.AutoAwesome, ElectricCyan) {
                onOpenSsAssistant()
            },
            QuickToolShortcut("Guides", Icons.AutoMirrored.Filled.HelpOutline, StudioAmber) {
                onOpenMiniGuides()
            },
            QuickToolShortcut("Exports", Icons.Filled.VideoFile, TextPrimary) {
                onOpenExportsTab()
            },
            QuickToolShortcut("Filters", Icons.Filled.FilterVintage, NeonRose) {
                onLaunchDemoProject(EditorToolTab.FILTERS)
            },
            QuickToolShortcut("AI Lab", Icons.Filled.AutoAwesome, VioletPulse) {
                onLaunchDemoProject(EditorToolTab.AI_LAB)
            },
            QuickToolShortcut("Urdu/HI", Icons.Filled.Subtitles, ElectricCyan) {
                onLaunchDemoProject(EditorToolTab.TEXT)
            },
            QuickToolShortcut(
                if (preferences.batterySaverMode) "Eco ON" else "Eco Mode",
                Icons.Filled.BatterySaver,
                StudioEmerald
            ) {
                onUpdatePreferences(null, null, null, null, !preferences.batterySaverMode, null, null, null, null)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (preferences.appThemeMode == "photo") Color.Transparent else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // Header Bar with Cloud Sync Status Pill, Mini Guides, SS Assistant & Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SsWordmarkBadge()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CloudSyncStatusPill(
                            syncState = cloudSyncState,
                            onClick = { showSettingsSheet = true }
                        )
                        IconButton(
                            onClick = onOpenMiniGuides,
                            modifier = Modifier
                                .testTag("hub_mini_guides_button")
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Mini editing guides",
                                tint = TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSsAssistant,
                            modifier = Modifier
                                .testTag("hub_ss_assistant_button")
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Open SS Assistant",
                                tint = ElectricCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .testTag("studio_settings_button")
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Studio settings",
                                tint = TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            item {
                // Hero New Project Card (CapCut-style prominent creation card)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E2936), Color(0xFF13151B), Color(0xFF281226))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(ElectricCyan.copy(alpha = 0.45f), NeonRose.copy(alpha = 0.35f))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.32f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Create without limits",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "24+ CapCut effects, 1080P+ Auto HD, Gemini AI Lab & Voice FX",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { onPickNewProject(false, EditorToolTab.ROOT) },
                            modifier = Modifier
                                .testTag("new_project_button")
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF041418)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New project",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onLaunchDemoProject(EditorToolTab.ROOT) },
                            modifier = Modifier
                                .testTag("demo_project_button")
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioSurface.copy(alpha = 0.72f),
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Try Studio Demo Reel",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item {
                // One-Tap Quick Preset Packs (Vlog Ready, Wedding Glow, Gaming Punch, Product Ad)
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Preset Packs",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "One-tap grade",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(WorkflowHelpersCatalog.quickPresetPacks, key = { it.id }) { pack ->
                        Surface(
                            modifier = Modifier
                                .testTag("quick_preset_${pack.id}")
                                .width(196.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onLaunchQuickPresetPack(pack) },
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(pack.accentHex).copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = Color(pack.accentHex).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = pack.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(pack.accentHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = pack.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = pack.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Quick Studio Tools 2x4 Grid
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Studio Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    shortcuts.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("shortcut_${item.label.lowercase()}")
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(onClick = item.action),
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.55f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(item.tint.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                tint = item.tint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(26.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Projects (${projects.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (projects.isNotEmpty()) {
                        Text(
                            text = "Tap to resume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (projects.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = StudioSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MovieFilter,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No drafts yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your video edits, trims, and color grades will auto-save right here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    DraftProjectRow(
                        project = project,
                        onOpen = { onOpenProject(project) },
                        onRename = { renameTarget = project },
                        onDuplicate = { onDuplicateProject(project) },
                        onDelete = { onDeleteProject(project.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "s.s Studio Settings & Cloud Sync",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                CloudSyncSettingsCard(
                    syncState = cloudSyncState,
                    onSyncNow = onSyncNow,
                    onToggleAutoSync = { enabled ->
                        onUpdatePreferences(null, null, null, null, null, enabled, null, null, null)
                    }
                )

                // Theme & Custom Photo Wallpaper
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Studio Theme & Wallpaper",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dark" to "Dark", "light" to "Light", "photo" to "Custom Photo").forEach { (modeKey, modeLabel) ->
                            val selected = preferences.appThemeMode == modeKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_$modeKey")
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (modeKey == "photo" && preferences.customBgUri.isBlank()) {
                                            wallpaperPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } else {
                                            onUpdatePreferences(null, null, null, null, null, null, modeKey, null, null)
                                        }
                                    },
                                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = modeLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ElectricCyan else TextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                    if (preferences.appThemeMode == "photo") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    wallpaperPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.Wallpaper, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Photo", color = ElectricCyan)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Blur: ${preferences.customBgBlur.toInt()}dp", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Slider(
                                    value = preferences.customBgBlur,
                                    onValueChange = {
                                        onUpdatePreferences(null, null, null, null, null, null, null, null, it)
                                    },
                                    valueRange = 0f..12f,
                                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Default Export Resolution (360P – 4K)",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.entries.chunked(3).forEach { rowRes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowRes.forEach { res ->
                                val selected = preferences.defaultResolution == res
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onUpdatePreferences(res, null, null, null, null, null, null, null, null)
                                        },
                                    color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder),
                                    shape = RoundedCornerShape(12.dp)
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

                Text(
                    text = "Default Frame Rate",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(24, 30, 60).forEach { fpsOption ->
                        val selected = preferences.defaultFps == fpsOption
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdatePreferences(null, fpsOption, null, null, null, null, null, null, null)
                                },
                            color = if (selected) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$fpsOption fps",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ElectricCyan else TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
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
                        Text("Battery & Low-End Saver Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Reduces preview blur & frame overhead for smooth editing", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = preferences.batterySaverMode,
                        onCheckedChange = { onUpdatePreferences(null, null, null, null, it, null, null, null, null) },
                        modifier = Modifier.testTag("settings_battery_saver_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default s.s Watermark", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Toggle clean export or s.s Studio corner mark", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = preferences.addEndingWatermark,
                        onCheckedChange = { onUpdatePreferences(null, null, it, null, null, null, null, null, null) },
                        modifier = Modifier.testTag("settings_watermark_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-save drafts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Save every timeline edit offline locally", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = preferences.autoSaveDrafts,
                        onCheckedChange = { onUpdatePreferences(null, null, null, it, null, null, null, null, null) },
                        modifier = Modifier.testTag("settings_autosave_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = StudioSurfaceElevated,
            title = { Text("Rename project", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("Project name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameProject(target.id, text)
                    renameTarget = null
                }) {
                    Text("Save", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DraftProjectRow(
    project: ProjectData,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = remember(project.updatedAtMs) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(project.updatedAtMs))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                val thumbUri = project.coverConfig.coverImageUri ?: project.clips.firstOrNull()?.uriString
                if (thumbUri != null) {
                    AsyncImage(
                        model = Uri.parse(thumbUri),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                }
                if (project.coverConfig.coverImageUri != null || project.coverConfig.coverTitle.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(project.coverConfig.accentHex))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "COVER",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF041418)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatShortDuration(project.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${project.clips.size} clips · ${project.aspectRatio.label} · ${project.resolution.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Project options",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(StudioSurfaceHighlight)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Editor", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ElectricCyan) },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null, tint = TextSecondary) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = TextSecondary) },
                        onClick = { menuExpanded = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = NeonRose) },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = NeonRose) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}
