package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MainNavTab
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SsLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(0xFF0C1117))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(ElectricCyan, NeonRose)),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.66f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)

            // Upper cyan S ribbon
            val topPath = Path().apply {
                moveTo(w * 0.78f, h * 0.16f)
                lineTo(w * 0.34f, h * 0.16f)
                lineTo(w * 0.16f, h * 0.36f)
                lineTo(w * 0.54f, h * 0.56f)
            }
            drawPath(
                path = topPath,
                brush = Brush.linearGradient(listOf(ElectricCyan, Color(0xFF38BDF8))),
                style = stroke
            )

            // Lower neon rose S ribbon
            val bottomPath = Path().apply {
                moveTo(w * 0.46f, h * 0.44f)
                lineTo(w * 0.84f, h * 0.64f)
                lineTo(w * 0.66f, h * 0.84f)
                lineTo(w * 0.22f, h * 0.84f)
            }
            drawPath(
                path = bottomPath,
                brush = Brush.linearGradient(listOf(NeonRose, Color(0xFFF43F5E))),
                style = stroke
            )
        }
    }
}

@Composable
fun SsWordmarkBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SsLogoMark(size = 34.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "s.s",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = ElectricCyan.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
        ) {
            Text(
                text = "STUDIO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = ElectricCyan,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StudioBottomNavigation(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioSurface,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StudioBorder.copy(alpha = 0.65f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainNavTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val icon = when (tab) {
                        MainNavTab.EDIT -> if (selected) Icons.Filled.MovieCreation else Icons.Outlined.MovieCreation
                        MainNavTab.TEMPLATES -> if (selected) Icons.Filled.AutoAwesomeMosaic else Icons.Outlined.AutoAwesomeMosaic
                        MainNavTab.EXPORTS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                    }
                    Column(
                        modifier = Modifier
                            .testTag("nav_tab_${tab.name.lowercase()}")
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) ElectricCyan.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                tint = if (selected) ElectricCyan else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioWaveformStrip(
    waveform: List<Float>,
    barColor: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (waveform.isEmpty()) return@Canvas
        val barCount = waveform.size
        val step = size.width / barCount
        val barWidth = (step * 0.58f).coerceAtLeast(2f)
        val centerY = size.height / 2f
        waveform.forEachIndexed { index, amp ->
            val h = (amp.coerceIn(0.12f, 1f) * size.height * 0.82f)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * step, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

fun formatTimecode(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = (safe % 1000L) / 100L
    return String.format("%02d:%02d.%d", mins, secs, tenths)
}

fun formatShortDuration(ms: Long): String {
    val safe = (ms.coerceAtLeast(0L) + 500L) / 1000L
    val mins = safe / 60L
    val secs = safe % 60L
    return String.format("%02d:%02d", mins, secs)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%d KB", bytes / 1024L)
}
