package com.example.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.media.ExportUiState
import com.example.media.MediaEffectBuilder
import com.example.model.EditorToolTab
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.ui.components.formatTimecode
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    modifier: Modifier = Modifier
) {
    val state by viewModel.editorState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val project = state.project
    val context = LocalContext.current

    var showExportSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var transitionTargetClip by remember { mutableStateOf<VideoClip?>(null) }

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

    // Video ExoPlayer for real video clips
    val videoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Music ExoPlayer for studio soundtrack
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            audioPlayer.release()
        }
    }

    // Sync video clip to videoPlayer when active video clip changes
    val activeClip = activeSlice?.clip
    LaunchedEffect(
        activeClip?.id,
        activeClip?.uriString,
        activeClip?.isPhoto,
        project.filterPreset,
        project.filterIntensity,
        project.effectPreset,
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

    // Sync speed, volume, play state, and seek for videoPlayer
    LaunchedEffect(
        state.isPlaying,
        activeClip?.id,
        activeClip?.speed,
        activeClip?.volume,
        project.isOriginalAudioMuted
    ) {
        if (activeClip != null && !activeClip.isPhoto) {
            videoPlayer.playbackParameters = PlaybackParameters(activeClip.speed.coerceIn(0.25f, 4.0f))
            videoPlayer.volume = if (project.isOriginalAudioMuted) 0f else activeClip.volume.coerceIn(0f, 1.5f)
            if (state.isPlaying) {
                val targetPos = activeClip.trimStartMs + (activeSlice.localEffectiveOffsetMs * activeClip.speed).toLong()
                if (kotlin.math.abs(videoPlayer.currentPosition - targetPos) > 250L) {
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

    // Seek videoPlayer smoothly when scrubbed while paused
    LaunchedEffect(state.playheadMs, state.isPlaying, activeClip?.id) {
        if (!state.isPlaying && activeClip != null && !activeClip.isPhoto && activeSlice != null) {
            val targetPos = activeClip.trimStartMs + (activeSlice.localEffectiveOffsetMs * activeClip.speed).toLong()
            videoPlayer.seekTo(targetPos)
        }
    }

    // Sync audioTrack to audioPlayer
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
                if (kotlin.math.abs(audioPlayer.currentPosition - state.playheadMs) > 400L) {
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

    // Playhead ticker loop while playing
    LaunchedEffect(state.isPlaying, project.totalDurationMs) {
        if (state.isPlaying && project.totalDurationMs > 0L) {
            var lastTick = System.currentTimeMillis()
            while (state.isPlaying) {
                delay(33L)
                val now = System.currentTimeMillis()
                val delta = (now - lastTick).coerceIn(10L, 100L)
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

    // Auto-clear toast
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            text = "${project.resolution.label} · ${project.fps}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 2. Video Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (state.isFullscreenPreview) 1f else 0.46f)
                .background(Color(0xFF06070A))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(project.aspectRatio.ratioWidth / project.aspectRatio.ratioHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(project.canvasColorHex))
                    .border(1.dp, StudioBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val stageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val stageHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

                if (activeClip != null) {
                    val transformModifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = activeClip.rotationDegrees.toFloat()
                            scaleX = if (activeClip.flipHorizontal) -activeClip.zoomScale else activeClip.zoomScale
                            scaleY = activeClip.zoomScale
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

                    // Vignette overlay if adjusted
                    if (project.adjustments.vignette > 0.05f || project.effectPreset.name == "VIGNETTE_CINEMA") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 18.dp,
                                    color = Color.Black.copy(alpha = (project.adjustments.vignette + 0.35f).coerceAtMost(0.75f))
                                )
                        )
                    }
                } else {
                    Text("No clips in timeline", color = TextMuted)
                }

                // Picture-in-Picture Overlays
                project.pipOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { pip ->
                    val offsetX = ((pip.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((pip.normY - 0.5f) * stageHeightPx).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .fillMaxWidth(pip.scale.coerceIn(0.18f, 0.7f))
                            .aspectRatio(1f)
                            .alpha(pip.opacity)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, ElectricCyan, RoundedCornerShape(10.dp))
                            .pointerInput(pip.id, stageWidthPx, stageHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val nx = (pip.normX + dragAmount.x / stageWidthPx).coerceIn(0.15f, 0.85f)
                                    val ny = (pip.normY + dragAmount.y / stageHeightPx).coerceIn(0.15f, 0.85f)
                                    viewModel.updatePipOverlay(pip.copy(normX = nx, normY = ny))
                                }
                            }
                    ) {
                        AsyncImage(
                            model = Uri.parse(pip.uriString),
                            contentDescription = "PiP overlay",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Active Sticker Overlays (Draggable)
                project.stickerOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { sticker ->
                    val offsetX = ((sticker.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((sticker.normY - 0.5f) * stageHeightPx).roundToInt()
                    val isSelected = state.selectedStickerId == sticker.id
                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .clickable { viewModel.selectStickerOverlay(sticker.id) }
                            .pointerInput(sticker.id, stageWidthPx, stageHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val nx = (sticker.normX + dragAmount.x / stageWidthPx).coerceIn(0.1f, 0.9f)
                                    val ny = (sticker.normY + dragAmount.y / stageHeightPx).coerceIn(0.1f, 0.9f)
                                    viewModel.updateStickerOverlay(sticker.copy(normX = nx, normY = ny))
                                }
                            },
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
                }

                // Active Text Overlays (Draggable)
                project.textOverlays.filter {
                    state.playheadMs in it.startMs..it.endMs
                }.forEach { textItem ->
                    val offsetX = ((textItem.normX - 0.5f) * stageWidthPx).roundToInt()
                    val offsetY = ((textItem.normY - 0.5f) * stageHeightPx).roundToInt()
                    val isSelected = state.selectedTextId == textItem.id
                    val bgColor = Color(textItem.bgHex)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) ElectricCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectTextOverlay(textItem.id) }
                            .pointerInput(textItem.id, stageWidthPx, stageHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val nx = (textItem.normX + dragAmount.x / stageWidthPx).coerceIn(0.1f, 0.9f)
                                    val ny = (textItem.normY + dragAmount.y / stageHeightPx).coerceIn(0.1f, 0.9f)
                                    viewModel.updateTextOverlay(textItem.copy(normX = nx, normY = ny))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = textItem.text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = (18f * textItem.scale).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(textItem.colorHex)
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

        // 3. Transport Control Bar (Timecode | Play/Pause | Undo/Redo | Fullscreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioBackground)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatTimecode(state.playheadMs)} / ${formatTimecode(project.totalDurationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

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
                    onClick = { viewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.canUndo) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = state.canRedo,
                    modifier = Modifier.testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.canRedo) TextPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleFullscreenPreview() },
                    modifier = Modifier.testTag("editor_fullscreen_button")
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
                onAddClipClick = {
                    appendClipsLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
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
                selectedClip = project.clips.find { it.id == state.selectedClipId },
                selectedText = project.textOverlays.find { it.id == state.selectedTextId },
                selectedSticker = project.stickerOverlays.find { it.id == state.selectedStickerId },
                selectedPip = project.pipOverlays.find { it.id == state.selectedPipId },
                onSelectTab = { viewModel.setToolTab(it) },
                onSplitClip = { viewModel.splitSelectedClipAtPlayhead() },
                onUpdateTrim = { id, s, e -> viewModel.updateClipTrim(id, s, e) },
                onUpdateSpeed = { id, spd -> viewModel.updateClipSpeed(id, spd) },
                onUpdateClipVolume = { id, vol -> viewModel.updateClipVolume(id, vol) },
                onRotateClip = { viewModel.rotateClip(it) },
                onFlipClip = { viewModel.flipClipHorizontal(it) },
                onUpdateClipZoom = { id, z -> viewModel.updateClipZoom(id, z) },
                onMoveClip = { id, delta -> viewModel.moveClip(id, delta) },
                onDuplicateClip = { viewModel.duplicateClip(it) },
                onDeleteClip = { viewModel.deleteClip(it) },
                onSelectMusicPreset = { viewModel.selectStudioMusic(it) },
                onImportAudioClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onUpdateAudioVolume = { viewModel.updateAudioVolume(it) },
                onToggleAudioMute = { viewModel.toggleAudioMute() },
                onRemoveAudio = { viewModel.removeAudioTrack() },
                onAddText = { txt, style, anim -> viewModel.addTextOverlay(txt, style, anim) },
                onGenerateAutoCaptions = { viewModel.generateAutoCaptions() },
                onUpdateText = { viewModel.updateTextOverlay(it) },
                onDeleteText = { viewModel.deleteTextOverlay(it) },
                onAddSticker = { viewModel.addStickerOverlay(it) },
                onDeleteSticker = { viewModel.deleteStickerOverlay(it) },
                onAddPipClick = {
                    pipPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onUpdatePip = { viewModel.updatePipOverlay(it) },
                onDeletePip = { viewModel.deletePipOverlay(it) },
                onSelectEffect = { viewModel.updateEffect(it) },
                onSelectFilter = { preset, intensity -> viewModel.updateFilter(preset, intensity) },
                onUpdateAdjustments = { viewModel.updateAdjustments(it) },
                onResetAdjustments = { viewModel.resetAdjustments() },
                onSelectRatio = { viewModel.updateAspectRatio(it) },
                onSelectCanvasColor = { viewModel.updateCanvasColor(it) }
            )
        }
    }

    // Transition Picker Bottom Sheet
    transitionTargetClip?.let { clip ->
        ModalBottomSheet(
            onDismissRequest = { transitionTargetClip = null },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Clip Transition", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransitionType.entries.forEach { t ->
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = t.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color(0xFF041418) else TextPrimary,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
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
            onUpdateExportSettings = { res, fps -> viewModel.updateExportSettings(res, fps) },
            onStartExport = { viewModel.startExport() },
            onCancelExport = { viewModel.cancelExport() },
            onDismiss = {
                showExportSheet = false
                if (exportState !is ExportUiState.Exporting) {
                    viewModel.resetExportState()
                }
            }
        )
    }
}
