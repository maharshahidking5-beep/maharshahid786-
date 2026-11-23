package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
import coil.compose.AsyncImage
import com.example.model.CanvaAspectOption
import com.example.model.CanvaBackgroundStyle
import com.example.model.CanvaBrandKit
import com.example.model.CanvaDesignDoc
import com.example.model.CanvaElementItem
import com.example.model.CanvaElementType
import com.example.model.CanvaProCatalog
import com.example.model.CanvaProject
import com.example.model.CanvaShapeKind
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

private val canvaColorSwatches = listOf(
    0xFFFFFFFF, 0xFF041418, 0xFF00E5FF, 0xFFFF3B6B,
    0xFF8B5CF6, 0xFFF59E0B, 0xFF10B981, 0xFF3B82F6,
    0xFFF43F5E, 0xFFFDE047, 0xFF0F172A, 0xFFEC4899
)

@Composable
fun CanvaStudioScreen(
    savedDesigns: List<CanvaDesignDoc>,
    decodeDesign: (String) -> CanvaProject?,
    onSaveDesignToCloud: (CanvaProject) -> Unit,
    onDeleteCloudDesign: (String) -> Unit,
    onUseAsCapCutCover: (coverUriString: String, title: String) -> Unit,
    onPublishCanvaToFeed: (title: String, headline: String, accentHex: Long, pngUri: String) -> Unit,
    onShowMessage: (String) -> Unit,
    externalOpenProject: CanvaProject? = null,
    onConsumedExternalProject: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeDesign by remember(externalOpenProject?.id) {
        mutableStateOf(externalOpenProject)
    }
    if (externalOpenProject != null && activeDesign?.id == externalOpenProject.id) {
        onConsumedExternalProject()
    }

    if (activeDesign != null) {
        CanvaCanvasEditor(
            initialProject = activeDesign!!,
            onBack = { activeDesign = null },
            onSaveCloud = { updated ->
                activeDesign = updated
                onSaveDesignToCloud(updated)
            },
            onExportPng = { project ->
                scope.launch {
                    val savedUri = exportCanvaBitmapToGallery(context, project)
                    if (savedUri != null) {
                        onShowMessage("Exported high-res PNG to Pictures/s.s_Canva")
                    } else {
                        onShowMessage("Failed exporting PNG")
                    }
                }
            },
            onUseAsCapCutCover = { project ->
                scope.launch {
                    val coverUri = saveCanvaDesignCachePng(context, project)
                    if (coverUri != null) {
                        onSaveDesignToCloud(project)
                        onUseAsCapCutCover(coverUri.toString(), project.title)
                    } else {
                        onShowMessage("Could not render cover image")
                    }
                }
            },
            onPublishToFeed = { project ->
                scope.launch {
                    val previewUri = saveCanvaDesignCachePng(context, project)?.toString().orEmpty()
                    val headline = project.elements.firstOrNull { it.type == CanvaElementType.TEXT }?.text.orEmpty()
                    val accent = project.elements.firstOrNull { it.colorHex != 0x00000000L }?.colorHex ?: 0xFF8B5CF6L
                    onSaveDesignToCloud(project)
                    onPublishCanvaToFeed(project.title, headline, accent, previewUri)
                }
            },
            modifier = modifier
        )
    } else {
        CanvaHubGallery(
            savedDesigns = savedDesigns,
            decodeDesign = decodeDesign,
            onOpenDesign = { activeDesign = it },
            onDeleteDesign = onDeleteCloudDesign,
            modifier = modifier
        )
    }
}

