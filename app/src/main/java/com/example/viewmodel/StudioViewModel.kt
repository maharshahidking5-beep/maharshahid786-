package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.ExportEntity
import com.example.data.StudioDatabase
import com.example.data.StudioPreferences
import com.example.data.StudioRepository
import com.example.media.AudioSynthLibrary
import com.example.media.ExportUiState
import com.example.media.MediaInspector
import com.example.media.StudioCatalog
import com.example.media.VideoExportManager
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackItem
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.PipOverlayItem
import com.example.model.ProjectData
import com.example.model.StickerCatalogItem
import com.example.model.StickerOverlayItem
import com.example.model.TemplateItem
import com.example.model.TextAnimationType
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorUiState(
    val project: ProjectData = ProjectData(),
    val selectedClipId: String? = null,
    val selectedTextId: String? = null,
    val selectedStickerId: String? = null,
    val selectedPipId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val activeToolTab: EditorToolTab = EditorToolTab.ROOT,
    val isFullscreenPreview: Boolean = false,
    val isImportingMedia: Boolean = false,
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
                        transitionAfter = if (idx < pickedUris.lastIndex) template.transitionType else TransitionType.NONE
                    )
                }
            } else {
                StudioCatalog.createSampleClipsForProject(appContext, template.previewDrawableName)
                    .mapIndexed { idx, clip ->
                        clip.copy(transitionAfter = if (idx < 2) template.transitionType else TransitionType.NONE)
                    }
            }

            val music = AudioSynthLibrary.ensureTrackFile(appContext, template.audioPresetId)
            val project = ProjectData(
                title = template.title,
                aspectRatio = template.aspectRatio,
                resolution = prefs.defaultResolution,
                fps = prefs.defaultFps,
                clips = clips,
                audioTrack = music,
                filterPreset = template.filterPreset,
                effectPreset = template.effectPreset,
                textOverlays = listOf(
                    TextOverlayItem(
                        text = template.defaultCaption,
                        stylePreset = TextStylePreset.CYBER_NEON,
                        startMs = 0L,
                        endMs = 3500L
                    )
                )
            )
            openFreshProject(project, EditorToolTab.ROOT)
            repository.saveProject(project)
            onReady()
        }
    }

    fun openExistingProject(project: ProjectData, initialTool: EditorToolTab = EditorToolTab.ROOT) {
        openFreshProject(project, initialTool)
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
                _editorState.update { it.copy(statusToast = "Draft saved") }
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
                activeToolTab = if (clipId != null) EditorToolTab.EDIT_CLIP else EditorToolTab.ROOT
            )
        }
    }

    fun selectTextOverlay(id: String?) {
        _editorState.update {
            it.copy(
                selectedTextId = id,
                selectedStickerId = null,
                activeToolTab = if (id != null) EditorToolTab.TEXT else it.activeToolTab
            )
        }
    }

    fun selectStickerOverlay(id: String?) {
        _editorState.update {
            it.copy(
                selectedStickerId = id,
                selectedTextId = null,
                activeToolTab = if (id != null) EditorToolTab.STICKERS else it.activeToolTab
            )
        }
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
            p.copy(clips = p.clips.map { if (it.id == clipId) it.copy(speed = safeSpeed) else it })
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
        pushProjectMutation("Flipped") { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(flipHorizontal = !it.flipHorizontal) else it
                }
            )
        }
    }

    fun updateClipZoom(clipId: String, zoomScale: Float) {
        pushProjectMutation { p ->
            p.copy(
                clips = p.clips.map {
                    if (it.id == clipId) it.copy(zoomScale = zoomScale.coerceIn(0.5f, 2.5f)) else it
                }
            )
        }
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

    // Text & Captions
    fun addTextOverlay(
        text: String,
        preset: TextStylePreset = TextStylePreset.CYBER_NEON,
        animation: TextAnimationType = TextAnimationType.POP
    ) {
        val state = _editorState.value
        val start = state.playheadMs
        val total = state.project.totalDurationMs.coerceAtLeast(3000L)
        val end = (start + 3500L).coerceAtMost(total).coerceAtLeast(start + 800L)
        val newItem = TextOverlayItem(
            text = text.ifBlank { "Tap to edit" },
            stylePreset = preset,
            colorHex = preset.textColorHex,
            bgHex = preset.bgColorHex,
            startMs = start,
            endMs = end,
            animationType = animation
        )
        pushProjectMutation("Added text") { p ->
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

    fun updateEffect(preset: VideoEffectPreset) {
        pushProjectMutation("Effect: ${preset.label}") { p ->
            p.copy(effectPreset = preset)
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

    fun updateCanvasColor(colorHex: Long) {
        pushProjectMutation { p -> p.copy(canvasColorHex = colorHex) }
    }

    fun updateExportSettings(resolution: ExportResolution, fps: Int) {
        pushProjectMutation("Export ${resolution.label} · ${fps}fps") { p ->
            p.copy(resolution = resolution, fps = fps)
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
        autoSaveDrafts: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updatePreferences(defaultResolution, defaultFps, addWatermark, autoSaveDrafts)
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
