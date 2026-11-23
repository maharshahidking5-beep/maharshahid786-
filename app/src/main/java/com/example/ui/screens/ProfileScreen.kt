package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.CloudSyncUiState
import com.example.data.ExportEntity
import com.example.media.SmartMediaHelpers
import com.example.model.CanvaDesignDoc
import com.example.model.CanvaProject
import com.example.model.CloudProjectSummaryDoc
import com.example.model.SocialVideoPostDoc
import com.example.model.UserProfileDoc
import com.example.ui.components.CloudSyncSettingsCard
import com.example.ui.components.formatFileSize
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
import com.example.viewmodel.ProfileUiState
import kotlinx.coroutines.launch
import java.io.File

private enum class ProfileSubTab(val label: String) {
    POSTS("Posts"),
    CLOUD_CAPCUT("CapCut Cloud"),
    CANVA("Canva"),
    SAVED("Saved"),
    EXPORTS("Exports")
}

@Composable
fun ProfileScreen(
    profileUiState: ProfileUiState,
    profile: UserProfileDoc?,
    privateEmail: String,
    linkedPhone: String,
    cloudSyncState: CloudSyncUiState,
    useTransparentBg: Boolean = false,
    myPosts: List<SocialVideoPostDoc>,
    allFeedPosts: List<SocialVideoPostDoc>,
    savedPostIds: Set<String>,
    cloudProjects: List<CloudProjectSummaryDoc>,
    canvaDesigns: List<CanvaDesignDoc>,
    exports: List<ExportEntity>,
    decodeCanvaDesign: (String) -> CanvaProject?,
    onUpdateProfile: (displayName: String, handle: String, bio: String) -> Unit,
    onSavePhone: (String) -> Unit,
    onOpenSsAssistant: () -> Unit,
    onSyncAllNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onOpenFeedTab: () -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenCloudCapCutProject: (CloudProjectSummaryDoc) -> Unit,
    onOpenCanvaDesign: (CanvaProject) -> Unit,
    onDeleteCanvaDesign: (String) -> Unit,
    onPublishExportToTikTok: (ExportEntity) -> Unit,
    onDeleteExport: (ExportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(ProfileSubTab.POSTS) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var playingExport by remember { mutableStateOf<ExportEntity?>(null) }
    var galleryBannerMsg by remember { mutableStateOf<String?>(null) }

    val savedPosts = remember(allFeedPosts, savedPostIds) {
        allFeedPosts.filter { savedPostIds.contains(it.id) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (useTransparentBg) Color.Transparent else StudioBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Creator Profile",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .testTag("profile_edit_button")
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.Filled.Edit, "Edit profile", tint = ElectricCyan)
                    }
                    IconButton(
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier
                            .testTag("profile_sign_out_button")
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Sign out", tint = NeonRose)
                    }
                }
            }
        }

        item {
            // Profile Hero Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                if (profileUiState is ProfileUiState.Loading && profile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                } else {
                    val p = profile ?: UserProfileDoc()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricCyan, NeonRose)))
                                .border(2.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (p.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = p.photoUrl,
                                    contentDescription = p.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = p.displayName.take(1).uppercase().ifBlank { "S" },
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF041418)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = p.displayName.ifBlank { "s.s Creator" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = p.handle.ifBlank { "@creator" },
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }

                        if (privateEmail.isNotBlank()) {
                            Surface(
                                color = StudioSurface,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.Email, null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = privateEmail,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (p.bio.isNotBlank()) {
                            Text(
                                text = p.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatColumn(p.followingCount.toString(), "Following")
                            ProfileStatColumn(p.followersCount.toString(), "Followers")
                            ProfileStatColumn(p.likesCount.toString(), "Likes")
                        }
                    }
                }
            }
        }

        item {
            // Creator Monetization Tracker (1,000 Followers + 100,000 30-Day Views) & Phone Link
            val p = profile ?: UserProfileDoc()
            val followersCount = p.followersCount
            val views30d = remember(myPosts) {
                myPosts.sumOf { (it.likesCount * 14L) + (it.sharesCount * 28L) + (it.savesCount * 18L) + 1200L }
            }
            val eligible = followersCount >= 1000 && views30d >= 100_000L

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("monetization_tracker_card"),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (eligible) StudioEmerald.copy(alpha = 0.6f) else StudioAmber.copy(alpha = 0.45f))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.MonetizationOn, null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Creator Monetization",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        Surface(
                            color = (if (eligible) StudioEmerald else StudioAmber).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (eligible) "Eligible" else "In Progress",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (eligible) StudioEmerald else StudioAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text("Followers: $followersCount / 1,000", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    LinearProgressIndicator(
                        progress = { (followersCount / 1000f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ElectricCyan,
                        trackColor = StudioSurface
                    )

                    Text("30-Day Views: $views30d / 100,000", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    LinearProgressIndicator(
                        progress = { (views30d / 100_000f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = NeonRose,
                        trackColor = StudioSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPhoneDialog = true },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Phone, null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (linkedPhone.isNotBlank()) linkedPhone else "Link Phone",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (linkedPhone.isNotBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$linkedPhone")))
                                } else {
                                    showPhoneDialog = true
                                }
                            },
                            border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Call, null, tint = StudioEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audio", style = MaterialTheme.typography.labelSmall, color = StudioEmerald)
                        }
                        OutlinedButton(
                            onClick = onOpenSsAssistant,
                            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Videocam, null, tint = NeonRose, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Studio Call", style = MaterialTheme.typography.labelSmall, color = NeonRose)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSubTab.entries.forEach { tab ->
                    val active = selectedTab == tab
                    val countSuffix = when (tab) {
                        ProfileSubTab.POSTS -> " (${myPosts.size})"
                        ProfileSubTab.CLOUD_CAPCUT -> " (${cloudProjects.size})"
                        ProfileSubTab.CANVA -> " (${canvaDesigns.size})"
                        ProfileSubTab.SAVED -> " (${savedPosts.size})"
                        ProfileSubTab.EXPORTS -> " (${exports.size})"
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedTab = tab },
                        color = if (active) ElectricCyan else StudioSurfaceElevated,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                    ) {
                        Text(
                            text = "${tab.label}$countSuffix",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color(0xFF041418) else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when (selectedTab) {
            ProfileSubTab.POSTS -> {
                if (myPosts.isEmpty()) {
                    item {
                        EmptyProfileSection(
                            title = "No TikTok Posts Yet",
                            subtitle = "Publish a reel or Canva design to share on the live feed.",
                            ctaLabel = "Go to TikTok Feed",
                            onCta = onOpenFeedTab
                        )
                    }
                } else {
                    items(myPosts, key = { it.id }) { post ->
                        ProfilePostRow(
                            post = post,
                            onOpenFeed = onOpenFeedTab,
                            onDelete = { onDeletePost(post.id) }
                        )
                    }
                }
            }

            ProfileSubTab.CLOUD_CAPCUT -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        CloudSyncSettingsCard(
                            syncState = cloudSyncState,
                            onSyncNow = onSyncAllNow,
                            onToggleAutoSync = onToggleAutoSync
                        )
                    }
                }
                if (cloudProjects.isEmpty()) {
                    item {
                        EmptyProfileSection(
                            title = "No Cloud CapCut Projects Yet",
                            subtitle = "Tap Sync Now above to upload your offline local drafts to your Gmail cloud account."
                        )
                    }
                } else {
                    items(cloudProjects, key = { it.id }) { doc ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenCloudCapCutProject(doc) },
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricCyan.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MovieCreation, null, tint = ElectricCyan)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title.ifBlank { "Untitled Edit" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(
                                        text = "${doc.clipCount} clips · ${doc.aspectLabel} · ${doc.resolutionLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(Icons.Filled.CloudDone, "Synced", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            ProfileSubTab.CANVA -> {
                if (canvaDesigns.isEmpty()) {
                    item {
                        EmptyProfileSection(
                            title = "No Saved Canva Designs",
                            subtitle = "Create posters, thumbnails, and logos in Canva Pro Studio and save them to your cloud account."
                        )
                    }
                } else {
                    items(canvaDesigns, key = { it.id }) { doc ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val decoded = decodeCanvaDesign(doc.designJson) ?: CanvaProject(
                                        id = doc.id,
                                        title = doc.title,
                                        category = doc.category,
                                        bgColorHex = doc.bgHex
                                    )
                                    onOpenCanvaDesign(decoded)
                                },
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(doc.accentHex).copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(doc.bgHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Brush, null, tint = Color(doc.accentHex))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title.ifBlank { "Canva Design" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${doc.category} · ${doc.aspectLabel}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                IconButton(onClick = { onDeleteCanvaDesign(doc.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Delete design", tint = NeonRose)
                                }
                            }
                        }
                    }
                }
            }

            ProfileSubTab.SAVED -> {
                if (savedPosts.isEmpty()) {
                    item {
                        EmptyProfileSection(
                            title = "No Bookmarked Reels",
                            subtitle = "Tap the Bookmark icon on any TikTok reel to save it here."
                        )
                    }
                } else {
                    items(savedPosts, key = { it.id }) { post ->
                        ProfilePostRow(
                            post = post,
                            onOpenFeed = onOpenFeedTab,
                            onDelete = null
                        )
                    }
                }
            }

            ProfileSubTab.EXPORTS -> {
                galleryBannerMsg?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioEmerald,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
                if (exports.isEmpty()) {
                    item {
                        EmptyProfileSection(
                            title = "No Local Exports Yet",
                            subtitle = "Render an MP4 from CapCut Pro Studio to watch, share, or post it."
                        )
                    }
                } else {
                    items(exports, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { playingExport = item },
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StudioSurfaceHighlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, "Play export", tint = ElectricCyan)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.projectTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatShortDuration(item.durationMs)} · ${item.resolutionLabel} · ${formatFileSize(item.fileSizeBytes)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        val res = SmartMediaHelpers.saveVideoToGallery(context, File(item.filePath), item.projectTitle)
                                        galleryBannerMsg = if (res.isSuccess) "Saved \"${item.projectTitle}\" to Gallery!" else "Save failed"
                                    }
                                }) {
                                    Icon(Icons.Filled.Download, "Save to Gallery", tint = StudioEmerald)
                                }
                                IconButton(onClick = { onPublishExportToTikTok(item) }) {
                                    Icon(Icons.Filled.Publish, "Post to TikTok", tint = NeonRose)
                                }
                                IconButton(onClick = {
                                    val file = File(item.filePath)
                                    if (file.exists()) {
                                        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share s.s Video"))
                                    }
                                }) {
                                    Icon(Icons.Filled.Share, "Share", tint = ElectricCyan)
                                }
                                IconButton(onClick = { onDeleteExport(item) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Delete", tint = NeonRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPhoneDialog) {
        var phoneDraft by remember(linkedPhone) { mutableStateOf(linkedPhone) }
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Link Mobile Number", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = phoneDraft,
                    onValueChange = { phoneDraft = it },
                    label = { Text("Phone number") },
                    placeholder = { Text("+92 300 1234567") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePhone(phoneDraft.trim())
                    showPhoneDialog = false
                }) {
                    Text("Save", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditDialog) {
        val cur = profile ?: UserProfileDoc()
        var name by remember(cur.displayName) { mutableStateOf(cur.displayName) }
        var handle by remember(cur.handle) { mutableStateOf(cur.handle) }
        var bio by remember(cur.bio) { mutableStateOf(cur.bio) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Edit Creator Profile", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                    OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("@handle") }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateProfile(name, handle, bio)
                    showEditDialog = false
                }) {
                    Text("Save", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Sign Out?", color = TextPrimary) },
            text = {
                Text(
                    "Your CapCut Cloud projects, Canva Pro designs, and TikTok posts stay safely synced to your Gmail account.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    onSignOut()
                }) {
                    Text("Sign Out", color = NeonRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Stay Signed In", color = ElectricCyan)
                }
            }
        )
    }

    playingExport?.let { exportItem ->
        ExportVideoPreviewDialog(exportItem = exportItem, onDismiss = { playingExport = null })
    }
}

@Composable
private fun ProfileStatColumn(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ProfilePostRow(
    post: SocialVideoPostDoc,
    onOpenFeed: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenFeed),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(post.accentHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color(post.accentHex))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(post.caption, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Favorite, null, tint = NeonRose, modifier = Modifier.size(13.dp))
                    Text("${post.likesCount}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("· ${post.mediaType.replace("_", " ")}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, "Delete post", tint = NeonRose)
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileSection(
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            if (ctaLabel != null && onCta != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onCta,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(ctaLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
