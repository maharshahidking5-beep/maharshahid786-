package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ai.configureAppCheck
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutWithCleanup
import com.example.data.CloudSocialRepository
import com.example.media.StudioCatalog
import com.example.model.CanvaProject
import com.example.model.EditorToolTab
import com.example.model.MainNavTab
import com.example.model.TemplateItem
import com.example.ui.components.StudioBottomNavigation
import com.example.ui.screens.BatchPhotoToolSheet
import com.example.ui.screens.CanvaStudioScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.MiniGuidesBottomSheet
import com.example.ui.screens.PendingTikTokPostDraft
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SsAssistantBottomSheet
import com.example.ui.screens.TemplatesScreen
import com.example.ui.screens.TikTokFeedScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SsStudioTheme
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.SocialAndCanvaViewModel
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
object RouteHub

@Serializable
object RouteEditor

private sealed class PendingPickerIntent {
    data class NewProject(val autoCut: Boolean, val initialTool: EditorToolTab) : PendingPickerIntent()
    data class UseTemplate(val template: TemplateItem) : PendingPickerIntent()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            SsStudioTheme {
                SsStudioRoot()
            }
        }
    }
}

@Composable
fun SsStudioRoot() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            SsAuthenticatedStudioApp(currentUser = user)
        }
    }
}

