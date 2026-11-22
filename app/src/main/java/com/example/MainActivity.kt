package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.EditorToolTab
import com.example.model.MainNavTab
import com.example.model.TemplateItem
import com.example.ui.components.StudioBottomNavigation
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportsScreen
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SsStudioTheme
import com.example.ui.theme.StudioBackground
import com.example.viewmodel.StudioViewModel
import kotlinx.serialization.Serializable

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
        enableEdgeToEdge()
        setContent {
            SsStudioTheme {
                SsStudioApp()
            }
        }
    }
}

@Composable
fun SsStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val navController = rememberNavController()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val exports by viewModel.exports.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()

    var currentHubTab by remember { mutableStateOf(MainNavTab.EDIT) }
    var pendingPickerIntent by remember { mutableStateOf<PendingPickerIntent?>(null) }

    val multiMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris ->
        val pending = pendingPickerIntent
        pendingPickerIntent = null
        if (uris.isEmpty() || pending == null) return@rememberLauncherForActivityResult

        when (pending) {
            is PendingPickerIntent.NewProject -> {
                viewModel.createProjectFromUris(
                    uris = uris,
                    initialTool = pending.initialTool,
                    autoCut = pending.autoCut
                ) {
                    navController.navigate(RouteEditor) { launchSingleTop = true }
                }
            }
            is PendingPickerIntent.UseTemplate -> {
                viewModel.applyTemplate(
                    template = pending.template,
                    pickedUris = uris
                ) {
                    navController.navigate(RouteEditor) { launchSingleTop = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(StudioBackground)) {
        NavHost(
            navController = navController,
            startDestination = RouteHub,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable<RouteHub> {
                Scaffold(
                    containerColor = StudioBackground,
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
                                onPickNewProject = { autoCut, initialTool ->
                                    pendingPickerIntent = PendingPickerIntent.NewProject(autoCut, initialTool)
                                    multiMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onLaunchDemoProject = { initialTool ->
                                    viewModel.createDemoProject(initialTool) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                },
                                onOpenProject = { project ->
                                    viewModel.openExistingProject(project)
                                    navController.navigate(RouteEditor) { launchSingleTop = true }
                                },
                                onRenameProject = { id, title -> viewModel.renameProject(id, title) },
                                onDuplicateProject = { project -> viewModel.duplicateProject(project) },
                                onDeleteProject = { id -> viewModel.deleteProject(id) },
                                onOpenExportsTab = { currentHubTab = MainNavTab.EXPORTS },
                                onUpdatePreferences = { res, fps, autoSave ->
                                    viewModel.updateStudioPreferences(
                                        defaultResolution = res,
                                        defaultFps = fps,
                                        autoSaveDrafts = autoSave
                                    )
                                }
                            )

                            MainNavTab.TEMPLATES -> TemplatesScreen(
                                onUseTemplateWithPicker = { template ->
                                    pendingPickerIntent = PendingPickerIntent.UseTemplate(template)
                                    multiMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onPreviewTemplateWithSample = { template ->
                                    viewModel.applyTemplate(template, emptyList()) {
                                        navController.navigate(RouteEditor) { launchSingleTop = true }
                                    }
                                }
                            )

                            MainNavTab.EXPORTS -> ExportsScreen(
                                exports = exports,
                                onDeleteExport = { viewModel.deleteExport(it) }
                            )
                        }
                    }
                }
            }

            composable<RouteEditor> {
                EditorScreen(
                    viewModel = viewModel,
                    onExitEditor = { navController.popBackStack() }
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
    }
}
