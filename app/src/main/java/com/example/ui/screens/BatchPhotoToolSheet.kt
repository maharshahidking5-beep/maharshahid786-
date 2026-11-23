package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.media.BatchPhotoResult
import com.example.media.SmartMediaHelpers
import com.example.model.AspectRatioOption
import com.example.model.FilterPreset
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPhotoToolSheet(
    defaultWatermark: Boolean,
    onImportPhotosAsReel: (List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var chosenFilter by remember { mutableStateOf(FilterPreset.A1_STUDIO) }
    var chosenRatio by remember { mutableStateOf(AspectRatioOption.RATIO_4_5) }
    var includeWatermark by remember(defaultWatermark) { mutableStateOf(defaultWatermark) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var batchResult by remember { mutableStateOf<BatchPhotoResult?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            batchResult = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Batch Photo Filter & Resize",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Filter, resize & watermark multiple photos to Gallery at once",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }

            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_photo_pick_btn"),
                border = BorderStroke(1.dp, ElectricCyan)
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedUris.isEmpty()) "Select Photos (Up to 15)" else "${selectedUris.size} Photo(s) Selected · Change",
                    color = ElectricCyan
                )
            }

            if (selectedUris.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Text("Filter Style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FilterPreset.entries.take(10), key = { it.name }) { preset ->
                    val active = chosenFilter == preset
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenFilter = preset },
                        color = if (active) ElectricCyan else StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (active) ElectricCyan else StudioBorder)
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) Color(0xFF041418) else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Text("Social Output Size", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AspectRatioOption.entries, key = { it.name }) { ratio ->
                    val active = chosenRatio == ratio
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenRatio = ratio },
                        color = if (active) NeonRose else StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (active) NeonRose else StudioBorder)
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add s.s Studio Watermark", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("Stamp creator badge in bottom-right corner", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(
                    checked = includeWatermark,
                    onCheckedChange = { includeWatermark = it },
                    modifier = Modifier.testTag("batch_watermark_switch")
                )
            }

            if (isProcessing) {
                LinearProgressIndicator(
                    progress = {
                        if (progressTotal > 0) progressDone / progressTotal.toFloat() else 0.1f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ElectricCyan,
                    trackColor = StudioSurfaceElevated
                )
                Text(
                    text = "Processing photo $progressDone of $progressTotal...",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }

            batchResult?.let { res ->
                Surface(
                    color = StudioEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = StudioEmerald)
                        Text(
                            text = "Saved ${res.processedCount} photo(s) to Gallery (${res.outputFolderLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedUris.isEmpty() || isProcessing) return@Button
                        isProcessing = true
                        scope.launch {
                            val res = SmartMediaHelpers.processBatchPhotos(
                                context = context,
                                uris = selectedUris,
                                filterPreset = chosenFilter,
                                targetRatio = chosenRatio,
                                addWatermark = includeWatermark
                            ) { done, total ->
                                progressDone = done
                                progressTotal = total
                            }
                            batchResult = res
                            isProcessing = false
                        }
                    },
                    enabled = selectedUris.isNotEmpty() && !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("batch_photo_export_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF041418)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save All", fontWeight = FontWeight.Bold)
                }

                if (selectedUris.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onImportPhotosAsReel(selectedUris)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("batch_photo_to_reel_btn"),
                        border = BorderStroke(1.dp, NeonRose)
                    ) {
                        Icon(Icons.Filled.MovieCreation, null, tint = NeonRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Make Reel", color = TextPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
