package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import coil.compose.AsyncImage
import com.example.data.NetworkAndPowerObserver
import com.example.media.ExportUiState
import com.example.media.MediaEffectBuilder
import com.example.media.VoiceEnhancerAudioProcessor
import com.example.model.CanvasStyle
import com.example.model.ClipAnimationType
import com.example.model.EditorToolTab
import com.example.model.PipShape
import com.example.model.RemoveBgMode
import com.example.model.SafeZoneMode
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.ui.components.formatTimecode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ActiveClipSlice(
    val clip: VideoClip,
    val clipStartInProjectMs: Long,
    val localEffectiveOffsetMs: Long
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    viewModel: StudioViewModel,
    onExitEditor: () -> Unit,
    onPublishExportToTikTok: ((com.example.data.ExportEntity) -> Unit)? = null,
    onOpenAssistant: (() -> Unit)? = null,
    onOpenMiniGuides: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.editorState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val aiLabState by viewModel.aiLabState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val project = state.project
    val context = LocalContext.current
    val isBatterySaver = remember(preferences.batterySaverMode) {
        preferences.batterySaverMode || NetworkAndPowerObserver.isSystemPowerSaveMode(context)
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCoverSheet by remember { mutableStateOf(false) }
    var transitionTargetClip by remember { mutableStateOf<VideoClip?>(null) }
    var editingTextDialogItem by remember { mutableStateOf<TextOverlayItem?>(null) }

    BackHandler {
        if (state.isFullscreenPreview) {
            viewModel.toggleFullscreenPreview()
        } else if (state.activeToolTab != EditorToolTab.ROOT) {
            viewModel.setToolTab(EditorToolTab.ROOT)
        } else {
            viewModel.setPlaying(false)
            viewModel.saveCurrentProjectExplicit()
            onExitEditor()
        }
    }

    val appendClipsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.appendClipsFromUris(uris)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importAudioUri(uri)
    }

    val pipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addPipOverlay(uri)
    }

    val replaceClipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.replaceSelectedClip(uri)
    }

    val coverPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.updateCoverConfig(project.coverConfig.copy(coverImageUri = uri.toString()))
        }
    }

    // Determine active clip under playhead
    val activeSlice = remember(project.clips, state.playheadMs) {
        var cursor = 0L
        var found: ActiveClipSlice? = null
        for (clip in project.clips) {
            val dur = clip.effectiveDurationMs
            if (state.playheadMs <= cursor + dur || clip == project.clips.last()) {
                found = ActiveClipSlice(
                    clip = clip,
                    clipStartInProjectMs = cursor,
                    localEffectiveOffsetMs = (state.playheadMs - cursor).coerceIn(0L, dur)
                )
                break
            }
            cursor += dur
        }
        found
    }

    val voiceEnhancerProcessor = remember { VoiceEnhancerAudioProcessor() }
    LaunchedEffect(project.voiceEnhance) {
        voiceEnhancerProcessor.settings = project.voiceEnhance
    }

    val videoPlayer = remember(voiceEnhancerProcessor) {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(voiceEnhancerProcessor))
                    .build()
            }
        }
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    val sfxPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    var lastTriggeredSfxKey by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            audioPlayer.release()
            sfxPlayer.release()
        }
    }

    LaunchedEffect(state.isPlaying, state.playheadMs, project.sfxOverlays) {
        if (!state.isPlaying) {
            lastTriggeredSfxKey = null
            sfxPlayer.pause()
        } else {
            val hit = project.sfxOverlays.firstOrNull {
                state.playheadMs >= it.startMs && state.playheadMs <= it.startMs + 120L
            }
            if (hit != null && lastTriggeredSfxKey != "${hit.id}_${hit.startMs}") {
                lastTriggeredSfxKey = "${hit.id}_${hit.startMs}"
                sfxPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(hit.uriString)))
                sfxPlayer.volume = hit.volume
                sfxPlayer.prepare()
                sfxPlayer.play()
            }
        }
    }

    val activeClip = activeSlice?.clip
    LaunchedEffect(
        activeClip?.id,
        activeClip?.uriString,
        activeClip?.isPhoto,
        activeClip?.cropLeft,
        activeClip?.cropTop,
        activeClip?.cropRight,
        activeClip?.cropBottom,
        project.filterPreset,
        project.filterIntensity,
        project.effectPreset,
        project.effectIntensity,
        project.adjustments
    ) {
        if (activeClip != null && !activeClip.isPhoto) {
            val effects = MediaEffectBuilder.buildClipVideoEffects(
                project = project,
                clip = activeClip,
                includeExportPresentation = false
            )
            videoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeClip.uriString)))
            videoPlayer.setVideoEffects(effects)
            videoPlayer.prepare()
        } else {
            videoPlayer.stop()
        }
    }

    LaunchedEffect(
        state.isPlaying,
        activeClip?.id,
        activeClip?.speed,
        activeClip?.volume,
        project.isOriginalAudioMuted,
        project.voiceEnhance.pitchShift
    ) {
        if (activeClip != null && !activeClip.isPhoto) {
            videoPlayer.playbackParameters = PlaybackParameters(
                activeClip.speed.coerceIn(0.25f, 4.0f),
                project.voiceEnhance.pitchShift.coerceIn(0.75f, 1.35f)
            )
            videoPlayer.volume = if (project.isOriginalAudioMuted) 0f else activeClip.volume.coerceIn(0f, 1.5f)
            if (state.isPlaying) {
                val targetPos = activeClip.trimStartMs + (activeSlice.localEffectiveOffsetMs * activeClip.speed).toLong()
                if (abs(videoPlayer.currentPosition - targetPos) > 250L) {
                    videoPlayer.seekTo(targetPos)
                }
                videoPlayer.play()
            } else {
                videoPlayer.pause()
            }
        } else {
            videoPlayer.pause()
        }
    }

    LaunchedEffect(state.playheadMs, state.isPlaying, activeClip?.id) {
        if (!state.isPlaying && activeClip != null && !activeClip.isPhoto && activeSlice != null) {
            val targetPos = activeClip.trimStartMs + (activeSlice.localEffectiveOffsetMs * activeClip.speed).toLong()
            videoPlayer.seekTo(targetPos)
        }
    }

    val audioTrack = project.audioTrack
    LaunchedEffect(audioTrack?.uriString) {
        if (audioTrack != null) {
            audioPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audioTrack.uriString)))
            audioPlayer.prepare()
        } else {
            audioPlayer.stop()
        }
    }

    LaunchedEffect(state.isPlaying, audioTrack?.volume, audioTrack?.isMuted) {
        if (audioTrack != null) {
            audioPlayer.volume = if (audioTrack.isMuted) 0f else audioTrack.volume.coerceIn(0f, 1.2f)
            if (state.isPlaying) {
                if (abs(audioPlayer.currentPosition - state.playheadMs) > 400L) {
                    audioPlayer.seekTo(state.playheadMs % audioTrack.durationMs.coerceAtLeast(1000L))
                }
                audioPlayer.play()
            } else {
                audioPlayer.pause()
            }
        } else {
            audioPlayer.pause()
        }
    }

    LaunchedEffect(state.isPlaying, project.totalDurationMs, isBatterySaver) {
        if (state.isPlaying && project.totalDurationMs > 0L) {
            var lastTick = System.currentTimeMillis()
            val tickMs = if (isBatterySaver) 50L else 33L
            while (state.isPlaying) {
                delay(tickMs)
                val now = System.currentTimeMillis()
                val delta = (now - lastTick).coerceIn(10L, 120L)
                lastTick = now
                val next = state.playheadMs + delta
                if (next >= project.totalDurationMs) {
                    viewModel.seekTo(0L)
                    viewModel.setPlaying(false)
                    break
                } else {
                    viewModel.seekTo(next)
                }
            }
        }
    }

    LaunchedEffect(state.statusToast) {
        if (state.statusToast != null) {
            delay(1800L)
            viewModel.clearStatusToast()
        }
    }

    val composeColorFilter = remember(
        project.filterPreset,
        project.filterIntensity,
        project.effectPreset,
        project.adjustments
    ) {
        MediaEffectBuilder.buildComposeColorFilter(project)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // 1. Top CapCut Studio Bar
        if (!state.isFullscreenPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = {
                            viewModel.setPlaying(false)
                            viewModel.saveCurrentProjectExplicit()
                            onExitEditor()
                        },
                        modifier = Modifier
                            .testTag("editor_close_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close editor", tint = TextPrimary)
                    }
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenMiniGuides != null) {
                        IconButton(
                            onClick = onOpenMiniGuides,
                            modifier = Modifier
                                .testTag("editor_mini_guides_btn")
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(StudioSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Mini editing guides",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (onOpenAssistant != null) {
                        IconButton(
                            onClick = onOpenAssistant,
                            modifier = Modifier
                                .testTag("editor_assistant_btn")
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Open SS Assistant",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val autoEnhanced = project.adjustments.autoEnhanced
                    Surface(
                        modifier = Modifier
                            .testTag("editor_auto_hd_button")
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleAutoEnhanceVideo() },
                        color = if (autoEnhanced) ElectricCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (autoEnhanced) ElectricCyan else StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Hd,
                                contentDescription = "Toggle Auto Enhance",
                                tint = if (autoEnhanced) ElectricCyan else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoEnhanced) ElectricCyan else TextPrimary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .testTag("editor_quality_pill")
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.resetExportState()
                                showExportSheet = true
                            },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = "${project.resolution.label}·${project.fps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.resetExportState()
                            showExportSheet = true
                        },
                        modifier = Modifier
                            .testTag("editor_export_button")
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041418)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 2. Interactive CapCut Pro Video Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (state.isFullscreenPreview) 1f else 0.45f)
                .background(Color(0xFF06070A))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val stageBgBrush = when (project.canvasStyle) {
                CanvasStyle.NEON_GRADIENT -> Brush.linearGradient(listOf(Color(0xFF062B38), Color(0xFF140D26), Color(0xFF340C28)))
                CanvasStyle.SUNSET_GRADIENT -> Brush.linearGradient(listOf(Color(0xFF451A03), Color(0xFF1E1B4B), Color(0xFF7C2D12)))
                CanvasStyle.BLUR_BACKDROP, CanvasStyle.SOLID -> Brush.linearGradient(listOf(Color(project.canvasColorHex), Color(project.canvasColorHex)))
            }

            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(project.aspectRatio.ratioWidth / project.aspectRatio.ratioHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .clipToBounds()
                    .background(stageBgBrush)
                    .border(1.dp, StudioBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val stageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val stageHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                if (activeClip != null && activeSlice != null) {
                    val localMs = activeSlice.localEffectiveOffsetMs
                    val clipDurMs = activeClip.effectiveDurationMs.coerceAtLeast(300L)
                    val animDurF = activeClip.animationDurationMs.coerceIn(300L, 2500L).toFloat()
                    val animProgress = (localMs / animDurF).coerceIn(0f, 1f)
                    val tSec = state.playheadMs / 1000f
                    val stageTransform = interpolateClipTransform(activeClip, localMs)

                    // 10 Live Clip Animations
                    var animScale = 1f
                    var animAlpha = 1f
                    var animTransX = 0f
                    var animTransY = 0f
                    var animRot = 0f
                    when (activeClip.animation) {
                        ClipAnimationType.NONE -> Unit
                        ClipAnimationType.FADE_IN -> animAlpha = animProgress
                        ClipAnimationType.ZOOM_IN -> animScale = 0.84f + 0.16f * animProgress
                        ClipAnimationType.ZOOM_OUT -> animScale = 1.24f - 0.24f * animProgress
                        ClipAnimationType.PAN_LEFT -> {
                            animScale = 1.1f
                            animTransX = (1f - animProgress) * 55f
                        }
                        ClipAnimationType.PAN_RIGHT -> {
                            animScale = 1.1f
                            animTransX = -(1f - animProgress) * 55f
                        }
                        ClipAnimationType.SLIDE_UP -> {
                            animAlpha = animProgress
                            animTransY = (1f - animProgress) * 70f
                        }
                        ClipAnimationType.BOUNCE -> animScale = 0.90f + 0.14f * sin(animProgress * 3.14f)
                        ClipAnimationType.SPIN_IN -> {
                            animScale = 0.68f + 0.32f * animProgress
                            animRot = (1f - animProgress) * 360f
                        }
                        ClipAnimationType.SHAKE_POP -> {
                            animScale = 0.86f + 0.18f * sin(animProgress * 3.14f)
                            animTransX = sin(animProgress * 26f) * 14f * (1f - animProgress)
                        }
                    }

                    // Live Effect kinetic motion (including Zoom On, Zoom Off, Shake, Motion Blur)
                    val fxStrength = project.effectIntensity.coerceIn(0.15f, 1f)
                    val fxScale = when (project.effectPreset) {
                        VideoEffectPreset.ZOOM_PULSE -> 1f + 0.08f * fxStrength * abs(sin(tSec * 4.5f))
                        VideoEffectPreset.HEARTBEAT_ZOOM -> 1f + 0.10f * fxStrength * abs(sin(tSec * 6.5f))
                        VideoEffectPreset.FISHEYE_POP -> 1f + 0.12f * fxStrength
                        VideoEffectPreset.SHAKE_BEAT -> 1f + 0.05f * fxStrength
                        VideoEffectPreset.ZOOM_ON_BURST -> 1f + 0.32f * fxStrength * (localMs.toFloat() / clipDurMs.toFloat()).coerceIn(0f, 1f)
                        VideoEffectPreset.ZOOM_OFF_PULL -> 1f + 0.32f * fxStrength * (1f - (localMs.toFloat() / clipDurMs.toFloat()).coerceIn(0f, 1f))
                        VideoEffectPreset.MOTION_BLUR -> 1.06f
                        else -> 1f
                    }
                    val fxShakeX = if (project.effectPreset == VideoEffectPreset.SHAKE_BEAT) sin(tSec * 28f) * 10f * fxStrength else 0f
                    val fxShakeY = if (project.effectPreset == VideoEffectPreset.SHAKE_BEAT) sin(tSec * 22f) * 8f * fxStrength else 0f

                    // Live Transition kinetic motion near clip exit
                    val transDur = activeClip.transitionAfter.durationMs
                    val transRemaining = if (transDur > 0L && clipDurMs - localMs <= transDur) {
                        ((clipDurMs - localMs).toFloat() / transDur.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val transExitP = if (transDur > 0L && clipDurMs - localMs <= transDur) (1f - transRemaining) else 0f

                    val transScale = when (activeClip.transitionAfter) {
                        TransitionType.ZOOM -> 1f + 0.35f * transExitP
                        TransitionType.PULL_OUT -> 1f - 0.25f * transExitP
                        else -> 1f
                    }
                    val transRot = when (activeClip.transitionAfter) {
                        TransitionType.SPIN -> 180f * transExitP
                        TransitionType.SWIRL -> 35f * transExitP
                        else -> 0f
                    }
                    val transSlideX = when (activeClip.transitionAfter) {
                        TransitionType.SLIDE -> -stageWidthPx * 0.45f * transExitP
                        TransitionType.SWIRL -> -stageWidthPx * 0.3f * transExitP
                        else -> 0f
                    }

                    // Remove BG Backdrop preview when active
                    if (activeClip.removeBgMode != RemoveBgMode.NONE) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(activeClip.customBgHex))
                        )
                    } else if (project.canvasStyle == CanvasStyle.BLUR_BACKDROP && activeClip.isPhoto) {
                        AsyncImage(
                            model = Uri.parse(activeClip.uriString),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isBatterySaver) Modifier else Modifier.blur(18.dp))
                                .alpha(0.55f)
                        )
                    }

                    // Photo crop simulation scale when cropped
                    val photoCropScale = if (activeClip.isPhoto && activeClip.hasCrop && state.activeToolTab != EditorToolTab.CROP) {
                        val wFrac = (activeClip.cropRight - activeClip.cropLeft).coerceIn(0.2f, 1f)
                        val hFrac = (activeClip.cropBottom - activeClip.cropTop).coerceIn(0.2f, 1f)
                        (1f / minOf(wFrac, hFrac)).coerceAtMost(2.5f)
                    } else 1f
                    val photoCropPanX = if (activeClip.isPhoto && activeClip.hasCrop && state.activeToolTab != EditorToolTab.CROP) {
                        (0.5f - (activeClip.cropLeft + activeClip.cropRight) * 0.5f) * stageWidthPx
                    } else 0f
                    val photoCropPanY = if (activeClip.isPhoto && activeClip.hasCrop && state.activeToolTab != EditorToolTab.CROP) {
                        (0.5f - (activeClip.cropTop + activeClip.cropBottom) * 0.5f) * stageHeightPx
                    } else 0f

                    val totalScale = stageTransform.zoomScale * animScale * fxScale * transScale * photoCropScale
                    val latestActiveClip by rememberUpdatedState(activeClip)
                    val latestTab by rememberUpdatedState(state.activeToolTab)

                    val transformModifier = Modifier
                        .fillMaxSize()
                        .alpha(animAlpha)
                        .then(
                            if (activeClip.removeBgMode != RemoveBgMode.NONE) {
                                Modifier.padding(14.dp).clip(RoundedCornerShape(22.dp))
                            } else Modifier
                        )
                        .pointerInput(activeClip.id, stageWidthPx, stageHeightPx) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                if (latestTab == EditorToolTab.CROP || latestTab == EditorToolTab.AI_REMOVER) return@detectTransformGestures
                                val cur = latestActiveClip
                                val nextZoom = (cur.zoomScale * zoom).coerceIn(0.35f, 3.5f)
                                val nextPanX = (cur.panX + pan.x / stageWidthPx.coerceAtLeast(100f)).coerceIn(-0.8f, 0.8f)
                                val nextPanY = (cur.panY + pan.y / stageHeightPx.coerceAtLeast(100f)).coerceIn(-0.8f, 0.8f)
                                val nextRot = (cur.freeRotation + rotation).coerceIn(-180f, 180f)
                                viewModel.updateClipTransform(cur.id, nextZoom, nextPanX, nextPanY, nextRot, saveToUndo = false)
                            }
                        }
                        .graphicsLayer {
                            rotationZ = activeClip.rotationDegrees.toFloat() + stageTransform.freeRotation + animRot + transRot
                            scaleX = if (activeClip.flipHorizontal) -totalScale else totalScale
                            scaleY = if (activeClip.flipVertical) -totalScale else totalScale
                            translationX = stageTransform.panX * stageWidthPx + animTransX + fxShakeX + transSlideX + photoCropPanX
                            translationY = stageTransform.panY * stageHeightPx + animTransY + fxShakeY + photoCropPanY
                        }

                    if (activeClip.isPhoto) {
                        AsyncImage(
                            model = Uri.parse(activeClip.uriString),
                            contentDescription = activeClip.displayName,
                            contentScale = ContentScale.Crop,
                            colorFilter = composeColorFilter,
                            modifier = transformModifier
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    videoPlayer.setVideoTextureView(this)
                                }
                            },
                            modifier = transformModifier
                        )
                    }

                    // Remove BG luminous edge ring preview
                    if (activeClip.removeBgMode != RemoveBgMode.NONE) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .border(2.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        )
                    }

                    // Interactive AI Remover boxes layer (rendered + draggable/resizable in AI_REMOVER tab)
                    if (activeClip.aiRemoverZones.isNotEmpty()) {
                        InteractiveAiRemoverLayer(
                            zones = activeClip.aiRemoverZones,
                            selectedZoneId = state.selectedRemoverZoneId,
                            showHandles = state.activeToolTab == EditorToolTab.AI_REMOVER,
                            stageWidthPx = stageWidthPx,
                            stageHeightPx = stageHeightPx,
                            onSelectZone = { viewModel.selectRemoverZone(it) },
                            onUpdateZone = { viewModel.updateAiRemoverZone(activeClip.id, it) }
                        )
                    }

                    EffectPreviewOverlay(
                        effectPreset = project.effectPreset,
                        effectIntensity = project.effectIntensity,
                        vignette = project.adjustments.vignette,
                        playheadMs = state.playheadMs
                    )

                    TransitionPreviewOverlay(
                        transitionType = activeClip.transitionAfter,
                        progressRemaining = transRemaining
                    )

                    // Interactive Crop 3x3 grid overlay when Crop tab is open
                    if (state.activeToolTab == EditorToolTab.CROP) {
                        InteractiveCropOverlay(
                            clip = activeClip,
                            stageWidthPx = stageWidthPx,
                            stageHeightPx = stageHeightPx,
                            onUpdateCrop = { id, l, t, r, b -> viewModel.updateClipCrop(id, l, t, r, b) }
                        )
                    }
                } else {
                    Text("No clips in timeline", color = TextMuted)
                }

                // Picture-in-Picture Overlays (Drag + Pinch Scale + Rotate + Corner Resize Handle)
                project.pipOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { pip ->
                    val latestPip by rememberUpdatedState(pip)
                    val isSelected = state.selectedPipId == pip.id
                    val offsetX = ((pip.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((pip.normY - 0.5f) * stageHeightPx).roundToInt()
                    val pipClipShape = when (pip.shape) {
                        PipShape.RECTANGLE -> RoundedCornerShape(2.dp)
                        PipShape.ROUNDED -> RoundedCornerShape(12.dp)
                        PipShape.CIRCLE -> CircleShape
                    }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .graphicsLayer { rotationZ = pip.rotationDegrees }
                            .fillMaxWidth(pip.scale.coerceIn(0.16f, 0.78f))
                            .aspectRatio(1f)
                            .alpha(pip.opacity)
                            .clip(pipClipShape)
                            .then(
                                when {
                                    isSelected -> Modifier.border(2.dp, ElectricCyan, pipClipShape)
                                    pip.borderGlow -> Modifier.border(1.5.dp, ElectricCyan.copy(alpha = 0.7f), pipClipShape)
                                    else -> Modifier
                                }
                            )
                            .clickable { viewModel.selectPipOverlay(pip.id) }
                            .pointerInput(pip.id, stageWidthPx, stageHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val cur = latestPip
                                    viewModel.selectPipOverlay(cur.id)
                                    viewModel.updatePipOverlay(
                                        cur.copy(
                                            normX = (cur.normX + pan.x / stageWidthPx.coerceAtLeast(100f)).coerceIn(0.1f, 0.9f),
                                            normY = (cur.normY + pan.y / stageHeightPx.coerceAtLeast(100f)).coerceIn(0.1f, 0.9f),
                                            scale = (cur.scale * zoom).coerceIn(0.16f, 0.78f),
                                            rotationDegrees = (cur.rotationDegrees + rotation) % 360f
                                        )
                                    )
                                }
                            }
                    ) {
                        AsyncImage(
                            model = Uri.parse(pip.uriString),
                            contentDescription = "PiP overlay",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp))
                                    .background(ElectricCyan)
                                    .pointerInput("pip_resize_${pip.id}", stageWidthPx) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val cur = latestPip
                                            viewModel.updatePipOverlay(
                                                cur.copy(scale = (cur.scale + dragAmount.x / stageWidthPx.coerceAtLeast(100f)).coerceIn(0.16f, 0.78f))
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.OpenWith, "Resize PiP", tint = Color(0xFF041418), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }

                // Active Sticker Overlays (Drag + Pinch Scale + Rotate + Resize/Delete Handles)
                project.stickerOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { sticker ->
                    val latestSticker by rememberUpdatedState(sticker)
                    val offsetX = ((sticker.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((sticker.normY - 0.5f) * stageHeightPx).roundToInt()
                    val isSelected = state.selectedStickerId == sticker.id

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .graphicsLayer {
                                scaleX = sticker.scale
                                scaleY = sticker.scale
                                rotationZ = sticker.rotationDegrees
                            }
                            .clickable { viewModel.selectStickerOverlay(sticker.id) }
                            .pointerInput(sticker.id, stageWidthPx, stageHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val cur = latestSticker
                                    viewModel.selectStickerOverlay(cur.id)
                                    viewModel.updateStickerOverlay(
                                        cur.copy(
                                            normX = (cur.normX + pan.x / stageWidthPx.coerceAtLeast(100f)).coerceIn(0.08f, 0.92f),
                                            normY = (cur.normY + pan.y / stageHeightPx.coerceAtLeast(100f)).coerceIn(0.08f, 0.92f),
                                            scale = (cur.scale * zoom).coerceIn(0.45f, 2.8f),
                                            rotationDegrees = (cur.rotationDegrees + rotation) % 360f
                                        )
                                    )
                                }
                            }
                    ) {
                        Surface(
                            color = Color(sticker.accentHex),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(2.dp, Color.White) else null
                        ) {
                            Text(
                                text = sticker.badgeText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(NeonRose)
                                    .clickable { viewModel.deleteStickerOverlay(sticker.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, "Remove sticker", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 8.dp, y = 8.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                                    .pointerInput("sticker_resize_${sticker.id}") {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val cur = latestSticker
                                            viewModel.updateStickerOverlay(
                                                cur.copy(scale = (cur.scale + (dragAmount.x + dragAmount.y) * 0.005f).coerceIn(0.45f, 2.8f))
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.OpenWith, "Resize sticker", tint = Color(0xFF041418), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }

                // Active Text Overlays (Drag + Pinch Scale + Rotate + Quick Edit / Delete / Corner Resize)
                project.textOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { textItem ->
                    val latestText by rememberUpdatedState(textItem)
                    val offsetX = ((textItem.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((textItem.normY - 0.5f) * stageHeightPx).roundToInt()
                    val isSelected = state.selectedTextId == textItem.id
                    val bgColor = Color(textItem.bgHex)
                    val elapsedMs = (state.playheadMs - textItem.startMs).coerceAtLeast(0L)
                    val textAnimP = (elapsedMs / 380f).coerceIn(0f, 1f)
                    val textScaleAnim = when (textItem.animationType) {
                        TextAnimationType.POP, TextAnimationType.BOUNCE_IN -> 0.72f + 0.28f * textAnimP
                        else -> 1f
                    }
                    val textAlphaAnim = when (textItem.animationType) {
                        TextAnimationType.FADE -> textAnimP
                        TextAnimationType.NEON_FLICKER -> 0.65f + 0.35f * abs(sin(elapsedMs / 160f))
                        else -> 1f
                    }
                    val textSlideY = if (textItem.animationType == TextAnimationType.SLIDE_UP) (1f - textAnimP) * 28f else 0f
                    val shownText = if (textItem.animationType == TextAnimationType.TYPEWRITER) {
                        val charCount = ((elapsedMs / 55L).toInt()).coerceIn(1, textItem.text.length.coerceAtLeast(1))
                        textItem.text.take(charCount)
                    } else {
                        textItem.text
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY + textSlideY.roundToInt()) }
                            .graphicsLayer {
                                scaleX = textScaleAnim
                                scaleY = textScaleAnim
                                rotationZ = textItem.rotationDegrees
                                alpha = textAlphaAnim
                            }
                            .clickable { viewModel.selectTextOverlay(textItem.id) }
                            .pointerInput(textItem.id, stageWidthPx, stageHeightPx) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val cur = latestText
                                    viewModel.selectTextOverlay(cur.id)
                                    viewModel.updateTextOverlay(
                                        cur.copy(
                                            normX = (cur.normX + pan.x / stageWidthPx.coerceAtLeast(100f)).coerceIn(0.08f, 0.92f),
                                            normY = (cur.normY + pan.y / stageHeightPx.coerceAtLeast(100f)).coerceIn(0.08f, 0.92f),
                                            scale = (cur.scale * zoom).coerceIn(0.5f, 2.8f),
                                            rotationDegrees = (cur.rotationDegrees + rotation) % 360f
                                        )
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) ElectricCyan else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            StageTextOverlayView(
                                item = textItem,
                                shownText = shownText
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-8).dp, y = (-8).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(NeonRose)
                                    .clickable { viewModel.deleteTextOverlay(textItem.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, "Delete text", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(StudioSurfaceElevated)
                                    .border(1.dp, ElectricCyan, CircleShape)
                                    .clickable { editingTextDialogItem = textItem },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Edit, "Edit text", tint = ElectricCyan, modifier = Modifier.size(12.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 8.dp, y = 8.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                                    .pointerInput("text_resize_${textItem.id}") {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val cur = latestText
                                            viewModel.updateTextOverlay(
                                                cur.copy(scale = (cur.scale + (dragAmount.x + dragAmount.y) * 0.0045f).coerceIn(0.5f, 2.8f))
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.OpenWith, "Resize text", tint = Color(0xFF041418), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Safe-Zone Overlay Guide (TikTok / Reels / Shorts)
                SafeZoneOverlay(mode = state.safeZoneMode)

                // s.s Studio Watermark preview badge
                if (project.addWatermark) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        color = Color(0xA60A0E16),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "s.s STUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Eco Preview Pill in Battery Saver Mode
                if (isBatterySaver) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "Eco Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioEmerald,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Toast notification pill over stage
            androidx.compose.animation.AnimatedVisibility(
                visible = state.statusToast != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Surface(
                    color = StudioSurfaceElevated.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = state.statusToast.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Transport Control Bar (Timecode | Play/Pause | Mark | Keyframe ◆ | Undo/Redo | Fullscreen)
        val playheadHasKeyframe = remember(activeSlice, activeClip?.keyframes) {
            val localMs = activeSlice?.localEffectiveOffsetMs ?: -1L
            activeClip?.keyframes?.any { abs(it.localMs - localMs) < 180L } == true
        }
        val playheadHasMarker = remember(project.markers, state.playheadMs) {
            project.markers.any { abs(it.timeMs - state.playheadMs) < 250L }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${formatTimecode(state.playheadMs)} / ${formatTimecode(project.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                val safeActive = state.safeZoneMode != SafeZoneMode.NONE
                Surface(
                    modifier = Modifier
                        .testTag("editor_safe_zone_button")
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.cycleSafeZoneMode() },
                    color = if (safeActive) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (safeActive) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = if (safeActive) "Safe:${state.safeZoneMode.label}" else "Safe",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (safeActive) ElectricCyan else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .testTag("editor_play_pause_button")
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(StudioSurfaceElevated)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.toggleMarkerAtPlayhead() },
                    modifier = Modifier.testTag("editor_mark_button").size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdd,
                        contentDescription = "Add timeline marker",
                        tint = if (playheadHasMarker) StudioAmber else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleKeyframeAtPlayhead() },
                    modifier = Modifier.testTag("editor_keyframe_button").size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Diamond,
                        contentDescription = "Add or remove keyframe",
                        tint = if (playheadHasKeyframe) StudioAmber else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier.testTag("editor_undo_button").size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.canUndo) TextPrimary else TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = state.canRedo,
                    modifier = Modifier.testTag("editor_redo_button").size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo) TextPrimary else TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFullscreenPreview() },
                    modifier = Modifier.testTag("editor_fullscreen_button").size(38.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFullscreenPreview) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle fullscreen preview",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Multi-Track Timeline + Bottom Toolbar (hidden in fullscreen)
        if (!state.isFullscreenPreview) {
            EditorTimeline(
                project = project,
                playheadMs = state.playheadMs,
                isPlaying = state.isPlaying,
                selectedClipId = state.selectedClipId,
                selectedTextId = state.selectedTextId,
                selectedStickerId = state.selectedStickerId,
                onSeekMs = { viewModel.seekTo(it) },
                onSelectClip = { viewModel.selectClip(it) },
                onSelectText = { viewModel.selectTextOverlay(it) },
                onSelectSticker = { viewModel.selectStickerOverlay(it) },
                onUpdateClipTrim = { id, s, e -> viewModel.updateClipTrim(id, s, e) },
                onMoveClip = { id, delta -> viewModel.moveClip(id, delta) },
                onUpdateTextTiming = { viewModel.updateTextOverlay(it) },
                onUpdateStickerTiming = { viewModel.updateStickerOverlay(it) },
                onAddClipClick = {
                    appendClipsLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onOpenCoverSheet = { showCoverSheet = true },
                onOpenTransitionPicker = { transitionTargetClip = it },
                onToggleOriginalMute = { viewModel.toggleMuteOriginalAudio() },
                onOpenAudioTool = { viewModel.setToolTab(EditorToolTab.AUDIO) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.32f)
            )

            EditorBottomToolbar(
                activeTab = state.activeToolTab,
                project = project,
                aiLabState = aiLabState,
                selectedClip = project.clips.find { it.id == state.selectedClipId },
                selectedText = project.textOverlays.find { it.id == state.selectedTextId },
                selectedSticker = project.stickerOverlays.find { it.id == state.selectedStickerId },
                selectedPip = project.pipOverlays.find { it.id == state.selectedPipId },
                selectedRemoverZoneId = state.selectedRemoverZoneId,
                onSelectTab = { viewModel.setToolTab(it) },
                onToggleAutoEnhance = { viewModel.toggleAutoEnhanceVideo() },
                onRunAiVideoEnhance = { viewModel.runAiVideoEnhance() },
                onApplyAiEnhance = { viewModel.applyAiEnhanceRecommendation(it) },
                onRunAiCaptions = { viewModel.runAiSmartCaptions(it) },
                onApplyAiCaptions = { viewModel.applyAiCaptions(it) },
                onStreamAiScript = { topic, tone -> viewModel.streamAiScript(topic, tone) },
                onTranslateVideo = { dialogue, lang -> viewModel.runAiVideoTranslate(dialogue, lang) },
                onApplyTranslation = { viewModel.applyAiTranslatedCaptions(it) },
                onResetAiState = { viewModel.resetAiLabState() },
                onSelectVoicePreset = { viewModel.selectVoicePreset(it) },
                onUpdateVoiceSettings = { viewModel.updateVoiceSettings(it) },
                onSplitClip = { viewModel.splitSelectedClipAtPlayhead() },
                onFreezeFrame = { viewModel.freezeFrameAtPlayhead() },
                onReplaceClipClick = {
                    replaceClipLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onUpdateTrim = { id, s, e -> viewModel.updateClipTrim(id, s, e) },
                onUpdateSpeed = { id, spd -> viewModel.updateClipSpeed(id, spd) },
                onUpdateSpeedCurve = { id, curve -> viewModel.updateClipSpeedCurve(id, curve) },
                onUpdateClipAnimation = { id, anim, dur -> viewModel.updateClipAnimation(id, anim, dur) },
                onUpdateClipVolume = { id, vol -> viewModel.updateClipVolume(id, vol) },
                onRotateClip = { viewModel.rotateClip(it) },
                onFlipClip = { viewModel.flipClipHorizontal(it) },
                onFlipClipVertical = { viewModel.flipClipVertical(it) },
                onUpdateClipZoom = { id, z -> viewModel.updateClipZoom(id, z) },
                onUpdateClipTransform = { id, z, px, py, rot -> viewModel.updateClipTransform(id, z, px, py, rot, saveToUndo = false) },
                onResetClipTransform = { viewModel.resetClipTransform(it) },
                onApplyZoomOnOff = { id, zoomOn -> viewModel.applyZoomOnOffPreset(id, zoomOn) },
                onToggleKeyframe = { viewModel.toggleKeyframeAtPlayhead() },
                onUpdateClipCrop = { id, l, t, r, b -> viewModel.updateClipCrop(id, l, t, r, b) },
                onResetClipCrop = { viewModel.resetClipCrop(it) },
                onUpdateRemoveBg = { id, mode, keyHex, sens, bgHex ->
                    viewModel.updateClipRemoveBg(id, mode, keyHex, sens, bgHex)
                },
                onAddRemoverZone = { id, mode -> viewModel.addAiRemoverZone(id, mode) },
                onUpdateRemoverZone = { id, zone -> viewModel.updateAiRemoverZone(id, zone) },
                onDeleteRemoverZone = { id, zId -> viewModel.deleteAiRemoverZone(id, zId) },
                onMoveClip = { id, delta -> viewModel.moveClip(id, delta) },
                onDuplicateClip = { viewModel.duplicateClip(it) },
                onDeleteClip = { viewModel.deleteClip(it) },
                onSelectMusicPreset = { viewModel.selectStudioMusic(it) },
                onImportAudioClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onUpdateAudioVolume = { viewModel.updateAudioVolume(it) },
                onToggleAudioMute = { viewModel.toggleAudioMute() },
                onToggleAudioFades = { enable ->
                    viewModel.updateAudioFades(if (enable) 500L else 0L, if (enable) 700L else 0L)
                },
                onRemoveAudio = { viewModel.removeAudioTrack() },
                onAddSfx = { viewModel.addSfxAtPlayhead(it) },
                onDeleteSfx = { viewModel.deleteSfxOverlay(it) },
                onAutoBeatMarkers = { bpm, rhythmCut -> viewModel.applyAutoBeatMarkers(bpm, rhythmCut) },
                onAddText = { txt, style, anim, fontKey, curved, is3d, neon, rtl, lang ->
                    viewModel.addTextOverlay(
                        text = txt,
                        preset = style,
                        animation = anim,
                        fontFamilyKey = fontKey,
                        isCurved = curved,
                        is3D = is3d,
                        isNeonGlow = neon,
                        isRtl = rtl,
                        languageTag = lang
                    )
                },
                onGenerateAutoCaptions = { viewModel.generateAutoCaptions() },
                onUpdateText = { viewModel.updateTextOverlay(it) },
                onDuplicateText = { viewModel.duplicateTextOverlay(it) },
                onDeleteText = { viewModel.deleteTextOverlay(it) },
                onAddSticker = { viewModel.addStickerOverlay(it) },
                onUpdateSticker = { viewModel.updateStickerOverlay(it) },
                onDeleteSticker = { viewModel.deleteStickerOverlay(it) },
                onAddPipClick = {
                    pipPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onUpdatePip = { viewModel.updatePipOverlay(it) },
                onDeletePip = { viewModel.deletePipOverlay(it) },
                onSelectEffect = { fx, intensity -> viewModel.updateEffect(fx, intensity) },
                onSelectFilter = { preset, intensity -> viewModel.updateFilter(preset, intensity) },
                onUpdateAdjustments = { viewModel.updateAdjustments(it) },
                onResetAdjustments = { viewModel.resetAdjustments() },
                onSmartAutoCropRatio = { ratio, smartCrop -> viewModel.applyOneClickRatioAndSmartCrop(ratio, smartCrop) },
                onSelectCanvasColor = { viewModel.updateCanvasColor(it) },
                onSelectCanvasStyle = { viewModel.updateCanvasStyle(it) }
            )
        }
    }

    // Video Cover Sheet
    if (showCoverSheet) {
        VideoCoverBottomSheet(
            project = project,
            onCaptureAtPlayhead = { title -> viewModel.captureCoverFromPlayhead(title) },
            onPickCoverImage = {
                coverPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSaveCoverConfig = { viewModel.updateCoverConfig(it) },
            onDismiss = { showCoverSheet = false }
        )
    }

    // Transition Picker Bottom Sheet (12 CapCut Transitions)
    transitionTargetClip?.let { clip ->
        ModalBottomSheet(
            onDismissRequest = { transitionTargetClip = null },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Clip Transition", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                TransitionType.entries.chunked(3).forEach { rowTransitions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTransitions.forEach { t ->
                            val selected = clip.transitionAfter == t
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.updateClipTransition(clip.id, t)
                                        transitionTargetClip = null
                                    },
                                color = if (selected) ElectricCyan else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                            ) {
                                Text(
                                    text = t.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color(0xFF041418) else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // Quick stage inline text edit dialog
    editingTextDialogItem?.let { item ->
        var draft by remember(item.id) { mutableStateOf(item.text) }
        AlertDialog(
            onDismissRequest = { editingTextDialogItem = null },
            containerColor = StudioSurfaceElevated,
            title = { Text("Edit Text", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) {
                        viewModel.updateTextOverlay(item.copy(text = draft.trim()))
                    }
                    editingTextDialogItem = null
                }) {
                    Text("Apply", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTextDialogItem = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showRenameDialog) {
        var text by remember(project.title) { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Project Title", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameProject(project.id, text)
                    showRenameDialog = false
                }) {
                    Text("Save", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showExportSheet) {
        ExportBottomSheet(
            project = project,
            exportState = exportState,
            isBatterySaver = isBatterySaver,
            onUpdateExportSettings = { res, fps -> viewModel.updateExportSettings(res, fps) },
            onToggleWatermark = { viewModel.toggleProjectWatermark(it) },
            onStartExport = { viewModel.startExport() },
            onCancelExport = { viewModel.cancelExport() },
            onPublishToTikTok = onPublishExportToTikTok,
            onDismiss = {
                showExportSheet = false
                if (exportState !is ExportUiState.Exporting) {
                    viewModel.resetExportState()
                }
            }
        )
    }
}
