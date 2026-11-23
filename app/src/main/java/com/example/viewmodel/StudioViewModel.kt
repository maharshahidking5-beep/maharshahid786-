package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.ai.AiCaptionPack
import com.example.ai.AiEnhanceRecommendation
import com.example.ai.AiGeneratedVideoResult
import com.example.ai.AiLabService
import com.example.ai.AiLabUiState
import com.example.ai.AiTranslatedCaptionPack
import com.example.ai.AiVideoGenUiState
import com.example.ai.AiVideoGeneratorService
import com.example.ai.AiVideoVisualStyle
import com.example.data.ExportEntity
import com.example.data.StudioDatabase
import com.example.data.StudioPreferences
import com.example.data.StudioRepository
import com.example.media.AudioSynthLibrary
import com.example.media.ExportUiState
import com.example.media.MediaInspector
import com.example.media.QuickPresetPack
import com.example.media.SmartMediaHelpers
import com.example.media.StudioCatalog
import com.example.media.VideoExportManager
import com.example.model.AiRemoverMode
import com.example.model.AiRemoverZone
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackItem
import com.example.model.CanvasStyle
import com.example.model.ClipAnimationType
import com.example.model.ClipKeyframe
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.PipOverlayItem
import com.example.model.ProjectData
import com.example.model.RemoveBgMode
import com.example.model.SafeZoneMode
import com.example.model.SpeedCurvePreset
import com.example.model.StickerCatalogItem
import com.example.model.StickerOverlayItem
import com.example.model.TemplateItem
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.TimelineMarker
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoCoverConfig
import com.example.model.VideoEffectPreset
import com.example.model.VoiceEnhancePreset
import com.example.model.VoiceEnhanceSettings
import com.example.ui.theme.StudioFontCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String
)

data class EditorUiState(
    val project: ProjectData = ProjectData(),
    val selectedClipId: String? = null,
    val selectedTextId: String? = null,
    val selectedStickerId: String? = null,
    val selectedPipId: String? = null,
    val selectedRemoverZoneId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val activeToolTab: EditorToolTab = EditorToolTab.ROOT,
    val isFullscreenPreview: Boolean = false,
    val isImportingMedia: Boolean = false,
    val safeZoneMode: SafeZoneMode = SafeZoneMode.NONE,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusToast: String? = null
)

