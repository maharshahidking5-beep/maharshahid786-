package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import com.example.model.AiRemoverMode
import com.example.model.AiRemoverZone
import com.example.model.ProjectData
import com.example.model.SafeZoneMode
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoCoverConfig
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioFontCatalog
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

data class StageTransformValues(
    val zoomScale: Float,
    val panX: Float,
    val panY: Float,
    val freeRotation: Float
)

fun interpolateClipTransform(clip: VideoClip, localMs: Long): StageTransformValues {
    val sorted = clip.keyframes.sortedBy { it.localMs }
    if (sorted.isEmpty()) {
        return StageTransformValues(
            zoomScale = clip.zoomScale,
            panX = clip.panX,
            panY = clip.panY,
            freeRotation = clip.freeRotation
        )
    }
    if (localMs <= sorted.first().localMs) {
        val k = sorted.first()
        return StageTransformValues(k.zoomScale, k.panX, k.panY, k.rotation)
    }
    if (localMs >= sorted.last().localMs) {
        val k = sorted.last()
        return StageTransformValues(k.zoomScale, k.panX, k.panY, k.rotation)
    }
    for (i in 0 until sorted.lastIndex) {
        val a = sorted[i]
        val b = sorted[i + 1]
        if (localMs in a.localMs..b.localMs) {
            val span = (b.localMs - a.localMs).coerceAtLeast(1L).toFloat()
            val t = ((localMs - a.localMs).toFloat() / span).coerceIn(0f, 1f)
            return StageTransformValues(
                zoomScale = a.zoomScale + (b.zoomScale - a.zoomScale) * t,
                panX = a.panX + (b.panX - a.panX) * t,
                panY = a.panY + (b.panY - a.panY) * t,
                freeRotation = a.rotation + (b.rotation - a.rotation) * t
            )
        }
    }
    return StageTransformValues(clip.zoomScale, clip.panX, clip.panY, clip.freeRotation)
}