@Composable
fun SsAuthenticatedStudioApp(
    currentUser: FirebaseUser,
    studioViewModel: StudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val socialViewModel: SocialAndCanvaViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                SocialAndCanvaViewModel(
                    repo = CloudSocialRepository(context.applicationContext),
                    currentUserId = currentUser.uid,
                    appContext = context.applicationContext
                )
            }
        }
    )

    LaunchedEffect(currentUser.uid) {
        socialViewModel.ensureProfileAndSeed(currentUser)
    }

    DisposableEffect(studioViewModel, socialViewModel) {
        studioViewModel.onCloudSyncProject = { project ->
            socialViewModel.syncCapCutProject(project)
        }
        onDispose {
            studioViewModel.onCloudSyncProject = null
        }
    }

    val navController = rememberNavController()
    val projects by studioViewModel.projects.collectAsStateWithLifecycle()
    val exports by studioViewModel.exports.collectAsStateWithLifecycle()
    val preferences by studioViewModel.preferences.collectAsStateWithLifecycle()
    val editorState by studioViewModel.editorState.collectAsStateWithLifecycle()
    val assistantMessages by studioViewModel.assistantMessages.collectAsStateWithLifecycle()
    val isAssistantStreaming by studioViewModel.isAssistantStreaming.collectAsStateWithLifecycle()
    val assistantError by studioViewModel.assistantError.collectAsStateWithLifecycle()

    val profileUiState by socialViewModel.profileUiState.collectAsStateWithLifecycle()
    val profile by socialViewModel.profile.collectAsStateWithLifecycle()
    val privateEmail by socialViewModel.privateEmail.collectAsStateWithLifecycle()
    val feedPosts by socialViewModel.feedPosts.collectAsStateWithLifecycle()
    val myPosts by socialViewModel.myPosts.collectAsStateWithLifecycle()
    val likedPostIds by socialViewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedPostIds by socialViewModel.savedPostIds.collectAsStateWithLifecycle()
    val followingIds by socialViewModel.followingIds.collectAsStateWithLifecycle()
    val canvaDesigns by socialViewModel.canvaDesigns.collectAsStateWithLifecycle()
    val cloudProjects by socialViewModel.cloudProjects.collectAsStateWithLifecycle()
    val activeComments by socialViewModel.activeComments.collectAsStateWithLifecycle()
    val socialStatusMessage by socialViewModel.statusMessage.collectAsStateWithLifecycle()
    val cloudSyncState by socialViewModel.cloudSyncState.collectAsStateWithLifecycle()

    LaunchedEffect(projects, preferences.autoSyncOnline, preferences.lastCloudSyncMs) {
        socialViewModel.updateSyncMetadataFromLocal(
            localProjects = projects,
            autoSyncEnabled = preferences.autoSyncOnline,
            lastCloudSyncMs = preferences.lastCloudSyncMs
        )
    }

    LaunchedEffect(socialStatusMessage) {
        if (socialStatusMessage != null) {
            delay(2200L)
            socialViewModel.clearStatusMessage()
        }
    }

    var currentHubTab by remember { mutableStateOf(MainNavTab.EDIT) }
    var pendingPickerIntent by remember { mutableStateOf<PendingPickerIntent?>(null) }
    var pendingTikTokDraft by remember { mutableStateOf<PendingTikTokPostDraft?>(null) }
    var pendingCanvaProject by remember { mutableStateOf<CanvaProject?>(null) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var showMiniGuidesSheet by remember { mutableStateOf(false) }
    var showBatchPhotoSheet by remember { mutableStateOf(false) }

    val multiMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris ->
        val pending = pendingPickerIntent
        pendingPickerIntent = null
        if (uris.isEmpty() || pending == null) return@rememberLauncherForActivityResult

        when (pending) {
            is PendingPickerIntent.NewProject -> {
                studioViewModel.createProjectFromUris(
                    uris = uris,
                    initialTool = pending.initialTool,
                    autoCut = pending.autoCut
                ) {
                    navController.navigate(RouteEditor) { launchSingleTop = true }
                }
            }
            is PendingPickerIntent.UseTemplate -> {
                studioViewModel.applyTemplate(
                    template = pending.template,
                    pickedUris = uris
                ) {
                    navController.navigate(RouteEditor) { launchSingleTop = true }
                }
            }
        }
    }

    val hasPhotoWallpaper = preferences.appThemeMode == "photo" && preferences.customBgUri.isNotBlank()

    SsStudioTheme(themeMode = preferences.appThemeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (hasPhotoWallpaper) {
                AsyncImage(
                    model = preferences.customBgUri,
                    contentDescription = "Studio custom wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(preferences.customBgBlur.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.56f))
                )
            }

            NavHost(
                navController = navController,
                startDestination = RouteHub,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable<RouteHub> {
                    Scaffold(
                        containerColor = if (hasPhotoWallpaper) Color.Transparent else MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            StudioBottomNavigation(
                                currentTab = currentHubTab,
                                onSelectTab = { currentHubTab = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentHubTab) {
                                MainNavTab.EDIT -> HomeHubScreen(
                                    projects = projects,
                                    preferences = preferences,
                                    cloudSyncState = cloudSyncState,
                                    onSyncNow = {
                                        socialViewModel.syncAllProjectsNow(localProjects = projects) { ts ->
                                            studioViewModel.updateStudioPreferences(lastCloudSyncMs = ts)
                                        }
                                    },
                                    onPickNewProject = { autoCut, initialTool ->
                                        pendingPickerIntent = PendingPickerIntent.NewProject(autoCut, initialTool)
                                        multiMediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    onLaunchDemoProject = { initialTool ->
                                        studioViewModel.createDemoProject(initialTool) {
                                            navController.navigate(RouteEditor) { launchSingleTop = true }
                                        }
                                    },
                                    onLaunchQuickPresetPack = { pack ->
                                        studioViewModel.createProjectWithPresetPack(pack) {
                                            navController.navigate(RouteEditor) { launchSingleTop = true }
                                        }
                                    },
                                    onOpenProject = { project ->
                                        studioViewModel.openExistingProject(project)
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    },
                                    onRenameProject = { id, title -> studioViewModel.renameProject(id, title) },
                                    onDuplicateProject = { project -> studioViewModel.duplicateProject(project) },
                                    onDeleteProject = { id -> studioViewModel.deleteProject(id) },
                                    onOpenExportsTab = { currentHubTab = MainNavTab.PROFILE },
                                    onOpenSsAssistant = { showAssistantSheet = true },
                                    onOpenMiniGuides = { showMiniGuidesSheet = true },
                                    onOpenBatchPhotoTool = { showBatchPhotoSheet = true },
                                    onUpdatePreferences = { res, fps, wm, autoSave, batSaver, autoSync, themeMode, bgUri, bgBlur ->
                                        studioViewModel.updateStudioPreferences(
                                            defaultResolution = res,
                                            defaultFps = fps,
                                            addWatermark = wm,
                                            autoSaveDrafts = autoSave,
                                            batterySaverMode = batSaver,
                                            autoSyncOnline = autoSync,
                                            appThemeMode = themeMode,
                                            customBgUri = bgUri,
                                            customBgBlur = bgBlur
                                        )
                                    }
                                )

                            MainNavTab.CANVA -> CanvaStudioScreen(
                                savedDesigns = canvaDesigns,
                                decodeDesign = { socialViewModel.decodeCanvaDesign(it) },
                                onSaveDesignToCloud = { socialViewModel.saveCanvaDesign(it) },
                                onDeleteCloudDesign = { socialViewModel.deleteCanvaDesign(it) },
                                onUseAsCapCutCover = { coverUri, title ->
                                    studioViewModel.applyCanvaCoverToActiveOrDemo(coverUri, title) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                },
                                onPublishCanvaToFeed = { title, headline, accentHex, pngUri ->
                                    pendingTikTokDraft = PendingTikTokPostDraft(
                                        caption = headline.ifBlank { title },
                                        hashtagsRaw = "#canvapro #ssstudio #design",
                                        soundTitle = "Studio Visual Beat",
                                        mediaType = "CANVA_DESIGN",
                                        mediaUrl = pngUri,
                                        accentHex = accentHex,
                                        filterLabel = "Canva Pro"
                                    )
                                    currentHubTab = MainNavTab.FEED
                                },
                                onShowMessage = { socialViewModel.showStatus(it) },
                                externalOpenProject = pendingCanvaProject,
                                onConsumedExternalProject = { pendingCanvaProject = null }
                            )

                            MainNavTab.FEED -> TikTokFeedScreen(
                                currentUserId = currentUser.uid,
                                posts = feedPosts,
                                likedPostIds = likedPostIds,
                                savedPostIds = savedPostIds,
                                followingIds = followingIds,
                                activeComments = activeComments,
                                onToggleLike = { socialViewModel.toggleLike(it) },
                                onDoubleTapLike = { socialViewModel.ensureLikedOnDoubleTap(it) },
                                onToggleSave = { socialViewModel.toggleSave(it) },
                                onRecordShare = { socialViewModel.recordShare(it) },
                                onToggleFollow = { socialViewModel.toggleFollow(it) },
                                onOpenComments = { socialViewModel.openComments(it) },
                                onCloseComments = { socialViewModel.closeComments() },
                                onAddComment = { post, text -> socialViewModel.addComment(post, text) },
                                onDeletePost = { socialViewModel.deletePost(it) },
                                onUseTemplateInCapCut = { templateId ->
                                    val tpl = StudioCatalog.templates.find { it.id == templateId }
                                        ?: StudioCatalog.templates.first()
                                    studioViewModel.applyTemplate(tpl, emptyList()) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                },
                                onPublishPost = { caption, tags, sound, mType, mUrl, accentHex, filterLabel, tplId ->
                                    socialViewModel.publishPost(
                                        caption = caption,
                                        hashtagsRaw = tags,
                                        soundTitle = sound,
                                        mediaType = mType,
                                        mediaUrl = mUrl,
                                        accentHex = accentHex,
                                        filterLabel = filterLabel,
                                        templateId = tplId
                                    )
                                },
                                pendingDraft = pendingTikTokDraft,
                                onConsumedPendingDraft = { pendingTikTokDraft = null }
                            )

                            MainNavTab.TEMPLATES -> TemplatesScreen(
                                onUseTemplateWithPicker = { template ->
                                    pendingPickerIntent = PendingPickerIntent.UseTemplate(template)
                                    multiMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onPreviewTemplateWithSample = { template ->
                                    studioViewModel.applyTemplate(template, emptyList()) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                }
                            )

                            MainNavTab.PROFILE -> ProfileScreen(
                                profileUiState = profileUiState,
                                profile = profile,
                                privateEmail = privateEmail,
                                linkedPhone = preferences.linkedPhone,
                                cloudSyncState = cloudSyncState,
                                useTransparentBg = hasPhotoWallpaper,
                                myPosts = myPosts,
                                allFeedPosts = feedPosts,
                                savedPostIds = savedPostIds,
                                cloudProjects = cloudProjects,
                                canvaDesigns = canvaDesigns,
                                exports = exports,
                                decodeCanvaDesign = { socialViewModel.decodeCanvaDesign(it) },
                                onUpdateProfile = { name, handle, bio ->
                                    socialViewModel.updateProfile(name, handle, bio)
                                },
                                onSavePhone = { phone ->
                                    studioViewModel.updateStudioPreferences(linkedPhone = phone)
                                    socialViewModel.showStatus("Studio Phone Number Saved")
                                },
                                onOpenSsAssistant = { showAssistantSheet = true },
                                onSyncAllNow = {
                                    socialViewModel.syncAllProjectsNow(localProjects = projects) { ts ->
                                        studioViewModel.updateStudioPreferences(lastCloudSyncMs = ts)
                                    }
                                },
                                onToggleAutoSync = { enabled ->
                                    studioViewModel.updateStudioPreferences(autoSyncOnline = enabled)
                                },
                                onSignOut = {
                                    signOutWithCleanup(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                onOpenFeedTab = { currentHubTab = MainNavTab.FEED },
                                onDeletePost = { socialViewModel.deletePost(it) },
                                onOpenCloudCapCutProject = { cloudDoc ->
                                    studioViewModel.restoreCloudProject(cloudDoc.projectJson) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                },
                                onOpenCanvaDesign = { canvaProj ->
                                    pendingCanvaProject = canvaProj
                                    currentHubTab = MainNavTab.CANVA
                                },
                                onDeleteCanvaDesign = { socialViewModel.deleteCanvaDesign(it) },
                                onPublishExportToTikTok = { exportItem ->
                                    pendingTikTokDraft = PendingTikTokPostDraft(
                                        caption = "${exportItem.projectTitle} · Rendered in ${exportItem.resolutionLabel} 🔥",
                                        hashtagsRaw = "#ssstudio #capcutpro #1080pplus",
                                        soundTitle = "Original Sound - s.s Studio",
                                        mediaType = "VIDEO",
                                        mediaUrl = Uri.fromFile(File(exportItem.filePath)).toString(),
                                        accentHex = 0xFF00E5FFL,
                                        filterLabel = exportItem.resolutionLabel
                                    )
                                    currentHubTab = MainNavTab.FEED
                                },
                                onDeleteExport = { studioViewModel.deleteExport(it) }
                            )
                        }
                    }
                }
            }

            composable<RouteEditor> {
                EditorScreen(
                    viewModel = studioViewModel,
                    onExitEditor = { navController.popBackStack() },
                    onPublishExportToTikTok = { exportItem ->
                        pendingTikTokDraft = PendingTikTokPostDraft(
                            caption = "${exportItem.projectTitle} · Rendered in ${exportItem.resolutionLabel} 🔥",
                            hashtagsRaw = "#ssstudio #capcutpro #1080pplus",
                            soundTitle = "Original Sound - s.s Studio",
                            mediaType = "VIDEO",
                            mediaUrl = Uri.fromFile(File(exportItem.filePath)).toString(),
                            accentHex = 0xFF00E5FFL,
                            filterLabel = exportItem.resolutionLabel
                        )
                        navController.popBackStack()
                        currentHubTab = MainNavTab.FEED
                    },
                    onOpenAssistant = { showAssistantSheet = true },
                    onOpenMiniGuides = { showMiniGuidesSheet = true }
                )
            }
        }

        AnimatedVisibility(
            visible = socialStatusMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Surface(
                color = StudioSurfaceElevated.copy(alpha = 0.96f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Text(
                    text = socialStatusMessage.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (editorState.isImportingMedia) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        }

        if (showAssistantSheet) {
            SsAssistantBottomSheet(
                messages = assistantMessages,
                isStreaming = isAssistantStreaming,
                errorMessage = assistantError,
                linkedPhone = preferences.linkedPhone,
                onAskQuestion = { studioViewModel.askSsAssistant(it) },
                onClearChat = { studioViewModel.clearAssistantHistory() },
                onSavePhone = { phone ->
                    studioViewModel.updateStudioPreferences(linkedPhone = phone)
                    socialViewModel.showStatus("Studio Phone Saved")
                },
                onDismiss = { showAssistantSheet = false }
            )
        }

        if (showMiniGuidesSheet) {
            MiniGuidesBottomSheet(
                onTryTool = { toolTab ->
                    if (editorState.project.clips.isNotEmpty()) {
                        studioViewModel.setToolTab(toolTab)
                        navController.navigate(RouteEditor) { launchSingleTop = true }
                    } else {
                        studioViewModel.createDemoProject(toolTab) {
                            navController.navigate(RouteEditor) { launchSingleTop = true }
                        }
                    }
                },
                onAskAssistant = { prompt ->
                    showAssistantSheet = true
                    studioViewModel.askSsAssistant(prompt)
                },
                onDismiss = { showMiniGuidesSheet = false }
            )
        }

        if (showBatchPhotoSheet) {
            BatchPhotoToolSheet(
                defaultWatermark = preferences.addEndingWatermark,
                onImportPhotosAsReel = { uris ->
                    studioViewModel.createProjectFromUris(
                        uris = uris,
                        initialTool = EditorToolTab.FILTERS,
                        autoCut = true
                    ) {
                        navController.navigate(RouteEditor) { launchSingleTop = true }
                    }
                },
                onDismiss = { showBatchPhotoSheet = false }
            )
        }
        }
    }
}
