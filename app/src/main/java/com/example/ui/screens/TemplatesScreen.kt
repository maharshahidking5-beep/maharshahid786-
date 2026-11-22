package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.media.StudioCatalog
import com.example.model.TemplateItem
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onUseTemplateWithPicker: (TemplateItem) -> Unit,
    onPreviewTemplateWithSample: (TemplateItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("For You") }
    var activeTemplateSheet by remember { mutableStateOf<TemplateItem?>(null) }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == "For You") StudioCatalog.templates
        else StudioCatalog.templates.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "s.s Templates",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "One-tap beat sync, filters & animated titles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Surface(
                color = NeonRose.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = NeonRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Trending",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonRose
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StudioCatalog.templateCategories) { category ->
                val selected = category == selectedCategory
                Surface(
                    modifier = Modifier
                        .testTag("template_cat_${category.lowercase()}")
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = category },
                    color = if (selected) ElectricCyan else StudioSurfaceElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color(0xFF041418) else TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    onClick = { activeTemplateSheet = template }
                )
            }
        }
    }

    activeTemplateSheet?.let { template ->
        ModalBottomSheet(
            onDismissRequest = { activeTemplateSheet = null },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = StudioSurfaceElevated, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "${template.clipCount} clips · ${template.durationLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(color = StudioSurfaceElevated, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.FilterVintage, null, tint = NeonRose, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = template.filterPreset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    Surface(color = StudioSurfaceElevated, shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = template.aspectRatio.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val t = template
                        activeTemplateSheet = null
                        onUseTemplateWithPicker(t)
                    },
                    modifier = Modifier
                        .testTag("template_pick_media_button")
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041418)
                    )
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Your Clips & Photos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val t = template
                        activeTemplateSheet = null
                        onPreviewTemplateWithSample(t)
                    },
                    modifier = Modifier
                        .testTag("template_sample_button")
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Template with Sample Clips", color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_card_${template.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
            ) {
                Image(
                    painter = painterResource(id = StudioCatalog.drawableResForName(template.previewDrawableName)),
                    contentDescription = template.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${template.aspectRatio.label} · ${template.durationLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${template.clipCount} clips · ${template.usesCountLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ElectricCyan,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Use template",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF041418),
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