@OptIn(UnstableApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = StudioDatabase.getInstance(appContext).studioDao()
    private val repository = StudioRepository(appContext, dao)
    private val exportManager = VideoExportManager(appContext)
    private val aiLabService = AiLabService()
    private val aiVideoGeneratorService = AiVideoGeneratorService()

    private val _aiLabState = MutableStateFlow<AiLabUiState>(AiLabUiState.Idle)
    val aiLabState: StateFlow<AiLabUiState> = _aiLabState.asStateFlow()

    private val _aiVideoGenState = MutableStateFlow<AiVideoGenUiState>(AiVideoGenUiState.Idle)
    val aiVideoGenState: StateFlow<AiVideoGenUiState> = _aiVideoGenState.asStateFlow()

    private val _aiGeneratedVideos = MutableStateFlow<List<AiGeneratedVideoResult>>(emptyList())
    val aiGeneratedVideos: StateFlow<List<AiGeneratedVideoResult>> = _aiGeneratedVideos.asStateFlow()

    private val _assistantMessages = MutableStateFlow(
        listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Hi! I'm SS Assistant. Ask by voice or text about removing backgrounds, fixing shaky clips, Urdu/Hindi text, beat cuts, or 4K exports."
            )
        )
    )
    val assistantMessages: StateFlow<List<AssistantChatMessage>> = _assistantMessages.asStateFlow()

    private val _isAssistantStreaming = MutableStateFlow(false)
    val isAssistantStreaming: StateFlow<Boolean> = _isAssistantStreaming.asStateFlow()

    private val _assistantError = MutableStateFlow<String?>(null)
    val assistantError: StateFlow<String?> = _assistantError.asStateFlow()

    val projects: StateFlow<List<ProjectData>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exports: StateFlow<List<ExportEntity>> = repository.exports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<StudioPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioPreferences())

    val exportState: StateFlow<ExportUiState> = exportManager.exportState

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val undoStack = ArrayDeque<ProjectData>()
    private val redoStack = ArrayDeque<ProjectData>()
    private val cloudJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    var onCloudSyncProject: ((ProjectData) -> Unit)? = null

    fun clearStatusToast() {
        _editorState.update { it.copy(statusToast = null) }
    }

    private fun pushProjectMutation(
        toast: String? = null,
        saveToUndo: Boolean = true,
        transform: (ProjectData) -> ProjectData
    ) {
        val current = _editorState.value.project
        val updated = transform(current).copy(updatedAtMs = System.currentTimeMillis())
        if (saveToUndo && updated != current) {
            if (undoStack.size >= 25) undoStack.removeFirst()
            undoStack.addLast(current)
            redoStack.clear()
        }
        val validSelectedClip = _editorState.value.selectedClipId
            ?.takeIf { id -> updated.clips.any { it.id == id } }
            ?: updated.clips.firstOrNull()?.id

        _editorState.update {
            it.copy(
                project = updated,
                selectedClipId = validSelectedClip,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = toast ?: it.statusToast
            )
        }
        autoSaveIfEnabled(updated)
    }

    private fun autoSaveIfEnabled(project: ProjectData) {
        if (project.clips.isEmpty()) return
        viewModelScope.launch {
            if (preferences.value.autoSaveDrafts) {
                repository.saveProject(project)
                onCloudSyncProject?.invoke(project)
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _editorState.value.project
        val previous = undoStack.removeLast()
        redoStack.addLast(current)
        _editorState.update {
            it.copy(
                project = previous,
                selectedClipId = previous.clips.firstOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = "Undone"
            )
        }
        autoSaveIfEnabled(previous)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _editorState.value.project
        val next = redoStack.removeLast()
        undoStack.addLast(current)
        _editorState.update {
            it.copy(
                project = next,
                selectedClipId = next.clips.firstOrNull()?.id,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusToast = "Redone"
            )
        }
        autoSaveIfEnabled(next)
    }

    fun createProjectFromUris(
        uris: List<Uri>,
        initialTool: EditorToolTab = EditorToolTab.ROOT,
        autoCut: Boolean = false,
        onReady: () -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _editorState.update { it.copy(isImportingMedia = true) }
            val prefs = preferences.first()
            val clips = uris.mapIndexed { idx, uri ->
                runCatching {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val inspected = MediaInspector.inspectPickedClip(appContext, uri)
                if (autoCut) {
                    val cutEnd = inspected.totalDurationMs.coerceAtMost(2800L)
                    inspected.copy(
                        trimEndMs = cutEnd,
                        transitionAfter = if (idx < uris.lastIndex) TransitionType.FLASH else TransitionType.NONE
                    )
                } else {
                    inspected
                }
            }

            val music = if (autoCut) {
                AudioSynthLibrary.ensureTrackFile(appContext, "neon_pulse")
            } else null

            val title = if (autoCut) "AutoCut Beat Edit" else "Edit ${projects.value.size + 1}"
            val project = ProjectData(
                title = title,
                resolution = prefs.defaultResolution,
                fps = prefs.defaultFps,
                clips = clips,
                audioTrack = music,
                addWatermark = prefs.addEndingWatermark,
                filterPreset = if (autoCut) FilterPreset.CRISP_HDR else FilterPreset.ORIGINAL,
                textOverlays = if (autoCut) {
                    listOf(
                        TextOverlayItem(
                            text = "AUTOCUT REEL",
                            stylePreset = TextStylePreset.CYBER_NEON,
                            startMs = 0L,
                            endMs = 2600L
                        )
                    )
                } else emptyList()
            )
            openFreshProject(project, initialTool)
            repository.saveProject(project)
            onCloudSyncProject?.invoke(project)
            onReady()
        }
    }

    fun createDemoProject(
        initialTool: EditorToolTab = EditorToolTab.ROOT,
        onReady: () -> Unit
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isImportingMedia = true) }
            val prefs = preferences.first()
            val clips = StudioCatalog.createSampleClipsForProject(appContext)
            val music = AudioSynthLibrary.ensureTrackFile(appContext, "lofi_sunset")
            val project = ProjectData(
                title = "Studio Demo Reel",
                aspectRatio = AspectRatioOption.RATIO_9_16,
                resolution = prefs.defaultResolution,
                fps = prefs.defaultFps,
                clips = clips,
                audioTrack = music,
                addWatermark = prefs.addEndingWatermark,
                filterPreset = FilterPreset.TEAL_ORANGE,
                textOverlays = listOf(
                    TextOverlayItem(
                        text = "S.S STUDIO",
                        stylePreset = TextStylePreset.CYBER_NEON,
                        startMs = 0L,
                        endMs = 4000L,
                        normX = 0.5f,
                        normY = 0.72f
                    )
                )
            )
            openFreshProject(project, initialTool)
            repository.saveProject(project)
            onCloudSyncProject?.invoke(project)
            onReady()
        }
    }

    fun createProjectWithPresetPack(
        pack: QuickPresetPack,
        pickedUris: List<Uri> = emptyList(),
        onReady: () -> Unit
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isImportingMedia = true) }
            val prefs = preferences.first()
            val clips = if (pickedUris.isNotEmpty()) {
                pickedUris.map { uri ->
                    runCatching {
                        appContext.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    MediaInspector.inspectPickedClip(appContext, uri)
                }
            } else {
                StudioCatalog.createSampleClipsForProject(appContext)
            }
            val music = AudioSynthLibrary.ensureTrackFile(appContext, pack.audioPresetId)
            val project = ProjectData(
                title = "${pack.title} Edit",
                aspectRatio = pack.aspectRatio,
                resolution = if (prefs.batterySaverMode && prefs.defaultResolution == ExportResolution.RES_2K) {
                    ExportResolution.RES_1080P
                } else {
                    prefs.defaultResolution
                },
                fps = prefs.defaultFps,
                clips = clips,
                audioTrack = music,
                addWatermark = prefs.addEndingWatermark,
                filterPreset = pack.filterPreset,
                effectPreset = pack.effectPreset,
                adjustments = pack.adjustments,
                voiceEnhance = VoiceEnhanceSettings(
                    preset = pack.voicePreset,
                    clarityBoost = pack.voicePreset.clarityBoost,
                    noiseReduction = pack.voicePreset.noiseReduction,
                    loudnessGain = pack.voicePreset.loudnessGain,
                    warmthBass = pack.voicePreset.warmthBass,
                    pitchShift = pack.voicePreset.pitchShift
                ),
                textOverlays = listOf(
                    TextOverlayItem(
                        text = pack.defaultCaption,
                        stylePreset = TextStylePreset.CYBER_NEON,
                        startMs = 0L,
                        endMs = 3500L,
                        normX = 0.5f,
                        normY = 0.72f
                    )
                )
            )
            openFreshProject(project, EditorToolTab.ROOT)
            repository.saveProject(project)
            onCloudSyncProject?.invoke(project)
            _editorState.update { it.copy(statusToast = "Applied ${pack.title} Pack!") }
            onReady()
        }
    }

    fun applyTemplate(
        template: TemplateItem,
        pickedUris: List<Uri>,
        onReady: () -> Unit
    ) {
        viewModelScope.launch {
            _editorState.update { it.copy(isImportingMedia = true) }
            val prefs = preferences.first()
            val clips = if (pickedUris.isNotEmpty()) {
                pickedUris.mapIndexed { idx, uri ->
                    runCatching {
                        appContext.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val c = MediaInspector.inspectPickedClip(appContext, uri)
                    c.copy(
                        trimEndMs = c.totalDurationMs.coerceAtMost(3800L),
                        speed = template.speedPreset,
                        transitionAfter = if (idx < pickedUris.lastIndex) template.transitionType else TransitionType.NONE
                    )
                }
            } else {
                StudioCatalog.createSampleClipsForProject(appContext, template.previewDrawableName)
                    .mapIndexed { idx, clip ->
                        clip.copy(
                            speed = template.speedPreset,
                            transitionAfter = if (idx < 2) template.transitionType else TransitionType.NONE
                        )
                    }
            }

            val music = AudioSynthLibrary.ensureTrackFile(appContext, template.audioPresetId)
            val overlays = buildList {
                add(
                    TextOverlayItem(
                        text = template.defaultCaption,
                        stylePreset = TextStylePreset.CYBER_NEON,
                        startMs = 0L,
                        endMs = 3500L,
                        normX = 0.5f,
                        normY = 0.72f
                    )
                )
                if (template.secondaryCaption.isNotBlank()) {
                    add(
                        TextOverlayItem(
                            text = template.secondaryCaption,
                            stylePreset = TextStylePreset.SUBTITLE,
                            colorHex = TextStylePreset.SUBTITLE.textColorHex,
                            bgHex = TextStylePreset.SUBTITLE.bgColorHex,
                            startMs = 3600L,
                            endMs = 7600L,
                            normX = 0.5f,
                            normY = 0.80f,
                            scale = 0.88f
                        )
                    )
                }
            }
            val project = ProjectData(
                title = template.title,
                aspectRatio = template.aspectRatio,
                resolution = ExportResolution.RES_1080P_PLUS,
                fps = prefs.defaultFps,
                clips = clips,
                audioTrack = music,
                filterPreset = template.filterPreset,
                effectPreset = template.effectPreset,
                adjustments = if (template.autoEnhance) {
                    ColorAdjustments(clarity = 0.55f, contrast = 0.10f, saturation = 1.12f, autoEnhanced = true)
                } else ColorAdjustments(),
                textOverlays = overlays
            )
            openFreshProject(project, EditorToolTab.ROOT)
            repository.saveProject(project)
            onCloudSyncProject?.invoke(project)
            onReady()
        }
    }

    fun openExistingProject(project: ProjectData, initialTool: EditorToolTab = EditorToolTab.ROOT) {
        openFreshProject(project, initialTool)
    }

    fun restoreCloudProject(projectJson: String, onReady: () -> Unit) {
        viewModelScope.launch {
            val decoded = runCatching {
                cloudJson.decodeFromString<ProjectData>(projectJson)
            }.getOrNull()
            if (decoded == null || decoded.clips.isEmpty()) {
                val clips = StudioCatalog.createSampleClipsForProject(appContext)
                val fallback = (decoded ?: ProjectData(title = "Cloud Restored Edit")).copy(clips = clips)
                repository.saveProject(fallback)
                openFreshProject(fallback, EditorToolTab.ROOT)
            } else {
                repository.saveProject(decoded)
                openFreshProject(decoded, EditorToolTab.ROOT)
            }
            onReady()
        }
    }

    fun applyCanvaCoverToActiveOrDemo(coverUriString: String, coverTitle: String, onReady: () -> Unit) {
        val current = _editorState.value.project
        if (current.clips.isNotEmpty()) {
            val updated = current.copy(
                coverConfig = current.coverConfig.copy(
                    coverImageUri = coverUriString,
                    coverTitle = coverTitle.take(40)
                ),
                updatedAtMs = System.currentTimeMillis()
            )
            openFreshProject(updated, EditorToolTab.ROOT)
            viewModelScope.launch { repository.saveProject(updated) }
            onCloudSyncProject?.invoke(updated)
            onReady()
        } else {
            createDemoProject(EditorToolTab.ROOT) {
                val demo = _editorState.value.project
                val updated = demo.copy(
                    coverConfig = demo.coverConfig.copy(
                        coverImageUri = coverUriString,
                        coverTitle = coverTitle.take(40)
                    )
                )
                openFreshProject(updated, EditorToolTab.ROOT)
                onReady()
            }
        }
    }

    private fun openFreshProject(project: ProjectData, initialTool: EditorToolTab) {
        undoStack.clear()
        redoStack.clear()
        _editorState.value = EditorUiState(
            project = project,
            selectedClipId = project.clips.firstOrNull()?.id,
            playheadMs = 0L,
            isPlaying = false,
            activeToolTab = initialTool,
            isImportingMedia = false,
            canUndo = false,
            canRedo = false
        )
    }

    fun appendClipsFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val newClips = uris.map { uri ->
                runCatching {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                MediaInspector.inspectPickedClip(appContext, uri)
            }
            pushProjectMutation("Added ${newClips.size} clip(s)") { p ->
                p.copy(clips = p.clips + newClips)
            }
        }
    }

    fun saveCurrentProjectExplicit() {
        viewModelScope.launch {
            val project = _editorState.value.project
            if (project.clips.isNotEmpty()) {
                repository.saveProject(project)
                onCloudSyncProject?.invoke(project)
                _editorState.update { it.copy(statusToast = "Saved & Synced to Cloud") }
            }
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        val clean = newTitle.trim().ifBlank { "Untitled Edit" }
        if (_editorState.value.project.id == projectId) {
            pushProjectMutation("Renamed project") { it.copy(title = clean) }
        } else {
            viewModelScope.launch {
                val p = repository.getProject(projectId) ?: return@launch
                repository.saveProject(p.copy(title = clean))
            }
        }
    }

    fun duplicateProject(project: ProjectData) {
        viewModelScope.launch {
            val copy = project.copy(
                id = UUID.randomUUID().toString(),
                title = "${project.title} (Copy)",
                updatedAtMs = System.currentTimeMillis()
            )
            repository.saveProject(copy)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch { repository.deleteProject(projectId) }
    }

    // Playback & Selection
    fun setPlaying(playing: Boolean) {
        _editorState.update { it.copy(isPlaying = playing) }
    }

    fun togglePlayPause() {
        _editorState.update { state ->
            val total = state.project.totalDurationMs
            val resetStart = !state.isPlaying && total > 0 && state.playheadMs >= total - 50L
            state.copy(
                isPlaying = !state.isPlaying,
                playheadMs = if (resetStart) 0L else state.playheadMs
            )
        }
    }

    fun seekTo(ms: Long) {
        val total = _editorState.value.project.totalDurationMs
        _editorState.update { it.copy(playheadMs = ms.coerceIn(0L, total)) }
    }

    fun selectClip(clipId: String?) {
        _editorState.update {
            it.copy(
                selectedClipId = clipId,
                selectedTextId = null,
                selectedStickerId = null,
                selectedPipId = null,
                activeToolTab = if (clipId != null) EditorToolTab.EDIT_CLIP else EditorToolTab.ROOT
            )
        }
    }

    fun selectTextOverlay(id: String?) {
        _editorState.update {
            it.copy(
                selectedTextId = id,
                selectedStickerId = null,
                selectedPipId = null,
                activeToolTab = if (id != null) EditorToolTab.TEXT else it.activeToolTab
            )
        }
    }

    fun selectStickerOverlay(id: String?) {
        _editorState.update {
            it.copy(
                selectedStickerId = id,
                selectedTextId = null,
                selectedPipId = null,
                activeToolTab = if (id != null) EditorToolTab.STICKERS else it.activeToolTab
            )
        }
    }

    fun selectPipOverlay(id: String?) {
        _editorState.update {
            it.copy(
                selectedPipId = id,
                selectedTextId = null,
                selectedStickerId = null,
                activeToolTab = if (id != null) EditorToolTab.OVERLAY else it.activeToolTab
            )
        }
    }

    fun selectRemoverZone(id: String?) {
        _editorState.update { it.copy(selectedRemoverZoneId = id) }
    }

    fun setToolTab(tab: EditorToolTab) {
        _editorState.update { it.copy(activeToolTab = tab) }
    }

    fun toggleFullscreenPreview() {
        _editorState.update { it.copy(isFullscreenPreview = !it.isFullscreenPreview) }
    }

    // Clip Editing Actions
    fun splitSelectedClipAtPlayhead() {
        val state = _editorState.value
        val clips = state.project.clips
        if (clips.isEmpty()) return

        var cursor = 0L
        var targetIndex = -1
        var localEffectiveOffsetMs = 0L
        for ((index, clip) in clips.withIndex()) {
            val dur = clip.effectiveDurationMs
            if (state.playheadMs >= cursor && state.playheadMs <= cursor + dur) {
                targetIndex = index
                localEffectiveOffsetMs = state.playheadMs - cursor
                break
            }
            cursor += dur
        }
        if (targetIndex == -1) return
        val target = clips[targetIndex]
        val rawSplitOffset = (localEffectiveOffsetMs * target.speed).toLong()
        val splitPoint = (target.trimStartMs + rawSplitOffset)
        if (splitPoint - target.trimStartMs < 350L || target.trimEndMs - splitPoint < 350L) {
            _editorState.update { it.copy(statusToast = "Move playhead away from clip edges to split") }
            return
        }

        val firstHalf = target.copy(
            trimEndMs = splitPoint,
            transitionAfter = TransitionType.NONE
        )
        val secondHalf = target.copy(
            id = UUID.randomUUID().toString(),
            displayName = "${target.displayName} (2)",
            trimStartMs = splitPoint
        )

        pushProjectMutation("Split clip") { p ->
            val mutable = p.clips.toMutableList()
            mutable.removeAt(targetIndex)
            mutable.add(targetIndex, firstHalf)
            mutable.add(targetIndex + 1, secondHalf)
            p.copy(clips = mutable)
        }
        _editorState.update { it.copy(selectedClipId = secondHalf.id) }
    }

    fun updateClipTrim(clipId: String, trimStartMs: Long, trimEndMs: Long) {
        pushProjectMutation(saveToUndo = true) { p ->
            p.copy(
                clips = p.clips.map { clip ->
                    if (clip.id == clipId) {
                        val safeStart = trimStartMs.coerceIn(0L, (clip.totalDurationMs - 400L).coerceAtLeast(0L))
                        val safeEnd = trimEndMs.coerceIn(safeStart + 400L, clip.totalDurationMs.coerceAtLeast(400L))
                        clip.copy(trimStartMs = safeStart, trimEndMs = safeEnd)
                    } else clip
                }
            )
        }
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        pushProjectMutation("Speed ${String.format("%.2f", safeSpeed)}x") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(speed = safeSpeed, speedCurve = SpeedCurvePreset.NONE) else it
                }
            )
        }
    }

    fun updateClipSpeedCurve(clipId: String, curve: SpeedCurvePreset) {
        pushProjectMutation("Curve: ${curve.label}") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(speed = curve.effectiveMultiplier, speedCurve = curve) else it
                }
            )
        }
    }

    fun updateClipAnimation(clipId: String, animation: ClipAnimationType, durationMs: Long = 750L) {
        pushProjectMutation("Animation: ${animation.label}") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(animation = animation, animationDurationMs = durationMs.coerceIn(300L, 2500L)) else it
                }
            )
        }
    }

    fun freezeFrameAtPlayhead() {
        val state = _editorState.value
        val clips = state.project.clips
        if (clips.isEmpty()) return

        var cursor = 0L
        var targetIndex = -1
        var localOffsetMs = 0L
        for ((index, clip) in clips.withIndex()) {
            val dur = clip.effectiveDurationMs
            if (state.playheadMs >= cursor && state.playheadMs <= cursor + dur) {
                targetIndex = index
                localOffsetMs = state.playheadMs - cursor
                break
            }
            cursor += dur
        }
        if (targetIndex == -1) return
        val target = clips[targetIndex]
        val videoOffsetMs = (localOffsetMs * target.speed).toLong()

        viewModelScope.launch {
            val frameUri = MediaInspector.extractFreezeFrameUri(appContext, target, videoOffsetMs)
            if (frameUri == null) {
                _editorState.update { it.copy(statusToast = "Could not freeze frame on this clip") }
                return@launch
            }
            val freezeClip = VideoClip(
                uriString = frameUri.toString(),
                isPhoto = true,
                displayName = "${target.displayName} (Freeze)",
                totalDurationMs = 2500L,
                trimStartMs = 0L,
                trimEndMs = 2500L,
                rotationDegrees = target.rotationDegrees,
                flipHorizontal = target.flipHorizontal,
                zoomScale = target.zoomScale
            )
            pushProjectMutation("Inserted 2.5s Freeze Frame") { p ->
                val mutable = p.clips.toMutableList()
                mutable.add(targetIndex + 1, freezeClip)
                p.copy(clips = mutable)
            }
            _editorState.update { it.copy(selectedClipId = freezeClip.id) }
        }
    }

    fun replaceSelectedClip(uri: Uri) {
        val selectedId = _editorState.value.selectedClipId ?: _editorState.value.project.clips.firstOrNull()?.id ?: return
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val replacement = MediaInspector.inspectPickedClip(appContext, uri)
            pushProjectMutation("Replaced clip") { p ->
                p.copy(
                    clips = p.clips.map { old ->
                        if (old.id == selectedId) replacement.copy(id = old.id, transitionAfter = old.transitionAfter)
                        else old
                    }
                )
            }
        }
    }

    fun updateClipVolume(clipId: String, volume: Float) {
        pushProjectMutation { p ->
            p.copy(clips = p.clips.map { if (it.id == clipId) it.copy(volume = volume.coerceIn(0f, 1.5f)) else it })
        }
    }

    fun rotateClip(clipId: String) {
        pushProjectMutation("Rotated 90°") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) else it
                }
            )
        }
    }

    fun flipClipHorizontal(clipId: String) {
        pushProjectMutation("Mirrored Horizontal") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(flipHorizontal = !it.flipHorizontal) else it
                }
            )
        }
    }

    fun flipClipVertical(clipId: String) {
        pushProjectMutation("Reflected Vertical") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(flipVertical = !it.flipVertical) else it
                }
            )
        }
    }

    fun updateClipZoom(clipId: String, zoomScale: Float) {
        pushProjectMutation { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(zoomScale = zoomScale.coerceIn(0.35f, 3.5f)) else it
                }
            )
        }
    }

    fun updateClipTransform(
        clipId: String,
        zoomScale: Float,
        panX: Float,
        panY: Float,
        freeRotation: Float,
        saveToUndo: Boolean = false
    ) {
        pushProjectMutation(saveToUndo = saveToUndo) { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) {
                        it.copy(
                            zoomScale = zoomScale.coerceIn(0.35f, 3.5f),
                            panX = panX.coerceIn(-0.8f, 0.8f),
                            panY = panY.coerceIn(-0.8f, 0.8f),
                            freeRotation = freeRotation.coerceIn(-180f, 180f)
                        )
                    } else it
                }
            )
        }
    }

    fun resetClipTransform(clipId: String) {
        pushProjectMutation("Reset Transform") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) {
                        it.copy(
                            zoomScale = 1f,
                            panX = 0f,
                            panY = 0f,
                            rotationDegrees = 0,
                            freeRotation = 0f,
                            flipHorizontal = false,
                            flipVertical = false,
                            keyframes = emptyList()
                        )
                    } else it
                }
            )
        }
    }

    fun applyZoomOnOffPreset(clipId: String, zoomOn: Boolean) {
        pushProjectMutation(if (zoomOn) "Zoom On Keyframed" else "Zoom Off Keyframed") { p ->
            p.copy(
                clips = p.clips.map { clip ->
                    if (clip.id == clipId) {
                        val endMs = clip.effectiveDurationMs.coerceAtLeast(600L)
                        val kfs = if (zoomOn) {
                            listOf(
                                ClipKeyframe(localMs = 0L, zoomScale = 1.0f, panX = 0f, panY = 0f, rotation = 0f),
                                ClipKeyframe(localMs = endMs, zoomScale = 1.48f, panX = 0f, panY = 0f, rotation = 0f)
                            )
                        } else {
                            listOf(
                                ClipKeyframe(localMs = 0L, zoomScale = 1.48f, panX = 0f, panY = 0f, rotation = 0f),
                                ClipKeyframe(localMs = endMs, zoomScale = 1.0f, panX = 0f, panY = 0f, rotation = 0f)
                            )
                        }
                        clip.copy(zoomScale = if (zoomOn) 1.25f else 1.0f, keyframes = kfs)
                    } else clip
                }
            )
        }
    }

    fun toggleKeyframeAtPlayhead() {
        val state = _editorState.value
        val clips = state.project.clips
        if (clips.isEmpty()) return
        var cursor = 0L
        var target: VideoClip? = null
        var localMs = 0L
        for (clip in clips) {
            val dur = clip.effectiveDurationMs
            if (state.playheadMs <= cursor + dur || clip == clips.last()) {
                target = clip
                localMs = (state.playheadMs - cursor).coerceIn(0L, dur)
                break
            }
            cursor += dur
        }
        val active = target ?: return
        val existing = active.keyframes.find { kotlin.math.abs(it.localMs - localMs) < 180L }
        pushProjectMutation(if (existing != null) "Removed Keyframe" else "Added Keyframe ◆") { p ->
            p.copy(
                clips = p.clips.map { c ->
                    if (c.id == active.id) {
                        val nextKfs = if (existing != null) {
                            c.keyframes.filterNot { it.id == existing.id }
                        } else {
                            (c.keyframes + ClipKeyframe(
                                localMs = localMs,
                                zoomScale = c.zoomScale,
                                panX = c.panX,
                                panY = c.panY,
                                rotation = c.freeRotation
                            )).sortedBy { it.localMs }
                        }
                        c.copy(keyframes = nextKfs)
                    } else c
                }
            )
        }
    }

    fun updateClipCrop(clipId: String, left: Float, top: Float, right: Float, bottom: Float) {
        val safeL = left.coerceIn(0f, 0.75f)
        val safeT = top.coerceIn(0f, 0.75f)
        val safeR = right.coerceIn(safeL + 0.20f, 1f)
        val safeB = bottom.coerceIn(safeT + 0.20f, 1f)
        pushProjectMutation(saveToUndo = false) { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(cropLeft = safeL, cropTop = safeT, cropRight = safeR, cropBottom = safeB)
                    else it
                }
            )
        }
    }

    fun resetClipCrop(clipId: String) {
        pushProjectMutation("Reset Crop") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f) else it
                }
            )
        }
    }

    fun updateClipRemoveBg(
        clipId: String,
        mode: RemoveBgMode,
        chromaColorHex: Long? = null,
        sensitivity: Float? = null,
        customBgHex: Long? = null
    ) {
        pushProjectMutation("Remove BG: ${mode.label}") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) {
                        it.copy(
                            removeBgMode = mode,
                            chromaColorHex = chromaColorHex ?: it.chromaColorHex,
                            chromaSensitivity = sensitivity ?: it.chromaSensitivity,
                            customBgHex = customBgHex ?: it.customBgHex
                        )
                    } else it
                }
            )
        }
    }

    fun addAiRemoverZone(clipId: String, mode: AiRemoverMode = AiRemoverMode.SMART_PATCH) {
        val zone = AiRemoverZone(mode = mode)
        pushProjectMutation("Added AI Remover Box — Drag to position") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(aiRemoverZones = it.aiRemoverZones + zone) else it
                }
            )
        }
        _editorState.update { it.copy(selectedRemoverZoneId = zone.id) }
    }

    fun updateAiRemoverZone(clipId: String, zone: AiRemoverZone) {
        pushProjectMutation(saveToUndo = false) { p ->
            p.copy(
                clips = p.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(aiRemoverZones = clip.aiRemoverZones.map { if (it.id == zone.id) zone else it })
                    } else clip
                }
            )
        }
    }

    fun deleteAiRemoverZone(clipId: String, zoneId: String) {
        pushProjectMutation("Removed eraser box") { p ->
            p.copy(
                clips = p.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(aiRemoverZones = clip.aiRemoverZones.filterNot { it.id == zoneId })
                    } else clip
                }
            )
        }
        _editorState.update { it.copy(selectedRemoverZoneId = null) }
    }

    fun duplicateClip(clipId: String) {
        pushProjectMutation("Duplicated clip") { p ->
            val idx = p.clips.indexOfFirst { it.id == clipId }
            if (idx == -1) return@pushProjectMutation p
            val copy = p.clips[idx].copy(id = UUID.randomUUID().toString())
            val mutable = p.clips.toMutableList()
            mutable.add(idx + 1, copy)
            p.copy(clips = mutable)
        }
    }

    fun deleteClip(clipId: String) {
        val currentClips = _editorState.value.project.clips
        if (currentClips.size <= 1) {
            _editorState.update { it.copy(statusToast = "Keep at least 1 clip in timeline") }
            return
        }
        pushProjectMutation("Deleted clip") { p ->
            p.copy(clips = p.clips.filterNot { it.id == clipId })
        }
    }

    fun moveClip(clipId: String, delta: Int) {
        pushProjectMutation("Reordered clip") { p ->
            val idx = p.clips.indexOfFirst { it.id == clipId }
            val newIdx = (idx + delta).coerceIn(0, p.clips.lastIndex)
            if (idx == -1 || idx == newIdx) return@pushProjectMutation p
            val mutable = p.clips.toMutableList()
            val moved = mutable.removeAt(idx)
            mutable.add(newIdx, moved)
            p.copy(clips = mutable)
        }
    }

    fun updateClipTransition(clipId: String, transitionType: TransitionType) {
        pushProjectMutation("Transition: ${transitionType.label}") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(transitionAfter = transitionType) else it
                }
            )
        }
    }

    // Video Cover & Timeline Markers
    fun updateCoverConfig(config: VideoCoverConfig) {
        pushProjectMutation("Updated Video Cover") { p -> p.copy(coverConfig = config) }
    }

    fun captureCoverFromPlayhead(title: String = _editorState.value.project.coverConfig.coverTitle) {
        val state = _editorState.value
        val clips = state.project.clips
        if (clips.isEmpty()) return
        var cursor = 0L
        var target = clips.first()
        var localMs = 0L
        for (clip in clips) {
            val dur = clip.effectiveDurationMs
            if (state.playheadMs <= cursor + dur || clip == clips.last()) {
                target = clip
                localMs = (state.playheadMs - cursor).coerceIn(0L, dur)
                break
            }
            cursor += dur
        }
        viewModelScope.launch {
            val uri = if (target.isPhoto) {
                Uri.parse(target.uriString)
            } else {
                MediaInspector.extractFreezeFrameUri(appContext, target, (localMs * target.speed).toLong())
            }
            pushProjectMutation("Video Cover Saved") { p ->
                p.copy(
                    coverConfig = p.coverConfig.copy(
                        coverImageUri = uri?.toString() ?: p.coverConfig.coverImageUri,
                        coverTimestampMs = state.playheadMs,
                        coverTitle = title
                    )
                )
            }
        }
    }

    fun toggleMarkerAtPlayhead(label: String = "Mark") {
        val ms = _editorState.value.playheadMs
        val existing = _editorState.value.project.markers.find { kotlin.math.abs(it.timeMs - ms) < 250L }
        pushProjectMutation(if (existing != null) "Removed Marker" else "Added Timeline Mark") { p ->
            val updated = if (existing != null) {
                p.markers.filterNot { it.id == existing.id }
            } else {
                (p.markers + TimelineMarker(timeMs = ms, label = label)).sortedBy { it.timeMs }
            }
            p.copy(markers = updated)
        }
    }

    fun deleteMarker(id: String) {
        pushProjectMutation("Deleted Marker") { p ->
            p.copy(markers = p.markers.filterNot { it.id == id })
        }
    }

    // Audio Studio
    fun toggleMuteOriginalAudio() {
        pushProjectMutation { p ->
            p.copy(isOriginalAudioMuted = !p.isOriginalAudioMuted)
        }
    }

    fun selectStudioMusic(presetId: String) {
        viewModelScope.launch {
            val track = AudioSynthLibrary.ensureTrackFile(appContext, presetId)
            pushProjectMutation("Added track: ${track.title}") { p ->
                p.copy(audioTrack = track)
            }
        }
    }

    fun importAudioUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val title = queryFileName(uri) ?: "Custom Audio"
            val track = AudioTrackItem(
                title = title,
                artist = "Device Audio",
                uriString = uri.toString(),
                durationMs = 30000L,
                volume = 0.85f
            )
            pushProjectMutation("Imported $title") { it.copy(audioTrack = track) }
        }
    }

    fun updateAudioVolume(volume: Float) {
        pushProjectMutation { p ->
            val track = p.audioTrack ?: return@pushProjectMutation p
            p.copy(audioTrack = track.copy(volume = volume.coerceIn(0f, 1.2f)))
        }
    }

    fun toggleAudioMute() {
        pushProjectMutation { p ->
            val track = p.audioTrack ?: return@pushProjectMutation p
            p.copy(audioTrack = track.copy(isMuted = !track.isMuted))
        }
    }

    fun removeAudioTrack() {
        pushProjectMutation("Removed audio") { it.copy(audioTrack = null) }
    }

    fun updateAudioFades(fadeInMs: Long, fadeOutMs: Long) {
        pushProjectMutation("Updated audio fades") { p ->
            val track = p.audioTrack ?: return@pushProjectMutation p
            p.copy(audioTrack = track.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs))
        }
    }

    fun addSfxAtPlayhead(sfxId: String) {
        val startMs = _editorState.value.playheadMs
        viewModelScope.launch {
            val sfxItem = AudioSynthLibrary.ensureSfxFile(appContext, sfxId, startMs)
            pushProjectMutation("Added SFX: ${sfxItem.title}") { p ->
                p.copy(sfxOverlays = p.sfxOverlays + sfxItem)
            }
        }
    }

    fun deleteSfxOverlay(id: String) {
        pushProjectMutation("Removed SFX") { p ->
            p.copy(sfxOverlays = p.sfxOverlays.filterNot { it.id == id })
        }
    }

    // Text & Captions (Multilingual Urdu / Hindi / English + Curved / 3D / Neon / RTL)
    fun addTextOverlay(
        text: String,
        preset: TextStylePreset = TextStylePreset.CYBER_NEON,
        animation: TextAnimationType = TextAnimationType.POP,
        fontFamilyKey: String = "display",
        isCurved: Boolean = false,
        is3D: Boolean = false,
        isNeonGlow: Boolean = preset.hasGlow,
        isRtl: Boolean = false,
        languageTag: String = "EN"
    ) {
        val state = _editorState.value
        val start = state.playheadMs
        val total = state.project.totalDurationMs.coerceAtLeast(3000L)
        val end = (start + 3500L).coerceAtMost(total).coerceAtLeast(start + 800L)
        val resolvedFont = if (fontFamilyKey == "display" && languageTag != "EN") {
            StudioFontCatalog.defaultKeyForLanguage(languageTag)
        } else fontFamilyKey
        val newItem = TextOverlayItem(
            text = text.ifBlank { "Tap to edit" },
            stylePreset = preset,
            colorHex = preset.textColorHex,
            bgHex = preset.bgColorHex,
            fontFamilyKey = resolvedFont,
            startMs = start,
            endMs = end,
            animationType = animation,
            isCurved = isCurved,
            is3D = is3D,
            isNeonGlow = isNeonGlow,
            isRtl = isRtl,
            languageTag = languageTag
        )
        pushProjectMutation("Added $languageTag text") { p ->
            p.copy(textOverlays = p.textOverlays + newItem)
        }
        _editorState.update { it.copy(selectedTextId = newItem.id) }
    }

    fun generateAutoCaptions() {
        val total = _editorState.value.project.totalDurationMs.coerceAtLeast(4000L)
        val phrases = listOf("Watch until the end", "Every frame tells a story", "Edited in s.s studio")
        val slot = (total / phrases.size).coerceAtLeast(1000L)
        val captions = phrases.mapIndexed { idx, line ->
            val s = idx * slot
            val e = ((idx + 1) * slot).coerceAtMost(total)
            TextOverlayItem(
                text = line,
                stylePreset = TextStylePreset.SUBTITLE,
                colorHex = TextStylePreset.SUBTITLE.textColorHex,
                bgHex = TextStylePreset.SUBTITLE.bgColorHex,
                startMs = s,
                endMs = e,
                normX = 0.5f,
                normY = 0.84f,
                scale = 0.85f,
                animationType = TextAnimationType.FADE
            )
        }
        pushProjectMutation("Generated ${captions.size} captions") { p ->
            p.copy(textOverlays = p.textOverlays + captions)
        }
    }

    fun updateTextOverlay(item: TextOverlayItem) {
        pushProjectMutation { p ->
            p.copy(textOverlays = p.textOverlays.map { if (it.id == item.id) item else it })
        }
    }

    fun duplicateTextOverlay(id: String) {
        pushProjectMutation("Duplicated text") { p ->
            val target = p.textOverlays.find { it.id == id } ?: return@pushProjectMutation p
            val copy = target.copy(
                id = UUID.randomUUID().toString(),
                normY = (target.normY + 0.08f).coerceAtMost(0.88f)
            )
            p.copy(textOverlays = p.textOverlays + copy)
        }
    }

    fun deleteTextOverlay(id: String) {
        pushProjectMutation("Removed text") { p ->
            p.copy(textOverlays = p.textOverlays.filterNot { it.id == id })
        }
        _editorState.update { it.copy(selectedTextId = null) }
    }

    // Stickers
    fun addStickerOverlay(catalogItem: StickerCatalogItem) {
        val state = _editorState.value
        val start = state.playheadMs
        val total = state.project.totalDurationMs.coerceAtLeast(3000L)
        val end = (start + 3500L).coerceAtMost(total).coerceAtLeast(start + 800L)
        val sticker = StickerOverlayItem(
            stickerId = catalogItem.id,
            label = catalogItem.label,
            badgeText = catalogItem.badgeText,
            accentHex = (catalogItem.accentColor.value shr 32).toLong(),
            startMs = start,
            endMs = end
        )
        pushProjectMutation("Added ${catalogItem.label} sticker") { p ->
            p.copy(stickerOverlays = p.stickerOverlays + sticker)
        }
        _editorState.update { it.copy(selectedStickerId = sticker.id) }
    }

    fun updateStickerOverlay(item: StickerOverlayItem) {
        pushProjectMutation { p ->
            p.copy(stickerOverlays = p.stickerOverlays.map { if (it.id == item.id) item else it })
        }
    }

    fun deleteStickerOverlay(id: String) {
        pushProjectMutation("Removed sticker") { p ->
            p.copy(stickerOverlays = p.stickerOverlays.filterNot { it.id == id })
        }
        _editorState.update { it.copy(selectedStickerId = null) }
    }

    // PiP Overlay
    fun addPipOverlay(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val state = _editorState.value
            val start = state.playheadMs
            val total = state.project.totalDurationMs.coerceAtLeast(3000L)
            val end = (start + 4000L).coerceAtMost(total).coerceAtLeast(start + 1000L)
            val mime = appContext.contentResolver.getType(uri) ?: ""
            val pip = PipOverlayItem(
                uriString = uri.toString(),
                isPhoto = mime.startsWith("image/"),
                startMs = start,
                endMs = end
            )
            pushProjectMutation("Added PiP overlay") { p ->
                p.copy(pipOverlays = p.pipOverlays + pip)
            }
            _editorState.update { it.copy(selectedPipId = pip.id) }
        }
    }

    fun updatePipOverlay(item: PipOverlayItem) {
        pushProjectMutation { p ->
            p.copy(pipOverlays = p.pipOverlays.map { if (it.id == item.id) item else it })
        }
    }

    fun deletePipOverlay(id: String) {
        pushProjectMutation("Removed PiP overlay") { p ->
            p.copy(pipOverlays = p.pipOverlays.filterNot { it.id == id })
        }
        _editorState.update { it.copy(selectedPipId = null) }
    }

    // Filters, Effects, Adjustments, Ratio, Canvas
    fun updateFilter(preset: FilterPreset, intensity: Float = _editorState.value.project.filterIntensity) {
        pushProjectMutation { p ->
            p.copy(filterPreset = preset, filterIntensity = intensity.coerceIn(0f, 1f))
        }
    }

    fun updateEffect(preset: VideoEffectPreset, intensity: Float = _editorState.value.project.effectIntensity) {
        pushProjectMutation(if (preset == VideoEffectPreset.NONE) "Effects Off" else "Effect: ${preset.label}") { p ->
            p.copy(effectPreset = preset, effectIntensity = intensity.coerceIn(0.1f, 1f))
        }
    }

    fun updateAdjustments(adjustments: ColorAdjustments) {
        pushProjectMutation { p ->
            p.copy(adjustments = adjustments)
        }
    }

    fun resetAdjustments() {
        pushProjectMutation("Reset color adjustments") { p ->
            p.copy(adjustments = ColorAdjustments(), filterPreset = FilterPreset.ORIGINAL, filterIntensity = 1f)
        }
    }

    fun updateAspectRatio(ratio: AspectRatioOption) {
        pushProjectMutation("Ratio ${ratio.label}") { p ->
            p.copy(aspectRatio = ratio)
        }
    }

    fun applyOneClickRatioAndSmartCrop(ratio: AspectRatioOption, smartAutoCrop: Boolean = true) {
        if (!smartAutoCrop) {
            updateAspectRatio(ratio)
            return
        }
        val currentProject = _editorState.value.project
        if (currentProject.clips.isEmpty()) {
            updateAspectRatio(ratio)
            return
        }
        viewModelScope.launch {
            var subjectLabel = "Smart Auto-Crop"
            val croppedClips = currentProject.clips.mapIndexed { idx, clip ->
                val result = SmartMediaHelpers.computeSmartAutoCrop(appContext, clip, ratio)
                if (idx == 0) subjectLabel = result.detectedSubjectLabel
                clip.copy(
                    cropLeft = result.cropLeft,
                    cropTop = result.cropTop,
                    cropRight = result.cropRight,
                    cropBottom = result.cropBottom,
                    panX = result.panX,
                    panY = result.panY
                )
            }
            pushProjectMutation("${ratio.label} · $subjectLabel") { p ->
                p.copy(aspectRatio = ratio, clips = croppedClips)
            }
        }
    }

    fun applyQuickPresetPack(pack: QuickPresetPack) {
        viewModelScope.launch {
            val track = _editorState.value.project.audioTrack
                ?: AudioSynthLibrary.ensureTrackFile(appContext, pack.audioPresetId)
            pushProjectMutation("Applied ${pack.title} Pack!") { p ->
                p.copy(
                    filterPreset = pack.filterPreset,
                    effectPreset = pack.effectPreset,
                    adjustments = pack.adjustments,
                    audioTrack = track,
                    voiceEnhance = VoiceEnhanceSettings(
                        preset = pack.voicePreset,
                        clarityBoost = pack.voicePreset.clarityBoost,
                        noiseReduction = pack.voicePreset.noiseReduction,
                        loudnessGain = pack.voicePreset.loudnessGain,
                        warmthBass = pack.voicePreset.warmthBass,
                        pitchShift = pack.voicePreset.pitchShift
                    )
                )
            }
        }
    }

    fun applyAutoBeatMarkers(bpm: Int = 124, rhythmCut: Boolean = false) {
        val current = _editorState.value.project
        if (current.clips.isEmpty()) return
        val updatedClips = if (rhythmCut) {
            SmartMediaHelpers.applyRhythmCutToClips(current.clips, bpm = bpm)
        } else {
            current.clips
        }
        val totalMs = updatedClips.sumOf { it.effectiveDurationMs }.coerceAtLeast(1000L)
        val beatMarkers = SmartMediaHelpers.generateBeatMarkers(totalMs, bpm = bpm)
        pushProjectMutation(
            if (rhythmCut) "Rhythm Cut (${bpm} BPM) + ${beatMarkers.size} Beat Marks"
            else "Added ${beatMarkers.size} Beat Marks (${bpm} BPM)"
        ) { p ->
            val nonBeat = p.markers.filterNot { it.label.startsWith("♪") }
            p.copy(
                clips = updatedClips,
                markers = (nonBeat + beatMarkers).sortedBy { it.timeMs }
            )
        }
    }

    fun cycleSafeZoneMode() {
        val all = SafeZoneMode.entries
        _editorState.update { cur ->
            val next = all[(all.indexOf(cur.safeZoneMode) + 1) % all.size]
            cur.copy(
                safeZoneMode = next,
                statusToast = if (next == SafeZoneMode.NONE) "Safe-Zone Overlay OFF" else "Safe-Zone: ${next.label}"
            )
        }
    }

    fun setSafeZoneMode(mode: SafeZoneMode) {
        _editorState.update { it.copy(safeZoneMode = mode) }
    }

    fun toggleProjectWatermark(enabled: Boolean) {
        pushProjectMutation(if (enabled) "s.s Watermark ON" else "Watermark OFF") { p ->
            p.copy(addWatermark = enabled)
        }
        updateStudioPreferences(addWatermark = enabled)
    }

    fun askSsAssistant(question: String) {
        val clean = question.trim()
        if (clean.isEmpty() || _isAssistantStreaming.value) return
        _assistantError.value = null
        val project = _editorState.value.project
        val prefs = preferences.value
        val contextSummary = "Project '${project.title}', ${project.clips.size} clips, " +
            "ratio ${project.aspectRatio.label}, export ${project.resolution.label} @ ${project.fps}fps, " +
            "filter ${project.filterPreset.label}, effect ${project.effectPreset.label}, " +
            "batterySaver=${prefs.batterySaverMode}, watermark=${project.addWatermark}."
        val historyPairs = _assistantMessages.value.map { msg ->
            (if (msg.isUser) "User" else "Assistant") to msg.text
        }
        val userMsg = AssistantChatMessage(isUser = true, text = clean)
        val replyId = UUID.randomUUID().toString()
        _assistantMessages.update { it + userMsg + AssistantChatMessage(id = replyId, isUser = false, text = "") }
        _isAssistantStreaming.value = true

        viewModelScope.launch {
            try {
                aiLabService.streamAssistantHelp(clean, contextSummary, historyPairs).collect { partial ->
                    _assistantMessages.update { list ->
                        list.map { m -> if (m.id == replyId) m.copy(text = partial) else m }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _assistantError.value = "SS Assistant timed out. Try a shorter question."
                _assistantMessages.update { list -> list.filterNot { it.id == replyId && it.text.isBlank() } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _assistantError.value = e.localizedMessage ?: "Could not reach SS Assistant."
                _assistantMessages.update { list -> list.filterNot { it.id == replyId && it.text.isBlank() } }
            } finally {
                _isAssistantStreaming.value = false
            }
        }
    }

    fun clearAssistantHistory() {
        _assistantError.value = null
        _assistantMessages.value = listOf(
            AssistantChatMessage(
                isUser = false,
                text = "Chat cleared! Ask me anything by voice or text."
            )
        )
    }

    fun updateCanvasColor(colorHex: Long) {
        pushProjectMutation { p -> p.copy(canvasColorHex = colorHex) }
    }

    fun updateCanvasStyle(style: CanvasStyle) {
        pushProjectMutation("Canvas: ${style.label}") { p -> p.copy(canvasStyle = style) }
    }

    fun updateExportSettings(resolution: ExportResolution, fps: Int) {
        pushProjectMutation("Export ${resolution.label} · ${fps}fps") { p ->
            p.copy(resolution = resolution, fps = fps)
        }
    }

    // One-Tap 1080P+ Auto Enhance Video
    fun toggleAutoEnhanceVideo() {
        val current = _editorState.value.project
        val enable = !current.adjustments.autoEnhanced
        pushProjectMutation(
            if (enable) "1080P+ Auto Enhance ON" else "Auto Enhance OFF"
        ) { p ->
            val updatedAdj = if (enable) {
                p.adjustments.copy(
                    autoEnhanced = true,
                    clarity = p.adjustments.clarity.coerceAtLeast(0.65f),
                    contrast = p.adjustments.contrast.coerceAtLeast(0.12f),
                    saturation = p.adjustments.saturation.coerceAtLeast(1.12f),
                    brightness = p.adjustments.brightness.coerceAtLeast(0.02f)
                )
            } else {
                p.adjustments.copy(autoEnhanced = false, clarity = 0f)
            }
            val upgradedRes = if (enable && p.resolution.scaleFactor < ExportResolution.RES_1080P_PLUS.scaleFactor) {
                ExportResolution.RES_1080P_PLUS
            } else p.resolution
            p.copy(adjustments = updatedAdj, resolution = upgradedRes)
        }
    }

    // Voice Enhancer Controls
    fun selectVoicePreset(preset: VoiceEnhancePreset) {
        pushProjectMutation("Voice: ${preset.label}") { p ->
            p.copy(
                voiceEnhance = VoiceEnhanceSettings(
                    preset = preset,
                    clarityBoost = preset.clarityBoost,
                    noiseReduction = preset.noiseReduction,
                    loudnessGain = preset.loudnessGain,
                    warmthBass = preset.warmthBass,
                    pitchShift = preset.pitchShift
                )
            )
        }
    }

    fun updateVoiceSettings(settings: VoiceEnhanceSettings) {
        pushProjectMutation { p -> p.copy(voiceEnhance = settings) }
    }

    // Firebase AI Lab Actions
    fun resetAiLabState() {
        _aiLabState.value = AiLabUiState.Idle
    }

    fun runAiVideoEnhance() {
        val project = _editorState.value.project
        val targetClip = project.clips.find { it.id == _editorState.value.selectedClipId } ?: project.clips.firstOrNull()
        viewModelScope.launch {
            _aiLabState.value = AiLabUiState.Loading("Analyzing frame & lighting in 1080P+...")
            try {
                val rec = aiLabService.analyzeAndRecommendEnhancement(appContext, targetClip, project.title)
                _aiLabState.value = AiLabUiState.EnhanceReady(rec)
            } catch (e: TimeoutCancellationException) {
                _aiLabState.value = AiLabUiState.Error("AI Lab analysis timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiLabState.value = AiLabUiState.Error(e.localizedMessage ?: "AI analysis failed.")
            }
        }
    }

    fun applyAiEnhanceRecommendation(rec: AiEnhanceRecommendation) {
        pushProjectMutation("Applied AI 1080P+ Grade") { p ->
            p.copy(
                resolution = ExportResolution.RES_1080P_PLUS,
                filterPreset = rec.filterPreset,
                effectPreset = rec.effectPreset,
                adjustments = ColorAdjustments(
                    brightness = rec.brightness,
                    contrast = rec.contrast,
                    saturation = rec.saturation,
                    warmth = rec.warmth,
                    clarity = rec.clarity,
                    autoEnhanced = true
                ),
                textOverlays = if (rec.hookCaption.isNotBlank() && p.textOverlays.isEmpty()) {
                    listOf(
                        TextOverlayItem(
                            text = rec.hookCaption,
                            stylePreset = TextStylePreset.CYBER_NEON,
                            startMs = 0L,
                            endMs = p.totalDurationMs.coerceAtMost(3500L).coerceAtLeast(1000L)
                        )
                    )
                } else p.textOverlays
            )
        }
    }

    fun runAiSmartCaptions(topicPrompt: String) {
        val project = _editorState.value.project
        val targetClip = project.clips.find { it.id == _editorState.value.selectedClipId } ?: project.clips.firstOrNull()
        viewModelScope.launch {
            _aiLabState.value = AiLabUiState.Loading("Generating viral captions...")
            try {
                val pack = aiLabService.generateSmartCaptions(appContext, targetClip, topicPrompt)
                _aiLabState.value = AiLabUiState.CaptionsReady(pack)
            } catch (e: TimeoutCancellationException) {
                _aiLabState.value = AiLabUiState.Error("Caption generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiLabState.value = AiLabUiState.Error(e.localizedMessage ?: "AI caption generation failed.")
            }
        }
    }

    fun applyAiCaptions(pack: AiCaptionPack) {
        val total = _editorState.value.project.totalDurationMs.coerceAtLeast(4500L)
        val slot = (total / 3L).coerceAtLeast(1000L)
        val newItems = listOf(
            TextOverlayItem(
                text = pack.hookTitle,
                stylePreset = TextStylePreset.CYBER_NEON,
                colorHex = TextStylePreset.CYBER_NEON.textColorHex,
                bgHex = TextStylePreset.CYBER_NEON.bgColorHex,
                startMs = 0L,
                endMs = slot,
                normX = 0.5f,
                normY = 0.72f
            ),
            TextOverlayItem(
                text = pack.middleCaption,
                stylePreset = TextStylePreset.SUBTITLE,
                colorHex = TextStylePreset.SUBTITLE.textColorHex,
                bgHex = TextStylePreset.SUBTITLE.bgColorHex,
                startMs = slot,
                endMs = (slot * 2L).coerceAtMost(total),
                normX = 0.5f,
                normY = 0.80f,
                scale = 0.9f
            ),
            TextOverlayItem(
                text = pack.outroCta,
                stylePreset = TextStylePreset.ROSE_POP,
                colorHex = TextStylePreset.ROSE_POP.textColorHex,
                bgHex = TextStylePreset.ROSE_POP.bgColorHex,
                startMs = (slot * 2L).coerceAtMost(total - 800L),
                endMs = total,
                normX = 0.5f,
                normY = 0.75f
            )
        )
        pushProjectMutation("Added AI Captions") { p ->
            p.copy(textOverlays = p.textOverlays + newItems)
        }
    }

    fun streamAiScript(topic: String, tone: String) {
        viewModelScope.launch {
            _aiLabState.value = AiLabUiState.Loading("Writing creator script...")
            var lastText = ""
            try {
                aiLabService.streamCreatorScript(topic, tone).collect { partial ->
                    lastText = partial
                    _aiLabState.value = AiLabUiState.StreamingScript(partial)
                }
                if (lastText.isNotBlank()) {
                    _aiLabState.value = AiLabUiState.ScriptReady(lastText)
                } else {
                    _aiLabState.value = AiLabUiState.Error("AI returned an empty script.")
                }
            } catch (e: TimeoutCancellationException) {
                _aiLabState.value = AiLabUiState.Error("Script generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiLabState.value = AiLabUiState.Error(e.localizedMessage ?: "Script generation failed.")
            }
        }
    }

    fun runAiVideoTranslate(sourceDialogue: String, targetLanguage: String) {
        val project = _editorState.value.project
        val targetClip = project.clips.find { it.id == _editorState.value.selectedClipId } ?: project.clips.firstOrNull()
        val combinedText = sourceDialogue.trim().ifBlank {
            project.textOverlays.joinToString(" | ") { it.text }
        }
        viewModelScope.launch {
            _aiLabState.value = AiLabUiState.Loading("Translating video captions to $targetLanguage...")
            try {
                val pack = aiLabService.translateAndCaptionVideo(appContext, targetClip, combinedText, targetLanguage)
                _aiLabState.value = AiLabUiState.TranslatedReady(pack)
            } catch (e: TimeoutCancellationException) {
                _aiLabState.value = AiLabUiState.Error("Translation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiLabState.value = AiLabUiState.Error(e.localizedMessage ?: "Video translation failed.")
            }
        }
    }

    fun applyAiTranslatedCaptions(pack: AiTranslatedCaptionPack) {
        val lines = listOf(pack.line1, pack.line2, pack.line3).filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        val total = _editorState.value.project.totalDurationMs.coerceAtLeast(4500L)
        val slot = (total / lines.size).coerceAtLeast(1000L)
        val newItems = lines.mapIndexed { idx, line ->
            TextOverlayItem(
                text = line,
                stylePreset = TextStylePreset.SUBTITLE,
                colorHex = TextStylePreset.SUBTITLE.textColorHex,
                bgHex = TextStylePreset.SUBTITLE.bgColorHex,
                startMs = idx * slot,
                endMs = ((idx + 1) * slot).coerceAtMost(total),
                normX = 0.5f,
                normY = 0.82f,
                scale = 0.92f,
                animationType = TextAnimationType.FADE
            )
        }
        pushProjectMutation("Added ${pack.targetLanguage} Captions") { p ->
            p.copy(textOverlays = p.textOverlays + newItems)
        }
    }

    // ==========================================
    // AI PROMPT-TO-VIDEO PRO GENERATOR
    // ==========================================

    fun resetAiVideoGenState() {
        _aiVideoGenState.value = AiVideoGenUiState.Idle
    }

    fun generateAiVideoFromPrompt(
        prompt: String,
        style: AiVideoVisualStyle,
        aspectRatio: AspectRatioOption,
        hasProAccess: Boolean,
        onRequireProPackage: () -> Unit = {}
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            _aiVideoGenState.value = AiVideoGenUiState.Error("Please write a video prompt first (Urdu, Hindi, or English).")
            return
        }
        viewModelScope.launch {
            val prefs = repository.preferences.first()
            if (!hasProAccess && prefs.aiVideoTrialCount >= 1) {
                _aiVideoGenState.value = AiVideoGenUiState.Error(
                    "Free AI Video trial used (1/1). Purchase a Pro package via JazzCash or EasyPaisa to unlock unlimited AI Video Gen Pro!"
                )
                onRequireProPackage()
                return@launch
            }
            _aiVideoGenState.value = AiVideoGenUiState.Generating("Connecting to AI Video Director...", 0.08f)
            try {
                val targetRes = if (hasProAccess) ExportResolution.RES_1080P_PLUS else ExportResolution.RES_1080P
                val result = aiVideoGeneratorService.generateAiVideo(
                    context = appContext,
                    prompt = cleanPrompt,
                    style = style,
                    aspectRatio = aspectRatio,
                    addWatermark = if (hasProAccess) prefs.addEndingWatermark else true,
                    targetResolution = targetRes,
                    targetFps = prefs.defaultFps
                ) { stage, fraction ->
                    _aiVideoGenState.value = AiVideoGenUiState.Generating(stage, fraction)
                }
                if (!hasProAccess) {
                    repository.updatePreferences(aiVideoTrialCount = prefs.aiVideoTrialCount + 1)
                }
                repository.saveProject(result.project)
                onCloudSyncProject?.invoke(result.project)
                _aiGeneratedVideos.update { current -> listOf(result) + current.take(9) }
                _aiVideoGenState.value = AiVideoGenUiState.Ready(result)
            } catch (e: TimeoutCancellationException) {
                _aiVideoGenState.value = AiVideoGenUiState.Error(
                    e.localizedMessage ?: "AI video synthesis timed out. Please try a shorter prompt or retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiVideoGenState.value = AiVideoGenUiState.Error(
                    e.localizedMessage ?: "AI Video Pro generation failed. Check connection and try again."
                )
            }
        }
    }

    fun openGeneratedVideoInEditor(result: AiGeneratedVideoResult, onReady: () -> Unit) {
        viewModelScope.launch {
            repository.saveProject(result.project)
            openFreshProject(result.project, EditorToolTab.ROOT)
            onReady()
        }
    }

    // Export Execution
    fun startExport() {
        val project = _editorState.value.project
        _editorState.update { it.copy(isPlaying = false) }
        exportManager.startExport(project) { entity ->
            viewModelScope.launch {
                repository.recordExport(entity)
            }
        }
    }

    fun cancelExport() {
        exportManager.cancelExport()
        exportManager.resetState()
    }

    fun resetExportState() {
        exportManager.resetState()
    }

    fun deleteExport(entity: ExportEntity) {
        viewModelScope.launch { repository.deleteExport(entity) }
    }

    fun updateStudioPreferences(
        defaultResolution: ExportResolution? = null,
        defaultFps: Int? = null,
        addWatermark: Boolean? = null,
        autoSaveDrafts: Boolean? = null,
        batterySaverMode: Boolean? = null,
        autoSyncOnline: Boolean? = null,
        lastCloudSyncMs: Long? = null,
        appThemeMode: String? = null,
        customBgUri: String? = null,
        customBgBlur: Float? = null,
        linkedPhone: String? = null,
        isPakistaniPro: Boolean? = null,
        profileCoverStyle: String? = null,
        profileLocalPhotoUri: String? = null,
        profileLocalBannerUri: String? = null,
        studioPasswordHash: String? = null,
        studioPasswordSalt: String? = null,
        registeredDevicesJson: String? = null
    ) {
        viewModelScope.launch {
            repository.updatePreferences(
                defaultResolution = defaultResolution,
                defaultFps = defaultFps,
                addEndingWatermark = addWatermark,
                autoSaveDrafts = autoSaveDrafts,
                batterySaverMode = batterySaverMode,
                autoSyncOnline = autoSyncOnline,
                lastCloudSyncMs = lastCloudSyncMs,
                appThemeMode = appThemeMode,
                customBgUri = customBgUri,
                customBgBlur = customBgBlur,
                linkedPhone = linkedPhone,
                isPakistaniPro = isPakistaniPro,
                profileCoverStyle = profileCoverStyle,
                profileLocalPhotoUri = profileLocalPhotoUri,
                profileLocalBannerUri = profileLocalBannerUri,
                studioPasswordHash = studioPasswordHash,
                studioPasswordSalt = studioPasswordSalt,
                registeredDevicesJson = registeredDevicesJson
            )
        }
    }

    fun registerCurrentDeviceSession(userId: String) {
        viewModelScope.launch {
            val prefs = repository.preferences.first()
            val updated = com.example.auth.AccountSecurityHelpers.registerDeviceInList(
                existingJson = prefs.registeredDevicesJson,
                deviceId = "${userId.take(12)}_${android.os.Build.MODEL.replace(" ", "_")}",
                deviceLabel = com.example.auth.AccountSecurityHelpers.currentDeviceLabel()
            )
            repository.updatePreferences(registeredDevicesJson = updated)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        exportManager.cancelExport()
    }
}
