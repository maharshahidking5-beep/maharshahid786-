package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.auth.AccountSecurityHelpers
import com.example.auth.MAX_MULTI_DEVICE_LOGINS
import com.example.auth.ProfileCustomizationCatalog
import com.example.data.CloudSyncUiState
import com.example.data.ExportEntity
import com.example.data.StudioPreferences
import com.example.media.SmartMediaHelpers
import com.example.model.CanvaDesignDoc
import com.example.model.CanvaProject
import com.example.model.CloudProjectSummaryDoc
import com.example.model.ProEntitlementDoc
import com.example.model.ProPackageTier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileUiState: ProfileUiState,
    profile: UserProfileDoc?,
    privateEmail: String,
    preferences: StudioPreferences,
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
    onUpdateProfile: (displayName: String, handle: String, bio: String, photoUrl: String?) -> Unit,
    onUpdateProfileCustomization: (coverStyle: String?, localPhotoUri: String?, localBannerUri: String?) -> Unit,
    onActivatePakistaniPro: (normalizedPhone: String) -> Unit,
    onDisablePakistaniPro: () -> Unit,
    onSavePhone: (String) -> Unit,
    onShareApp: () -> Unit,
    onShareProfile: () -> Unit,
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
    hasAiProAccess: Boolean = false,
    isAdminUser: Boolean = false,
    entitlement: ProEntitlementDoc? = null,
    pendingAdminRequestsCount: Int = 0,
    onOpenAiVideoGenPro: () -> Unit = {},
    onOpenProPackages: () -> Unit = {},
    onOpenAdminConsole: () -> Unit = {},
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
    val coverTheme = remember(preferences.profileCoverStyle) {
        ProfileCustomizationCatalog.findCoverTheme(preferences.profileCoverStyle)
    }
    val registeredDevices = remember(preferences.registeredDevicesJson) {
        AccountSecurityHelpers.decodeDevices(preferences.registeredDevicesJson)
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
                        onClick = onShareApp,
                        modifier = Modifier
                            .testTag("profile_share_app_button")
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.Filled.Share, "Share app with friends", tint = StudioAmber)
                    }
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .testTag("profile_edit_button")
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.Filled.Edit, "Customize profile", tint = ElectricCyan)
                    }
                    IconButton(
                        onClick = { showSignOutConfirm = true },
                        modifier = Modifier
                            .testTag("profile_sign_out_button")
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Sign out or switch account", tint = NeonRose)
                    }
                }
            }
        }

        item {
            // Customizable Profile Hero Card with Banner Cover + Avatar + Pakistani Pro Badge
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(coverTheme.accentHex).copy(alpha = 0.55f))
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
                    val presetAvatar = ProfileCustomizationCatalog.findAvatarPreset(p.photoUrl)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Customizable Cover Banner Strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(coverTheme.startHex), Color(coverTheme.endHex))
                                    )
                                )
                        ) {
                            if (preferences.profileLocalBannerUri.isNotBlank()) {
                                AsyncImage(
                                    model = preferences.profileLocalBannerUri,
                                    contentDescription = "Profile cover photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.28f))
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showEditDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, null, tint = ElectricCyan, modifier = Modifier.size(12.dp))
                                    Text("Customize", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-36).dp)
                                    .padding(bottom = 0.dp)
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (presetAvatar != null) {
                                            Brush.linearGradient(
                                                listOf(Color(presetAvatar.primaryHex), Color(presetAvatar.secondaryHex))
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(ElectricCyan, NeonRose))
                                        }
                                    )
                                    .border(3.dp, StudioSurfaceElevated, CircleShape)
                                    .clickable { showEditDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    preferences.profileLocalPhotoUri.isNotBlank() -> {
                                        AsyncImage(
                                            model = preferences.profileLocalPhotoUri,
                                            contentDescription = p.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    presetAvatar != null -> {
                                        Text(
                                            text = p.displayName.take(2).uppercase().ifBlank { "SS" },
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White
                                        )
                                    }
                                    p.photoUrl.isNotBlank() -> {
                                        AsyncImage(
                                            model = p.photoUrl,
                                            contentDescription = p.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = p.displayName.take(1).uppercase().ifBlank { "S" },
                                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFF041418)
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(y = (-24).dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = p.displayName.ifBlank { "s.s Creator" },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    if (preferences.isPakistaniPro) {
                                        Surface(
                                            color = StudioEmerald.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, StudioEmerald)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Filled.Verified, null, tint = StudioEmerald, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = "PAK PRO",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = StudioEmerald
                                                )
                                            }
                                        }
                                    }
                                }
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
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                                    modifier = Modifier.offset(y = (-14).dp)
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
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStatColumn(p.followingCount.toString(), "Following")
                                ProfileStatColumn(p.followersCount.toString(), "Followers")
                                ProfileStatColumn(p.likesCount.toString(), "Likes")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onShareProfile,
                                    modifier = Modifier.weight(1f).testTag("profile_share_profile_button"),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Share Profile", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                                }
                                OutlinedButton(
                                    onClick = onShareApp,
                                    modifier = Modifier.weight(1f).testTag("profile_invite_friends_button"),
                                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null, tint = StudioAmber, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Share App", style = MaterialTheme.typography.labelMedium, color = StudioAmber)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // AI Video Gen Pro + JazzCash/EasyPaisa Package Card + Exclusive Admin Console Card
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("profile_ai_pro_card"),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (hasAiProAccess) StudioEmerald.copy(alpha = 0.65f) else StudioAmber.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeTierTitle = entitlement?.let { ProPackageTier.fromId(it.tier).title } ?: "Monthly Creator Pro"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.WorkspacePremium,
                                null,
                                tint = if (hasAiProAccess) StudioEmerald else StudioAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (hasAiProAccess) "AI Video Pro Active" else "AI Video Gen Pro Packages",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (hasAiProAccess) StudioEmerald else TextPrimary
                            )
                        }
                    }
                    Text(
                        text = if (hasAiProAccess) {
                            "Plan: $activeTierTitle · Unlimited Prompt-to-Video & 1080P+ HD unlocked."
                        } else {
                            "Purchase Weekly / Monthly / Yearly Pro via JazzCash or EasyPaisa."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAiVideoGenPro,
                            modifier = Modifier.weight(1f).testTag("profile_open_ai_video_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF041418)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("AI Video Pro", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = onOpenProPackages,
                            modifier = Modifier.weight(1f).testTag("profile_buy_pro_btn"),
                            border = BorderStroke(1.dp, StudioAmber),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (hasAiProAccess) "Manage Plan" else "JazzCash / EasyPaisa",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioAmber
                            )
                        }
                    }
                }
            }

            if (isAdminUser) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("profile_admin_console_card")
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenAdminConsole),
                    color = VioletPulse.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, VioletPulse.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VioletPulse.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, null, tint = VioletPulse, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Billing Console ($pendingAdminRequestsCount Pending)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Change JazzCash / EasyPaisa numbers & approve Pro orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Pakistani Pro (+92) vs Without Pro (Free) Card & Multi-Device (Up to 10 Mobiles) Card
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("pakistani_pro_card"),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (preferences.isPakistaniPro) StudioEmerald.copy(alpha = 0.6f) else StudioBorder
                )
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (preferences.isPakistaniPro) "Pakistani Pro Active (+92)" else "Free Mode (Without Pro)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (preferences.isPakistaniPro) StudioEmerald else TextPrimary
                            )
                            Text(
                                text = if (preferences.isPakistaniPro && linkedPhone.isNotBlank()) {
                                    "Verified Mobile: $linkedPhone · Zero watermark & Pro perks unlocked"
                                } else {
                                    "No phone number required for Free mode. Add a +92 number anytime for Pakistani Pro."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPhoneDialog = true },
                            modifier = Modifier.weight(1f).testTag("profile_pak_pro_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioEmerald,
                                contentColor = Color(0xFF041418)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Verified, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (preferences.isPakistaniPro) "Update +92 Number" else "Get Pakistani Pro (+92)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (preferences.isPakistaniPro) {
                            OutlinedButton(
                                onClick = onDisablePakistaniPro,
                                border = BorderStroke(1.dp, StudioBorder),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text("Use Without Pro", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("multi_device_card"),
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
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
                            Icon(Icons.Filled.Devices, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Multi-Device Re-Login (${registeredDevices.size.coerceAtLeast(1)} / $MAX_MULTI_DEVICE_LOGINS Mobiles)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                    Text(
                        text = "This Mobile: ${registeredDevices.lastOrNull()?.deviceLabel ?: AccountSecurityHelpers.currentDeviceLabel()} · Same account works simultaneously on up to $MAX_MULTI_DEVICE_LOGINS mobiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier.weight(1f).testTag("profile_relogin_button"),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Re-Login / Switch Account", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        }
                        OutlinedButton(
                            onClick = onShareApp,
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Share, null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Phone", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
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
        var phoneError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Pakistani Pro (+92) Activation", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your Pakistani mobile number (+92 3XX XXXXXXX or 03XX XXXXXXX) to activate Pakistani Pro. No phone number is needed for Free mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = phoneDraft,
                        onValueChange = {
                            phoneDraft = it
                            phoneError = null
                        },
                        label = { Text("Pakistani Number (+92)") },
                        placeholder = { Text("0300 1234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    if (phoneError != null) {
                        Text(
                            text = phoneError.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonRose
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalized = AccountSecurityHelpers.normalizePakistaniPhone(phoneDraft)
                    if (normalized == null) {
                        phoneError = "Enter a valid Pakistani mobile number (e.g. 0300 1234567)"
                    } else {
                        onSavePhone(normalized)
                        onActivatePakistaniPro(normalized)
                        showPhoneDialog = false
                    }
                }) {
                    Text("Activate Pro", color = StudioEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        onDisablePakistaniPro()
                        showPhoneDialog = false
                    }) {
                        Text("Without Pro", color = TextSecondary)
                    }
                    TextButton(onClick = { showPhoneDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }

    if (showEditDialog) {
        val cur = profile ?: UserProfileDoc()
        var name by remember(cur.displayName) { mutableStateOf(cur.displayName) }
        var handle by remember(cur.handle) { mutableStateOf(cur.handle) }
        var bio by remember(cur.bio) { mutableStateOf(cur.bio) }
        var selectedPhotoUrl by remember(cur.photoUrl) { mutableStateOf(cur.photoUrl) }
        var localPhotoDraft by remember(preferences.profileLocalPhotoUri) { mutableStateOf(preferences.profileLocalPhotoUri) }
        var localBannerDraft by remember(preferences.profileLocalBannerUri) { mutableStateOf(preferences.profileLocalBannerUri) }
        var selectedCoverId by remember(preferences.profileCoverStyle) { mutableStateOf(preferences.profileCoverStyle) }

        val avatarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                localPhotoDraft = uri.toString()
            }
        }
        val bannerPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                localBannerDraft = uri.toString()
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showEditDialog = false },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customize Creator Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                // Avatar Customization Row (Photo Picker + Studio Avatars)
                Text("Profile Avatar", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery Photo", color = ElectricCyan)
                    }
                    if (localPhotoDraft.isNotBlank()) {
                        TextButton(onClick = { localPhotoDraft = "" }) {
                            Text("Use Studio Badge", color = TextSecondary)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ProfileCustomizationCatalog.avatarPresets, key = { it.id }) { preset ->
                        val active = localPhotoDraft.isBlank() && selectedPhotoUrl == preset.tokenUrl
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    localPhotoDraft = ""
                                    selectedPhotoUrl = preset.tokenUrl
                                },
                            color = if (active) Color(preset.primaryHex).copy(alpha = 0.2f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (active) Color(preset.primaryHex) else StudioBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(preset.primaryHex), Color(preset.secondaryHex))
                                            )
                                        )
                                )
                                Text(preset.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                }

                // Cover Banner Customization
                Text("Profile Cover Banner", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Wallpaper, null, tint = NeonRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Cover Photo", color = NeonRose)
                    }
                    if (localBannerDraft.isNotBlank()) {
                        TextButton(onClick = { localBannerDraft = "" }) {
                            Text("Reset Gradient", color = TextSecondary)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProfileCustomizationCatalog.coverThemes, key = { it.id }) { theme ->
                        val active = selectedCoverId == theme.id && localBannerDraft.isBlank()
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    localBannerDraft = ""
                                    selectedCoverId = theme.id
                                },
                            color = Color(theme.startHex),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, if (active) Color(theme.accentHex) else StudioBorder)
                        ) {
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color(theme.accentHex) else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Creator @username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onUpdateProfile(name, handle, bio, selectedPhotoUrl)
                        onUpdateProfileCustomization(selectedCoverId, localPhotoDraft, localBannerDraft)
                        showEditDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_save_customization_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041418)
                    )
                ) {
                    Text("Save Custom Profile", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
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