@Composable
fun InteractiveCropOverlay(
    clip: VideoClip,
    stageWidthPx: Float,
    stageHeightPx: Float,
    onUpdateCrop: (String, Float, Float, Float, Float) -> Unit
) {
    val latestClip by rememberUpdatedState(clip)
    val safeW = stageWidthPx.coerceAtLeast(100f)
    val safeH = stageHeightPx.coerceAtLeast(100f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dim areas outside the active crop rectangle & draw 3x3 CapCut thirds grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val l = latestClip.cropLeft * size.width
            val t = latestClip.cropTop * size.height
            val r = latestClip.cropRight * size.width
            val b = latestClip.cropBottom * size.height
            val maskColor = Color.Black.copy(alpha = 0.56f)

            // Outside shading bands
            drawRect(maskColor, Offset.Zero, Size(size.width, t))
            drawRect(maskColor, Offset(0f, b), Size(size.width, size.height - b))
            drawRect(maskColor, Offset(0f, t), Size(l, b - t))
            drawRect(maskColor, Offset(r, t), Size(size.width - r, b - t))

            // Crop frame outline
            drawRect(
                color = ElectricCyan,
                topLeft = Offset(l, t),
                size = Size((r - l).coerceAtLeast(1f), (b - t).coerceAtLeast(1f)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            // Rule of thirds lines
            val w = r - l
            val h = b - t
            val gridColor = Color.White.copy(alpha = 0.45f)
            drawLine(gridColor, Offset(l + w / 3f, t), Offset(l + w / 3f, b), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(l + 2f * w / 3f, t), Offset(l + 2f * w / 3f, b), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(l, t + h / 3f), Offset(r, t + h / 3f), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(l, t + 2f * h / 3f), Offset(r, t + 2f * h / 3f), strokeWidth = 1.5f)
        }

        // Top-Left corner drag handle
        CropCornerHandle(
            xPx = clip.cropLeft * safeW,
            yPx = clip.cropTop * safeH,
            testTag = "crop_handle_tl"
        ) { dx, dy ->
            val cur = latestClip
            val nextL = (cur.cropLeft + dx / safeW).coerceIn(0f, cur.cropRight - 0.22f)
            val nextT = (cur.cropTop + dy / safeH).coerceIn(0f, cur.cropBottom - 0.22f)
            onUpdateCrop(cur.id, nextL, nextT, cur.cropRight, cur.cropBottom)
        }

        // Top-Right corner drag handle
        CropCornerHandle(
            xPx = clip.cropRight * safeW,
            yPx = clip.cropTop * safeH,
            testTag = "crop_handle_tr"
        ) { dx, dy ->
            val cur = latestClip
            val nextR = (cur.cropRight + dx / safeW).coerceIn(cur.cropLeft + 0.22f, 1f)
            val nextT = (cur.cropTop + dy / safeH).coerceIn(0f, cur.cropBottom - 0.22f)
            onUpdateCrop(cur.id, cur.cropLeft, nextT, nextR, cur.cropBottom)
        }

        // Bottom-Left corner drag handle
        CropCornerHandle(
            xPx = clip.cropLeft * safeW,
            yPx = clip.cropBottom * safeH,
            testTag = "crop_handle_bl"
        ) { dx, dy ->
            val cur = latestClip
            val nextL = (cur.cropLeft + dx / safeW).coerceIn(0f, cur.cropRight - 0.22f)
            val nextB = (cur.cropBottom + dy / safeH).coerceIn(cur.cropTop + 0.22f, 1f)
            onUpdateCrop(cur.id, nextL, cur.cropTop, cur.cropRight, nextB)
        }

        // Bottom-Right corner drag handle
        CropCornerHandle(
            xPx = clip.cropRight * safeW,
            yPx = clip.cropBottom * safeH,
            testTag = "crop_handle_br"
        ) { dx, dy ->
            val cur = latestClip
            val nextR = (cur.cropRight + dx / safeW).coerceIn(cur.cropLeft + 0.22f, 1f)
            val nextB = (cur.cropBottom + dy / safeH).coerceIn(cur.cropTop + 0.22f, 1f)
            onUpdateCrop(cur.id, cur.cropLeft, cur.cropTop, nextR, nextB)
        }
    }
}

@Composable
private fun CropCornerHandle(
    xPx: Float,
    yPx: Float,
    testTag: String,
    onDelta: (Float, Float) -> Unit
) {
    val latestDelta by rememberUpdatedState(onDelta)
    Box(
        modifier = Modifier
            .offset { IntOffset((xPx - 28f).roundToInt(), (yPx - 28f).roundToInt()) }
            .size(34.dp)
            .testTag(testTag)
            .pointerInput(testTag) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    latestDelta(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(ElectricCyan)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun InteractiveAiRemoverLayer(
    zones: List<AiRemoverZone>,
    selectedZoneId: String?,
    showHandles: Boolean,
    stageWidthPx: Float,
    stageHeightPx: Float,
    onSelectZone: (String) -> Unit,
    onUpdateZone: (AiRemoverZone) -> Unit
) {
    val safeW = stageWidthPx.coerceAtLeast(100f)
    val safeH = stageHeightPx.coerceAtLeast(100f)

    Box(modifier = Modifier.fillMaxSize()) {
        zones.forEach { zone ->
            val latestZone by rememberUpdatedState(zone)
            val isSelected = showHandles && zone.id == selectedZoneId
            val boxW = (zone.normWidth * safeW).roundToInt().coerceAtLeast(48)
            val boxH = (zone.normHeight * safeH).roundToInt().coerceAtLeast(36)
            val centerX = (zone.normX * safeW).roundToInt()
            val centerY = (zone.normY * safeH).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(centerX - boxW / 2, centerY - boxH / 2) }
                    .size(
                        width = with(androidx.compose.ui.platform.LocalDensity.current) { boxW.toDp() },
                        height = with(androidx.compose.ui.platform.LocalDensity.current) { boxH.toDp() }
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected || showHandles) {
                            Modifier.border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonRose else ElectricCyan.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else Modifier
                    )
                    .clickable(enabled = showHandles) { onSelectZone(zone.id) }
                    .pointerInput(zone.id, showHandles, safeW, safeH) {
                        if (!showHandles) return@pointerInput
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val cur = latestZone
                            onUpdateZone(
                                cur.copy(
                                    normX = (cur.normX + dragAmount.x / safeW).coerceIn(0.08f, 0.92f),
                                    normY = (cur.normY + dragAmount.y / safeH).coerceIn(0.08f, 0.92f)
                                )
                            )
                        }
                    }
            ) {
                when (zone.mode) {
                    AiRemoverMode.SMART_PATCH -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(16.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xD8202632),
                                            Color(0xAA161A22),
                                            Color(0x5512151C)
                                        )
                                    )
                                )
                        )
                    }
                    AiRemoverMode.SOFT_BLUR -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp)
                                .background(Color.White.copy(alpha = 0.24f))
                        )
                    }
                    AiRemoverMode.MOSAIC -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cell = 16f
                            var y = 0f
                            var row = 0
                            while (y < size.height) {
                                var x = 0f
                                var col = 0
                                while (x < size.width) {
                                    val shade = if ((row + col) % 2 == 0) Color(0xCC2A3140) else Color(0xCC4B5568)
                                    drawRect(
                                        color = shade,
                                        topLeft = Offset(x, y),
                                        size = Size(cell, cell)
                                    )
                                    x += cell
                                    col++
                                }
                                y += cell
                                row++
                            }
                        }
                    }
                }

                // Bottom-right resize handle when editing eraser boxes
                if (showHandles) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(NeonRose.copy(alpha = 0.9f))
                            .pointerInput("resize_${zone.id}", safeW, safeH) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val cur = latestZone
                                    onUpdateZone(
                                        cur.copy(
                                            normWidth = (cur.normWidth + dragAmount.x / safeW).coerceIn(0.08f, 0.85f),
                                            normHeight = (cur.normHeight + dragAmount.y / safeH).coerceIn(0.06f, 0.85f)
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenWith,
                            contentDescription = "Resize eraser box",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCoverBottomSheet(
    project: ProjectData,
    onCaptureAtPlayhead: (String) -> Unit,
    onPickCoverImage: () -> Unit,
    onSaveCoverConfig: (VideoCoverConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by remember(project.coverConfig.coverTitle) {
        mutableStateOf(project.coverConfig.coverTitle)
    }
    var badgeText by remember(project.coverConfig.coverBadge) {
        mutableStateOf(project.coverConfig.coverBadge)
    }
    var accentHex by remember(project.coverConfig.accentHex) {
        mutableStateOf(project.coverConfig.accentHex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Video Cover", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "Shown at timeline start & project card",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }

            // Cover Preview Card
            val previewUri = project.coverConfig.coverImageUri ?: project.clips.firstOrNull()?.uriString
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(150.dp)
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioSurfaceElevated)
                    .border(2.dp, Color(accentHex), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = Uri.parse(previewUri),
                        contentDescription = "Cover preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("No Cover", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )

                if (badgeText.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = Color(accentHex),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF041418),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (titleText.isNotBlank()) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onCaptureAtPlayhead(titleText) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cover_capture_frame_btn"),
                    border = BorderStroke(1.dp, ElectricCyan)
                ) {
                    Icon(Icons.Filled.CameraAlt, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use Frame", color = ElectricCyan, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onPickCoverImage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cover_upload_photo_btn"),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = TextPrimary, maxLines = 1)
                }
            }

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Cover Headline (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(
                    listOf(
                        0xFF00E5FFL,
                        0xFFFF3B6BL,
                        0xFFF59E0BL,
                        0xFF8B5CF6L,
                        0xFF10B981L,
                        0xFFFFFFFFL
                    )
                ) { colorHex ->
                    val selected = accentHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                            .clickable { accentHex = colorHex }
                    )
                }
            }

            Button(
                onClick = {
                    onSaveCoverConfig(
                        project.coverConfig.copy(
                            coverTitle = titleText.trim(),
                            coverBadge = badgeText.trim(),
                            accentHex = accentHex
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cover_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418))
            ) {
                Text("Save Video Cover", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SafeZoneOverlay(mode: SafeZoneMode) {
    if (mode == SafeZoneMode.NONE) return
    val topFraction: Float
    val bottomFraction: Float
    val rightMarginFraction: Float
    val accentColor: Color
    when (mode) {
        SafeZoneMode.TIKTOK -> {
            topFraction = 0.11f
            bottomFraction = 0.24f
            rightMarginFraction = 0.16f
            accentColor = ElectricCyan
        }
        SafeZoneMode.REELS -> {
            topFraction = 0.10f
            bottomFraction = 0.22f
            rightMarginFraction = 0.14f
            accentColor = NeonRose
        }
        SafeZoneMode.SHORTS -> {
            topFraction = 0.09f
            bottomFraction = 0.20f
            rightMarginFraction = 0.15f
            accentColor = StudioAmber
        }
        SafeZoneMode.NONE -> return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val topY = size.height * topFraction
            val bottomY = size.height * (1f - bottomFraction)
            val rightX = size.width * (1f - rightMarginFraction)
            val leftX = size.width * 0.05f
            val uiDanger = Color.Black.copy(alpha = 0.28f)

            drawRect(uiDanger, Offset.Zero, Size(size.width, topY))
            drawRect(uiDanger, Offset(0f, bottomY), Size(size.width, size.height - bottomY))
            drawRect(uiDanger, Offset(rightX, topY), Size(size.width - rightX, bottomY - topY))

            drawRect(
                color = accentColor.copy(alpha = 0.85f),
                topLeft = Offset(leftX, topY),
                size = Size((rightX - leftX).coerceAtLeast(10f), (bottomY - topY).coerceAtLeast(10f)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.5f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
        ) {
            Text(
                text = "${mode.label} Safe Area",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StageTextOverlayView(
    item: TextOverlayItem,
    shownText: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fontOption = remember(item.fontFamilyKey) { StudioFontCatalog.optionForKey(item.fontFamilyKey) }
    val androidTypeface = remember(fontOption.fontResId) {
        runCatching { ResourcesCompat.getFont(context, fontOption.fontResId) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    val textColor = Color(item.colorHex)
    val shadowEffect = when {
        item.isNeonGlow -> Shadow(
            color = if (item.colorHex == 0xFFFFFFFFL) ElectricCyan else textColor,
            offset = Offset(0f, 0f),
            blurRadius = 18f
        )
        item.is3D -> Shadow(
            color = Color(0xCC06080D),
            offset = Offset(5f, 5f),
            blurRadius = 2f
        )
        else -> null
    }

    if (item.isCurved) {
        val boxW = (170f * item.scale.coerceIn(0.6f, 2.2f)).dp
        val boxH = (62f * item.scale.coerceIn(0.6f, 2.2f)).dp
        Canvas(modifier = modifier.size(boxW, boxH)) {
            val arcPath = android.graphics.Path().apply {
                val padX = size.width * 0.06f
                addArc(
                    padX,
                    size.height * 0.22f,
                    size.width - padX,
                    size.height * 1.45f,
                    205f,
                    130f
                )
            }
            val nativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor.toArgb()
                textSize = (16f * item.scale).sp.toPx()
                typeface = androidTypeface
                textAlign = Paint.Align.CENTER
                if (item.isNeonGlow) {
                    setShadowLayer(14f, 0f, 0f, (if (item.colorHex == 0xFFFFFFFFL) ElectricCyan else textColor).toArgb())
                } else if (item.is3D) {
                    setShadowLayer(2f, 5f, 5f, android.graphics.Color.argb(215, 6, 8, 14))
                }
            }
            drawContext.canvas.nativeCanvas.drawTextOnPath(shownText, arcPath, 0f, 0f, nativePaint)
        }
    } else {
        val layoutDir = if (item.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            Box(modifier = modifier) {
                if (item.is3D) {
                    Text(
                        text = shownText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = fontOption.fontFamily,
                            fontSize = (18f * item.scale).sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = NeonRose.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                    )
                }
                Text(
                    text = shownText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = fontOption.fontFamily,
                        fontSize = (18f * item.scale).sp,
                        fontWeight = FontWeight.Bold,
                        shadow = shadowEffect
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