@Composable
private fun CanvaHubGallery(
    savedDesigns: List<CanvaDesignDoc>,
    decodeDesign: (String) -> CanvaProject?,
    onOpenDesign: (CanvaProject) -> Unit,
    onDeleteDesign: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Thumbnail", "Story", "Poster", "Logo", "Post")
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") CanvaProCatalog.templates
        else CanvaProCatalog.templates.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Canva Pro",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Surface(
                        color = VioletPulse.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, VioletPulse.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "STUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC4B5FD),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Posters, Thumbnails, Logos & Story Graphics synced online",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = {
                    onOpenDesign(
                        CanvaProject(
                            id = UUID.randomUUID().toString(),
                            title = "New Design",
                            category = "Poster",
                            elements = listOf(
                                CanvaElementItem(
                                    type = CanvaElementType.BADGE,
                                    text = "S.S CANVA PRO",
                                    normX = 0.5f,
                                    normY = 0.24f,
                                    colorHex = 0xFF00E5FF,
                                    textColorHex = 0xFF041418,
                                    fontSizeSp = 13f
                                ),
                                CanvaElementItem(
                                    type = CanvaElementType.TEXT,
                                    text = "TAP TO EDIT",
                                    normX = 0.5f,
                                    normY = 0.5f,
                                    colorHex = 0x00000000,
                                    textColorHex = 0xFFFFFFFF,
                                    fontSizeSp = 30f
                                )
                            )
                        )
                    )
                },
                modifier = Modifier.testTag("canva_blank_design_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletPulse, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Blank", fontWeight = FontWeight.Bold)
            }
        }

        // Brand Kits Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(CanvaProCatalog.brandKits, key = { it.id }) { kit ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onOpenDesign(
                                CanvaProject(
                                    id = UUID.randomUUID().toString(),
                                    title = "${kit.name} Poster",
                                    category = "Poster",
                                    bgStyle = CanvaBackgroundStyle.GRADIENT_DIAGONAL,
                                    bgColorHex = kit.bgHex,
                                    gradientEndHex = kit.gradientEndHex,
                                    elements = listOf(
                                        CanvaElementItem(
                                            type = CanvaElementType.BADGE,
                                            text = kit.name.uppercase(),
                                            normX = 0.5f,
                                            normY = 0.25f,
                                            colorHex = kit.primaryAccentHex,
                                            textColorHex = kit.bgHex,
                                            fontSizeSp = 12f
                                        ),
                                        CanvaElementItem(
                                            type = CanvaElementType.TEXT,
                                            text = "BRAND STUDIO\nCOLLECTION",
                                            normX = 0.5f,
                                            normY = 0.5f,
                                            colorHex = 0x00000000,
                                            textColorHex = kit.headlineColorHex,
                                            fontSizeSp = 28f
                                        )
                                    )
                                )
                            )
                        },
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(kit.primaryAccentHex), Color(kit.secondaryAccentHex))
                                    )
                                )
                        )
                        Text(kit.name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                }
            }
        }

        // Cloud Saved Designs section if any
        if (savedDesigns.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = "Your Cloud Designs (${savedDesigns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedDesigns, key = { it.id }) { doc ->
                    Surface(
                        modifier = Modifier
                            .width(156.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val parsed = decodeDesign(doc.designJson) ?: CanvaProject(
                                    id = doc.id,
                                    title = doc.title,
                                    category = doc.category,
                                    bgColorHex = doc.bgHex
                                )
                                onOpenDesign(parsed)
                            },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(doc.accentHex).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(doc.bgHex), Color(doc.accentHex).copy(alpha = 0.35f))
                                        )
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = doc.headline.ifBlank { doc.title },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                IconButton(
                                    onClick = { onDeleteDesign(doc.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, "Delete design", tint = NeonRose, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = doc.title.ifBlank { "Untitled Design" },
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.category} · ${doc.aspectLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val selected = cat == selectedCategory
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedCategory = cat },
                    color = if (selected) VioletPulse else StudioSurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (selected) VioletPulse else StudioBorder)
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Templates Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTemplates, key = { it.id }) { preset ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("canva_template_${preset.id}")
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            val freshCopy = preset.project.copy(
                                id = UUID.randomUUID().toString(),
                                elements = preset.project.elements.map { it.copy(id = UUID.randomUUID().toString()) }
                            )
                            onOpenDesign(freshCopy)
                        },
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.7f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.86f)
                                .clipToBounds()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(preset.project.bgColorHex), Color(preset.project.gradientEndHex))
                                    )
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val badge = preset.project.elements.firstOrNull { it.type == CanvaElementType.BADGE }
                            val headline = preset.project.elements.firstOrNull { it.type == CanvaElementType.TEXT }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (badge != null) {
                                    Surface(color = Color(badge.colorHex), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = badge.text,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(badge.textColorHex),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (headline != null) {
                                    Text(
                                        text = headline.text,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(headline.textColorHex),
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = preset.project.aspectOption.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${preset.category} · ${preset.project.elements.size} layers",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvaCanvasEditor(
    initialProject: CanvaProject,
    onBack: () -> Unit,
    onSaveCloud: (CanvaProject) -> Unit,
    onExportPng: (CanvaProject) -> Unit,
    onUseAsCapCutCover: (CanvaProject) -> Unit,
    onPublishToFeed: (CanvaProject) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var project by remember(initialProject.id) { mutableStateOf(initialProject) }
    var selectedElementId by remember(initialProject.id) {
        mutableStateOf(initialProject.elements.lastOrNull()?.id)
    }
    var editingTextElement by remember { mutableStateOf<CanvaElementItem?>(null) }
    var editingTitle by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val item = CanvaElementItem(
                type = CanvaElementType.IMAGE,
                text = "Photo",
                imageUri = uri.toString(),
                normX = 0.5f,
                normY = 0.45f,
                widthFrac = 0.48f,
                heightFrac = 0.36f,
                cornerRadiusDp = 18f
            )
            project = project.copy(elements = project.elements + item)
            selectedElementId = item.id
        }
    }

    val selectedElement = project.elements.find { it.id == selectedElementId }

    fun updateElement(updated: CanvaElementItem) {
        project = project.copy(
            elements = project.elements.map { if (it.id == updated.id) updated else it }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .statusBarsPadding()
    ) {
        // Editor Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Canva Hub", tint = TextPrimary)
                }
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { editingTitle = true }
                        .padding(horizontal = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSaveCloud(project) },
                    modifier = Modifier
                        .testTag("canva_save_cloud_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.CloudUpload, "Save to Cloud", tint = ElectricCyan)
                }
                IconButton(
                    onClick = { onExportPng(project) },
                    modifier = Modifier
                        .testTag("canva_export_png_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Download, "Export PNG", tint = StudioEmerald)
                }
                Button(
                    onClick = { onPublishToFeed(project) },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Interactive Canva Canvas Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.50f)
                .background(Color(0xFF06070B))
                .pointerInput(Unit) {
                    detectTapGestures { selectedElementId = null }
                }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(project.aspectOption.wRatio / project.aspectOption.hRatio)
                    .clip(RoundedCornerShape(12.dp))
                    .clipToBounds()
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            ) {
                val stageMaxW = maxWidth
                val stageMaxH = maxHeight
                val canvasW = with(LocalDensity.current) { stageMaxW.toPx() }
                val canvasH = with(LocalDensity.current) { stageMaxH.toPx() }

                // Background renderer
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startC = Color(project.bgColorHex)
                    val endC = Color(project.gradientEndHex)
                    when (project.bgStyle) {
                        CanvaBackgroundStyle.SOLID -> drawRect(color = startC)
                        CanvaBackgroundStyle.GRADIENT_DIAGONAL -> drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(startC, endC),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        CanvaBackgroundStyle.NEON_MESH -> {
                            drawRect(
                                brush = Brush.linearGradient(listOf(startC, endC))
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent),
                                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                                    radius = size.width * 0.65f
                                ),
                                radius = size.width * 0.65f,
                                center = Offset(size.width * 0.2f, size.height * 0.2f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonRose.copy(alpha = 0.22f), Color.Transparent),
                                    center = Offset(size.width * 0.82f, size.height * 0.78f),
                                    radius = size.width * 0.65f
                                ),
                                radius = size.width * 0.65f,
                                center = Offset(size.width * 0.82f, size.height * 0.78f)
                            )
                        }
                        CanvaBackgroundStyle.GRID_STUDIO -> {
                            drawRect(color = startC)
                            val step = size.width / 8f
                            var x = step
                            while (x < size.width) {
                                drawLine(Color.White.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                x += step
                            }
                            var y = step
                            while (y < size.height) {
                                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                                y += step
                            }
                        }
                    }
                }

                // Interactive Elements (Drag, Pinch, Rotate)
                project.elements.forEach { elem ->
                    val latestElem by rememberUpdatedState(elem)
                    val isSelected = elem.id == selectedElementId
                    val offsetX = ((elem.normX - 0.5f) * canvasW).roundToInt()
                    val offsetY = ((elem.normY - 0.5f) * canvasH).roundToInt()

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(offsetX, offsetY) }
                            .graphicsLayer {
                                scaleX = elem.scale
                                scaleY = elem.scale
                                rotationZ = elem.rotationDegrees
                            }
                            .alpha(elem.opacity.coerceIn(0.1f, 1f))
                            .pointerInput(elem.id, canvasW, canvasH) {
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val cur = latestElem
                                    selectedElementId = cur.id
                                    updateElement(
                                        cur.copy(
                                            normX = (cur.normX + pan.x / canvasW.coerceAtLeast(100f)).coerceIn(0.05f, 0.95f),
                                            normY = (cur.normY + pan.y / canvasH.coerceAtLeast(100f)).coerceIn(0.05f, 0.95f),
                                            scale = (cur.scale * zoom).coerceIn(0.35f, 3.2f),
                                            rotationDegrees = (cur.rotationDegrees + rotation) % 360f
                                        )
                                    )
                                }
                            }
                            .pointerInput(elem.id) {
                                detectTapGestures(
                                    onTap = { selectedElementId = elem.id },
                                    onDoubleTap = {
                                        selectedElementId = elem.id
                                        if (elem.type == CanvaElementType.TEXT || elem.type == CanvaElementType.BADGE) {
                                            editingTextElement = elem
                                        }
                                    }
                                )
                            }
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, ElectricCyan, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                    ) {
                        when (elem.type) {
                            CanvaElementType.TEXT -> {
                                val bgColor = Color(elem.colorHex)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = elem.text,
                                        fontSize = elem.fontSizeSp.sp,
                                        fontWeight = if (elem.bold) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = Color(elem.textColorHex),
                                        textAlign = TextAlign.Center,
                                        lineHeight = (elem.fontSizeSp * 1.15f).sp
                                    )
                                }
                            }
                            CanvaElementType.BADGE -> {
                                Surface(
                                    color = Color(elem.colorHex),
                                    shape = RoundedCornerShape(elem.cornerRadiusDp.dp)
                                ) {
                                    Text(
                                        text = elem.text,
                                        fontSize = elem.fontSizeSp.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(elem.textColorHex),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            CanvaElementType.SHAPE -> {
                                val boxW = stageMaxW * elem.widthFrac.coerceIn(0.12f, 0.95f)
                                val boxH = stageMaxH * elem.heightFrac.coerceIn(0.06f, 0.95f)
                                val shapeColor = Color(elem.colorHex)
                                Canvas(modifier = Modifier.size(boxW, boxH)) {
                                    when (elem.shapeKind) {
                                        CanvaShapeKind.CIRCLE -> drawOval(color = shapeColor, style = Fill)
                                        CanvaShapeKind.ROUNDED_RECT -> drawRoundRect(
                                            color = shapeColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(elem.cornerRadiusDp.dp.toPx())
                                        )
                                        CanvaShapeKind.PILL -> drawRoundRect(
                                            color = shapeColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                                        )
                                        CanvaShapeKind.OUTLINE_FRAME -> drawRoundRect(
                                            color = shapeColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(elem.cornerRadiusDp.dp.toPx()),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        CanvaShapeKind.DIAMOND -> {
                                            val path = Path().apply {
                                                moveTo(size.width / 2f, 0f)
                                                lineTo(size.width, size.height / 2f)
                                                lineTo(size.width / 2f, size.height)
                                                lineTo(0f, size.height / 2f)
                                                close()
                                            }
                                            drawPath(path, color = shapeColor)
                                        }
                                    }
                                }
                            }
                            CanvaElementType.IMAGE -> {
                                val boxW = stageMaxW * elem.widthFrac.coerceIn(0.15f, 0.92f)
                                val boxH = stageMaxH * elem.heightFrac.coerceIn(0.15f, 0.92f)
                                AsyncImage(
                                    model = Uri.parse(elem.imageUri),
                                    contentDescription = "Canvas photo layer",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(boxW, boxH)
                                        .clip(RoundedCornerShape(elem.cornerRadiusDp.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Canva Tools & Contextual Inspector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.50f),
            color = StudioSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Add Strip + Use as CapCut Cover
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickCanvaActionChip("+ Text", Icons.Filled.TextFields, ElectricCyan) {
                        val item = CanvaElementItem(
                            type = CanvaElementType.TEXT,
                            text = "HEADLINE",
                            normX = 0.5f,
                            normY = 0.5f,
                            colorHex = 0x00000000,
                            textColorHex = 0xFFFFFFFF,
                            fontSizeSp = 26f
                        )
                        project = project.copy(elements = project.elements + item)
                        selectedElementId = item.id
                    }
                    QuickCanvaActionChip("+ Badge", Icons.Filled.AutoAwesome, StudioAmber) {
                        val item = CanvaElementItem(
                            type = CanvaElementType.BADGE,
                            text = "PRO PICK",
                            normX = 0.5f,
                            normY = 0.35f,
                            colorHex = 0xFF00E5FF,
                            textColorHex = 0xFF041418,
                            fontSizeSp = 13f
                        )
                        project = project.copy(elements = project.elements + item)
                        selectedElementId = item.id
                    }
                    QuickCanvaActionChip("+ Shape", Icons.Filled.Category, VioletPulse) {
                        val item = CanvaElementItem(
                            type = CanvaElementType.SHAPE,
                            shapeKind = CanvaShapeKind.ROUNDED_RECT,
                            normX = 0.5f,
                            normY = 0.5f,
                            widthFrac = 0.55f,
                            heightFrac = 0.25f,
                            colorHex = 0xFF8B5CF6,
                            opacity = 0.65f
                        )
                        project = project.copy(elements = project.elements + item)
                        selectedElementId = item.id
                    }
                    QuickCanvaActionChip("+ Photo", Icons.Filled.AddPhotoAlternate, StudioEmerald) {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    QuickCanvaActionChip("CapCut Cover", Icons.Filled.MovieCreation, NeonRose) {
                        onUseAsCapCutCover(project)
                    }
                }

                if (selectedElement != null) {
                    // Layer / Element Inspector
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected ${selectedElement.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ElectricCyan
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (selectedElement.type == CanvaElementType.TEXT || selectedElement.type == CanvaElementType.BADGE) {
                                        IconButton(
                                            onClick = { editingTextElement = selectedElement },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, "Edit text", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val reordered = project.elements.filterNot { it.id == selectedElement.id } + selectedElement
                                            project = project.copy(elements = reordered)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.FlipToFront, "Bring to front", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val copy = selectedElement.copy(
                                                id = UUID.randomUUID().toString(),
                                                normY = (selectedElement.normY + 0.08f).coerceAtMost(0.9f)
                                            )
                                            project = project.copy(elements = project.elements + copy)
                                            selectedElementId = copy.id
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Duplicate layer", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            project = project.copy(elements = project.elements.filterNot { it.id == selectedElement.id })
                                            selectedElementId = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, "Delete layer", tint = NeonRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (selectedElement.type == CanvaElementType.SHAPE) {
                                Text("Shape Style", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CanvaShapeKind.entries.forEach { kind ->
                                        val active = selectedElement.shapeKind == kind
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { updateElement(selectedElement.copy(shapeKind = kind)) },
                                            color = if (active) VioletPulse else StudioSurfaceHighlight,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = kind.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (selectedElement.type == CanvaElementType.TEXT) "Text Color" else "Fill Color",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                canvaColorSwatches.forEach { hex ->
                                    val isCurrent = if (selectedElement.type == CanvaElementType.TEXT) {
                                        selectedElement.textColorHex == hex
                                    } else {
                                        selectedElement.colorHex == hex
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (isCurrent) 2.5.dp else 1.dp,
                                                color = if (isCurrent) ElectricCyan else StudioBorder,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (selectedElement.type == CanvaElementType.TEXT) {
                                                    updateElement(selectedElement.copy(textColorHex = hex))
                                                } else {
                                                    updateElement(selectedElement.copy(colorHex = hex))
                                                }
                                            }
                                    )
                                }
                            }

                            Text(
                                text = "Scale ${String.format("%.1fx", selectedElement.scale)} · Opacity ${(selectedElement.opacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Slider(
                                value = selectedElement.scale,
                                onValueChange = { updateElement(selectedElement.copy(scale = it)) },
                                valueRange = 0.4f..2.8f,
                                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                            )
                            Slider(
                                value = selectedElement.opacity,
                                onValueChange = { updateElement(selectedElement.copy(opacity = it)) },
                                valueRange = 0.15f..1f,
                                colors = SliderDefaults.colors(thumbColor = VioletPulse, activeTrackColor = VioletPulse)
                            )
                        }
                    }
                } else {
                    // Canvas Aspect, Background Style & Brand Kit Inspector
                    Text("Canvas Format", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvaAspectOption.entries.forEach { opt ->
                            val selected = project.aspectOption == opt
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { project = project.copy(aspectOption = opt) },
                                color = if (selected) ElectricCyan.copy(alpha = 0.18f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else StudioBorder)
                            ) {
                                Text(
                                    text = opt.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) ElectricCyan else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Text("Background Style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvaBackgroundStyle.entries.forEach { style ->
                            val selected = project.bgStyle == style
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { project = project.copy(bgStyle = style) },
                                color = if (selected) VioletPulse.copy(alpha = 0.22f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) VioletPulse else StudioBorder)
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color(0xFFC4B5FD) else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Text("Apply Brand Kit Palette", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvaProCatalog.brandKits.forEach { kit ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        project = applyBrandKitToProject(project, kit)
                                    },
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(kit.primaryAccentHex))
                                    )
                                    Text(kit.name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingTextElement?.let { target ->
        var textVal by remember(target.id) { mutableStateOf(target.text) }
        AlertDialog(
            onDismissRequest = { editingTextElement = null },
            containerColor = StudioSurfaceElevated,
            title = { Text("Edit Text Layer", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = textVal,
                    onValueChange = { textVal = it },
                    label = { Text("Text content") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textVal.isNotBlank()) {
                        updateElement(target.copy(text = textVal))
                    }
                    editingTextElement = null
                }) {
                    Text("Apply", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTextElement = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (editingTitle) {
        var titleVal by remember(project.title) { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { editingTitle = false },
            containerColor = StudioSurfaceElevated,
            title = { Text("Design Name", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = titleVal,
                    onValueChange = { titleVal = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (titleVal.isNotBlank()) {
                        project = project.copy(title = titleVal.trim())
                    }
                    editingTitle = false
                }) {
                    Text("Save", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTitle = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun QuickCanvaActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}

private fun applyBrandKitToProject(project: CanvaProject, kit: CanvaBrandKit): CanvaProject {
    return project.copy(
        bgColorHex = kit.bgHex,
        gradientEndHex = kit.gradientEndHex,
        elements = project.elements.map { el ->
            when (el.type) {
                CanvaElementType.TEXT -> el.copy(textColorHex = kit.headlineColorHex)
                CanvaElementType.BADGE -> el.copy(colorHex = kit.primaryAccentHex, textColorHex = kit.bgHex)
                CanvaElementType.SHAPE -> el.copy(colorHex = kit.secondaryAccentHex)
                CanvaElementType.IMAGE -> el
            }
        }
    )
}

private fun renderCanvaBitmap(context: Context, project: CanvaProject): Bitmap {
    val baseW = 1080
    val baseH = ((baseW * (project.aspectOption.hRatio / project.aspectOption.wRatio)).roundToInt())
        .coerceIn(600, 1920)
    val bitmap = Bitmap.createBitmap(baseW, baseH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    when (project.bgStyle) {
        CanvaBackgroundStyle.SOLID, CanvaBackgroundStyle.GRID_STUDIO -> {
            bgPaint.color = project.bgColorHex.toInt()
            canvas.drawRect(0f, 0f, baseW.toFloat(), baseH.toFloat(), bgPaint)
            if (project.bgStyle == CanvaBackgroundStyle.GRID_STUDIO) {
                val gridPaint = Paint().apply {
                    color = 0x18FFFFFF
                    strokeWidth = 2f
                }
                val step = baseW / 8f
                var x = step
                while (x < baseW) {
                    canvas.drawLine(x, 0f, x, baseH.toFloat(), gridPaint)
                    x += step
                }
                var y = step
                while (y < baseH) {
                    canvas.drawLine(0f, y, baseW.toFloat(), y, gridPaint)
                    y += step
                }
            }
        }
        CanvaBackgroundStyle.GRADIENT_DIAGONAL, CanvaBackgroundStyle.NEON_MESH -> {
            bgPaint.shader = LinearGradient(
                0f, 0f, baseW.toFloat(), baseH.toFloat(),
                project.bgColorHex.toInt(), project.gradientEndHex.toInt(),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, baseW.toFloat(), baseH.toFloat(), bgPaint)
        }
    }

    // Elements
    project.elements.forEach { elem ->
        val cx = elem.normX * baseW
        val cy = elem.normY * baseH
        val alphaInt = (elem.opacity.coerceIn(0.1f, 1f) * 255).roundToInt()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(elem.rotationDegrees)
        canvas.scale(elem.scale, elem.scale)

        when (elem.type) {
            CanvaElementType.SHAPE -> {
                val w = elem.widthFrac * baseW
                val h = elem.heightFrac * baseH
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = elem.colorHex.toInt()
                    alpha = alphaInt
                }
                val rect = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
                when (elem.shapeKind) {
                    CanvaShapeKind.CIRCLE -> canvas.drawOval(rect, paint)
                    CanvaShapeKind.ROUNDED_RECT -> canvas.drawRoundRect(rect, elem.cornerRadiusDp * 2.5f, elem.cornerRadiusDp * 2.5f, paint)
                    CanvaShapeKind.PILL -> canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
                    CanvaShapeKind.OUTLINE_FRAME -> {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 8f
                        canvas.drawRoundRect(rect, elem.cornerRadiusDp * 2.5f, elem.cornerRadiusDp * 2.5f, paint)
                    }
                    CanvaShapeKind.DIAMOND -> {
                        val path = AndroidPath().apply {
                            moveTo(0f, -h / 2f)
                            lineTo(w / 2f, 0f)
                            lineTo(0f, h / 2f)
                            lineTo(-w / 2f, 0f)
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
            CanvaElementType.BADGE -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = elem.textColorHex.toInt()
                    textSize = elem.fontSizeSp * 2.6f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val tw = textPaint.measureText(elem.text) + 64f
                val th = textPaint.textSize + 36f
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = elem.colorHex.toInt()
                    alpha = alphaInt
                }
                canvas.drawRoundRect(RectF(-tw / 2f, -th / 2f, tw / 2f, th / 2f), 28f, 28f, pillPaint)
                canvas.drawText(elem.text, 0f, textPaint.textSize * 0.35f, textPaint)
            }
            CanvaElementType.TEXT -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = elem.textColorHex.toInt()
                    alpha = alphaInt
                    textSize = elem.fontSizeSp * 2.7f
                    typeface = Typeface.create(Typeface.DEFAULT, if (elem.bold) Typeface.BOLD else Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                val lines = elem.text.split("\n")
                val lineHeight = textPaint.textSize * 1.2f
                val totalH = lines.size * lineHeight
                lines.forEachIndexed { idx, line ->
                    val y = -totalH / 2f + lineHeight * (idx + 0.8f)
                    canvas.drawText(line, 0f, y, textPaint)
                }
            }
            CanvaElementType.IMAGE -> {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(elem.imageUri))?.use { stream ->
                        val src = BitmapFactory.decodeStream(stream)
                        if (src != null) {
                            val w = (elem.widthFrac * baseW).roundToInt().coerceAtLeast(100)
                            val h = (elem.heightFrac * baseH).roundToInt().coerceAtLeast(100)
                            val scaled = Bitmap.createScaledBitmap(src, w, h, true)
                            canvas.drawBitmap(scaled, -w / 2f, -h / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = alphaInt })
                            if (scaled != src) scaled.recycle()
                            src.recycle()
                        }
                    }
                }
            }
        }
        canvas.restore()
    }
    return bitmap
}

private suspend fun saveCanvaDesignCachePng(context: Context, project: CanvaProject): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bmp = renderCanvaBitmap(context, project)
            val dir = File(context.cacheDir, "canva_renders").apply { mkdirs() }
            val outFile = File(dir, "canva_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 96, out)
            }
            bmp.recycle()
            Uri.fromFile(outFile)
        }.getOrNull()
    }

private suspend fun exportCanvaBitmapToGallery(context: Context, project: CanvaProject): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bmp = renderCanvaBitmap(context, project)
            val filename = "ss_canva_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/s.s_Canva")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching null
            resolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 96, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bmp.recycle()
            uri
        }.getOrNull()
    }
