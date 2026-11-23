package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.AudioSynthLibrary
import com.example.media.MediaInspector
import com.example.model.ClipAnimationType
import com.example.model.ProjectData
import com.example.model.StickerOverlayItem
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.ui.components.AudioWaveformStrip
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import kotlin.math.roundToInt

private val DP_PER_SECOND = 56.dp

@Composable
fun EditorTimeline(
    project: ProjectData,
    playheadMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedTextId: String?,
    selectedStickerId: String?,
    onSeekMs: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onSelectText: (String) -> Unit,
    onSelectSticker: (String) -> Unit,
    onUpdateClipTrim: (String, Long, Long) -> Unit,
    onMoveClip: (String, Int) -> Unit,
    onUpdateTextTiming: (TextOverlayItem) -> Unit,
    onUpdateStickerTiming: (StickerOverlayItem) -> Unit,
    onAddClipClick: () -> Unit,
    onOpenCoverSheet: () -> Unit,
    onOpenTransitionPicker: (VideoClip) -> Unit,
    onToggleOriginalMute: () -> Unit,
    onOpenAudioTool: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerMs = remember(density) { with(density) { DP_PER_SECOND.toPx() / 1000f } }

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Sync scroll position from playheadMs when playing or external seek happens
    LaunchedEffect(playheadMs, isPlaying) {
        val targetPx = (playheadMs * pxPerMs).roundToInt()
        if (kotlin.math.abs(scrollState.value - targetPx) > 3 && !scrollState.isScrollInProgress) {
            isProgrammaticScroll = true
            scrollState.scrollTo(targetPx)
            isProgrammaticScroll = false
        }
    }

    // Sync playheadMs when user manually drags timeline
    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && !isProgrammaticScroll) {
            val ms = (scrollState.value / pxPerMs).toLong().coerceIn(0L, project.totalDurationMs)
            onSeekMs(ms)
        }
    }

    val totalSec = ((project.totalDurationMs / 1000f) + 1f).toInt().coerceAtLeast(4)
    val trackWidthDp: Dp = (DP_PER_SECOND * (project.totalDurationMs / 1000f).coerceAtLeast(1f))

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioBackground)
    ) {
        val halfViewportDp = maxWidth / 2f

        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = halfViewportDp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Time Ruler + User Markers ("Marks")
                    Box(
                        modifier = Modifier
                            .width(trackWidthDp + 80.dp)
                            .height(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (sec in 0..totalSec) {
                                Box(
                                    modifier = Modifier.width(DP_PER_SECOND),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = if (sec % 2 == 0) "${sec}s" else "·",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (sec % 2 == 0) TextMuted else StudioBorder
                                    )
                                }
                            }
                        }

                        project.markers.forEach { marker ->
                            val markerOffsetDp = DP_PER_SECOND * (marker.timeMs / 1000f)
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = marker.label,
                                tint = Color(marker.colorHex),
                                modifier = Modifier
                                    .offset(x = markerOffsetDp - 7.dp, y = 2.dp)
                                    .size(16.dp)
                                    .clickable { onSeekMs(marker.timeMs) }
                            )
                        }
                    }

                    // 2. Main Video Track with CapCut Pro "Cover" Slot at start + Draggable Trim & Long-Press Reorder
                    Row(
                        modifier = Modifier.height(58.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Video Cover Card pinned before first clip (exact CapCut Pro UX)
                        val coverPreviewUri = project.coverConfig.coverImageUri
                            ?: project.clips.firstOrNull()?.uriString
                        Surface(
                            modifier = Modifier
                                .testTag("timeline_cover_slot")
                                .width(44.dp)
                                .fillMaxHeight()
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenCoverSheet),
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (coverPreviewUri != null && project.coverConfig.coverImageUri != null) {
                                    AsyncImage(
                                        model = Uri.parse(coverPreviewUri),
                                        contentDescription = "Video cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.PhotoCamera,
                                        contentDescription = "Set Cover",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cover",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        project.clips.forEachIndexed { idx, clip ->
                            val clipWidthDp = (DP_PER_SECOND * (clip.effectiveDurationMs / 1000f))
                                .coerceAtLeast(52.dp)
                            val isSelected = clip.id == selectedClipId

                            TimelineClipCell(
                                clip = clip,
                                widthDp = clipWidthDp,
                                isSelected = isSelected,
                                pxPerMs = pxPerMs,
                                onClick = { onSelectClip(if (isSelected) null else clip.id) },
                                onDragTrim = { startDeltaMs, endDeltaMs ->
                                    onUpdateClipTrim(
                                        clip.id,
                                        clip.trimStartMs + startDeltaMs,
                                        clip.trimEndMs + endDeltaMs
                                    )
                                },
                                onLongDragSwap = { dir ->
                                    onMoveClip(clip.id, dir)
                                }
                            )

                            if (idx < project.clips.lastIndex) {
                                val hasTrans = clip.transitionAfter != TransitionType.NONE
                                Box(
                                    modifier = Modifier
                                        .testTag("transition_btn_${clip.id}")
                                        .size(26.dp)
                                        .offset(x = (-1).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (hasTrans) ElectricCyan else StudioSurfaceElevated)
                                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                        .clickable { onOpenTransitionPicker(clip) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesomeMotion,
                                        contentDescription = "Clip transition",
                                        tint = if (hasTrans) Color(0xFF041418) else TextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier
                                .testTag("timeline_add_clip_button")
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onAddClipClick),
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add clip",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }

                    // 3. Draggable Text & Sticker Track
                    if (project.textOverlays.isNotEmpty() || project.stickerOverlays.isNotEmpty()) {
                        val totalDur = project.totalDurationMs.coerceAtLeast(3000L)
                        Box(
                            modifier = Modifier
                                .width(trackWidthDp)
                                .height(26.dp)
                        ) {
                            project.textOverlays.forEach { textItem ->
                                val offsetDp = DP_PER_SECOND * (textItem.startMs / 1000f)
                                val segWidthDp = (DP_PER_SECOND * ((textItem.endMs - textItem.startMs) / 1000f))
                                    .coerceAtLeast(52.dp)
                                val selected = textItem.id == selectedTextId
                                Surface(
                                    modifier = Modifier
                                        .offset(x = offsetDp)
                                        .width(segWidthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onSelectText(textItem.id) }
                                        .pointerInput(textItem.id, pxPerMs, totalDur) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaMs = (dragAmount / pxPerMs).toLong()
                                                val dur = (textItem.endMs - textItem.startMs).coerceAtLeast(500L)
                                                val newStart = (textItem.startMs + deltaMs).coerceIn(0L, (totalDur - dur).coerceAtLeast(0L))
                                                onUpdateTextTiming(textItem.copy(startMs = newStart, endMs = newStart + dur))
                                            }
                                        },
                                    color = if (selected) NeonRose else NeonRose.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = if (selected) BorderStroke(1.5.dp, Color.White) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Title,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = textItem.text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            project.stickerOverlays.forEach { sticker ->
                                val offsetDp = DP_PER_SECOND * (sticker.startMs / 1000f)
                                val segWidthDp = (DP_PER_SECOND * ((sticker.endMs - sticker.startMs) / 1000f))
                                    .coerceAtLeast(52.dp)
                                val selected = sticker.id == selectedStickerId
                                Surface(
                                    modifier = Modifier
                                        .offset(x = offsetDp)
                                        .width(segWidthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onSelectSticker(sticker.id) }
                                        .pointerInput(sticker.id, pxPerMs, totalDur) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaMs = (dragAmount / pxPerMs).toLong()
                                                val dur = (sticker.endMs - sticker.startMs).coerceAtLeast(500L)
                                                val newStart = (sticker.startMs + deltaMs).coerceIn(0L, (totalDur - dur).coerceAtLeast(0L))
                                                onUpdateStickerTiming(sticker.copy(startMs = newStart, endMs = newStart + dur))
                                            }
                                        },
                                    color = if (selected) StudioAmber else StudioAmber.copy(alpha = 0.38f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = if (selected) BorderStroke(1.5.dp, Color.White) else null
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = sticker.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Audio Track with Golden Beat Sync Markers
                    val track = project.audioTrack
                    if (track != null) {
                        val presetMeta = remember(track.id) {
                            AudioSynthLibrary.presets.find { it.id == track.id }
                        }
                        val presetWaveform = presetMeta?.waveform
                            ?: List(36) { idx -> if (idx % 2 == 0) 0.72f else 0.42f }
                        val bpm = presetMeta?.bpm ?: 120
                        val beatIntervalMs = (60_000L / bpm.coerceAtLeast(60)).coerceAtLeast(350L)

                        Box(
                            modifier = Modifier
                                .width(trackWidthDp.coerceAtLeast(160.dp))
                                .height(32.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenAudioTool),
                                color = ElectricCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (track.isMuted) "${track.title} (Muted)" else track.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AudioWaveformStrip(
                                        waveform = presetWaveform,
                                        barColor = if (track.isMuted) TextMuted else ElectricCyan.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(20.dp)
                                    )
                                }
                            }
                            val totalAudioMs = project.totalDurationMs.coerceAtLeast(2000L)
                            var beatCursor = beatIntervalMs
                            while (beatCursor < totalAudioMs) {
                                val beatOffsetDp = DP_PER_SECOND * (beatCursor / 1000f)
                                Box(
                                    modifier = Modifier
                                        .offset(x = beatOffsetDp, y = 26.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(StudioAmber)
                                )
                                beatCursor += beatIntervalMs
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .testTag("timeline_add_audio_chip")
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenAudioTool),
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Add audio",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // 5. SFX Lane (Sound FX added at playhead)
                    if (project.sfxOverlays.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(trackWidthDp.coerceAtLeast(160.dp))
                                .height(22.dp)
                        ) {
                            project.sfxOverlays.forEach { sfx ->
                                val offsetDp = DP_PER_SECOND * (sfx.startMs / 1000f)
                                Surface(
                                    modifier = Modifier
                                        .offset(x = offsetDp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = onOpenAudioTool),
                                    color = NeonRose.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.65f))
                                ) {
                                    Text(
                                        text = "♪ ${sfx.title}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Left floating mute toggle (CapCut-style)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .testTag("timeline_mute_clip_audio")
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleOriginalMute),
                    color = StudioSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (project.isOriginalAudioMuted)
                                Icons.AutoMirrored.Filled.VolumeOff
                            else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute clip audio",
                            tint = if (project.isOriginalAudioMuted) NeonRose else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Fixed Center Vertical Playhead Line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TimelineClipCell(
    clip: VideoClip,
    widthDp: Dp,
    isSelected: Boolean,
    pxPerMs: Float,
    onClick: () -> Unit,
    onDragTrim: (startDeltaMs: Long, endDeltaMs: Long) -> Unit,
    onLongDragSwap: (delta: Int) -> Unit
) {
    val context = LocalContext.current
    val videoThumb by produceState<Bitmap?>(initialValue = null, clip.id, clip.trimStartMs) {
        value = if (!clip.isPhoto) MediaInspector.extractThumbnail(context, clip) else null
    }
    var dragReorderAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .width(widthDp)
            .fillMaxHeight()
            .testTag("timeline_clip_${clip.id}")
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceHighlight)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color.White else StudioBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .pointerInput(clip.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragReorderAccum = 0f },
                    onDragEnd = { dragReorderAccum = 0f },
                    onDragCancel = { dragReorderAccum = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    dragReorderAccum += dragAmount.x
                    if (dragReorderAccum > 90f) {
                        onLongDragSwap(1)
                        dragReorderAccum = 0f
                    } else if (dragReorderAccum < -90f) {
                        onLongDragSwap(-1)
                        dragReorderAccum = 0f
                    }
                }
            }
    ) {
        if (clip.isPhoto) {
            AsyncImage(
                model = Uri.parse(clip.uriString),
                contentDescription = clip.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (videoThumb != null) {
            Image(
                bitmap = videoThumb!!.asImageBitmap(),
                contentDescription = clip.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        if (isSelected) {
            // Interactive Draggable Left Trim Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .pointerInput(clip.id, pxPerMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount / pxPerMs).toLong()
                            onDragTrim(deltaMs, 0L)
                        }
                    }
            )
            // Interactive Draggable Right Trim Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .pointerInput(clip.id, pxPerMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount / pxPerMs).toLong()
                            onDragTrim(0L, deltaMs)
                        }
                    }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = String.format("%.1fs", clip.effectiveDurationMs / 1000f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            if (kotlin.math.abs(clip.speed - 1.0f) > 0.05f) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", clip.speed)}x",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF041418),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (clip.animation != ClipAnimationType.NONE) {
                Surface(
                    color = VioletPulse.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ANI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (clip.keyframes.isNotEmpty()) {
                Surface(
                    color = StudioAmber.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "◆${clip.keyframes.size}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF041418),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
