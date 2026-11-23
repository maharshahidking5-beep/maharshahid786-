package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudSocialRepository
import com.example.data.CloudSyncStatus
import com.example.data.CloudSyncUiState
import com.example.data.NetworkAndPowerObserver
import com.example.model.CanvaDesignDoc
import com.example.model.CanvaProject
import com.example.model.CloudProjectSummaryDoc
import com.example.model.PostCommentDoc
import com.example.model.ProjectData
import com.example.model.SocialVideoPostDoc
import com.example.model.UserProfileDoc
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val profile: UserProfileDoc) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SocialAndCanvaViewModel(
    private val repo: CloudSocialRepository,
    val currentUserId: String,
    private val appContext: Context? = null
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow<UserProfileDoc?>(null)
    val profile: StateFlow<UserProfileDoc?> = _profile.asStateFlow()

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _privateEmail = MutableStateFlow("")
    val privateEmail: StateFlow<String> = _privateEmail.asStateFlow()

    private val _feedPosts = MutableStateFlow<List<SocialVideoPostDoc>>(emptyList())
    val feedPosts: StateFlow<List<SocialVideoPostDoc>> = _feedPosts.asStateFlow()

    private val _myPosts = MutableStateFlow<List<SocialVideoPostDoc>>(emptyList())
    val myPosts: StateFlow<List<SocialVideoPostDoc>> = _myPosts.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: StateFlow<Set<String>> = _followingIds.asStateFlow()

    private val _canvaDesigns = MutableStateFlow<List<CanvaDesignDoc>>(emptyList())
    val canvaDesigns: StateFlow<List<CanvaDesignDoc>> = _canvaDesigns.asStateFlow()

    private val _cloudProjects = MutableStateFlow<List<CloudProjectSummaryDoc>>(emptyList())
    val cloudProjects: StateFlow<List<CloudProjectSummaryDoc>> = _cloudProjects.asStateFlow()

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeComments: StateFlow<List<PostCommentDoc>> = _activeCommentPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repo.observeComments(postId).catch { error ->
                    unwrapStreamError(error)
                    emit(emptyList())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _cloudSyncState = MutableStateFlow(
        CloudSyncUiState(
            isOnline = appContext?.let { NetworkAndPowerObserver.isCurrentlyOnline(it) } ?: true
        )
    )
    val cloudSyncState: StateFlow<CloudSyncUiState> = _cloudSyncState.asStateFlow()

    private val pendingOfflineDrafts = linkedMapOf<String, ProjectData>()
    private var latestLocalProjects: List<ProjectData> = emptyList()
    private var hasAttemptedSeed = false

    init {
        startCollectors()
        observeConnectivityAndSync()
    }

    private fun unwrapStreamError(error: Throwable, reportToast: Boolean = false) {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError == null && error is CancellationException) throw error
        if (reportToast) {
            _statusMessage.value = fsError?.localizedMessage ?: error.localizedMessage ?: "Cloud sync interrupted"
        }
    }

    private fun startCollectors() {
        viewModelScope.launch {
            repo.observeUserProfile(currentUserId)
                .catch { error ->
                    unwrapStreamError(error)
                    _profileUiState.value = ProfileUiState.Error(
                        error.localizedMessage ?: "Could not load creator profile."
                    )
                }
                .collect { doc ->
                    if (doc != null) {
                        _profile.value = doc
                        _profileUiState.value = ProfileUiState.Ready(doc)
                        if (!hasAttemptedSeed) {
                            hasAttemptedSeed = true
                            repo.seedStarterFeedIfEmpty(doc)
                        }
                    }
                }
        }

        viewModelScope.launch {
            repo.observePrivateEmail(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { email ->
                    if (email.isNotBlank()) _privateEmail.value = email
                }
        }

        viewModelScope.launch {
            repo.observePublicFeedPosts()
                .catch { unwrapStreamError(it) }
                .collect { _feedPosts.value = it }
        }

        viewModelScope.launch {
            repo.observeUserPosts(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { _myPosts.value = it }
        }

        viewModelScope.launch {
            repo.observeLikedPostIds(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { _likedPostIds.value = it }
        }

        viewModelScope.launch {
            repo.observeSavedPostIds(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { _savedPostIds.value = it }
        }

        viewModelScope.launch {
            repo.observeFollowingIds(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { _followingIds.value = it }
        }

        viewModelScope.launch {
            repo.observeCanvaDesigns(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { _canvaDesigns.value = it }
        }

        viewModelScope.launch {
            repo.observeCloudProjects(currentUserId)
                .catch { unwrapStreamError(it) }
                .collect { docs ->
                    _cloudProjects.value = docs
                    _cloudSyncState.update { state ->
                        val nextStatus = if (!state.isOnline) {
                            CloudSyncStatus.OFFLINE_QUEUED
                        } else if (state.status == CloudSyncStatus.SYNCING) {
                            CloudSyncStatus.SYNCING
                        } else if (pendingOfflineDrafts.isEmpty() && docs.isNotEmpty()) {
                            CloudSyncStatus.SYNCED
                        } else state.status
                        state.copy(
                            syncedCloudCount = docs.size,
                            status = nextStatus
                        )
                    }
                }
        }
    }

    private fun observeConnectivityAndSync() {
        val ctx = appContext ?: return
        viewModelScope.launch {
            NetworkAndPowerObserver.observeIsOnline(ctx).collect { online ->
                _cloudSyncState.update { state ->
                    state.copy(
                        isOnline = online,
                        status = when {
                            !online -> CloudSyncStatus.OFFLINE_QUEUED
                            pendingOfflineDrafts.isEmpty() && state.status == CloudSyncStatus.OFFLINE_QUEUED -> CloudSyncStatus.SYNCED
                            else -> state.status
                        }
                    )
                }
                if (online && _cloudSyncState.value.autoSyncEnabled && pendingOfflineDrafts.isNotEmpty()) {
                    flushPendingOfflineDrafts()
                }
            }
        }
    }

    fun updateSyncMetadataFromLocal(
        localProjects: List<ProjectData>,
        autoSyncEnabled: Boolean,
        lastCloudSyncMs: Long
    ) {
        latestLocalProjects = localProjects
        _cloudSyncState.update { cur ->
            val syncedIds = _cloudProjects.value.map { it.id }.toSet()
            val unsyncedCount = localProjects.count { p ->
                val safeId = p.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
                !syncedIds.contains(safeId) && !syncedIds.contains(p.id)
            } + pendingOfflineDrafts.size
            cur.copy(
                localDraftCount = localProjects.size,
                pendingCount = unsyncedCount.coerceAtLeast(pendingOfflineDrafts.size),
                autoSyncEnabled = autoSyncEnabled,
                lastSyncedMs = maxOf(cur.lastSyncedMs, lastCloudSyncMs)
            )
        }
    }

    private suspend fun flushPendingOfflineDrafts() {
        val toFlush = pendingOfflineDrafts.values.toList()
        if (toFlush.isEmpty()) return
        _cloudSyncState.update { it.copy(status = CloudSyncStatus.SYNCING, errorMessage = null) }
        var failed = false
        toFlush.forEach { project ->
            val res = repo.syncProjectToCloud(project)
            if (res.isSuccess) {
                pendingOfflineDrafts.remove(project.id)
            } else {
                failed = true
            }
        }
        val now = System.currentTimeMillis()
        _cloudSyncState.update {
            it.copy(
                status = if (failed) CloudSyncStatus.ERROR else CloudSyncStatus.SYNCED,
                pendingCount = pendingOfflineDrafts.size,
                lastSyncedMs = if (!failed) now else it.lastSyncedMs,
                errorMessage = if (failed) "Some drafts queued for retry" else null
            )
        }
        if (!failed) {
            _statusMessage.value = "Offline edits auto-synced to Cloud"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun ensureProfileAndSeed(user: FirebaseUser) {
        if (user.email?.isNotBlank() == true && _privateEmail.value.isBlank()) {
            _privateEmail.value = user.email.orEmpty()
        }
        viewModelScope.launch {
            val res = repo.ensureUserProfile(user)
            if (res.isFailure) {
                val msg = res.exceptionOrNull()?.localizedMessage ?: "Failed syncing Google profile"
                _profileUiState.value = ProfileUiState.Error(msg)
                _statusMessage.value = msg
            }
        }
    }

    fun updateProfile(displayName: String, handle: String, bio: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.updateUserProfile(displayName, handle, bio)
            _isBusy.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Creator profile updated"
                onDone()
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Profile update failed"
            }
        }
    }

    fun publishPost(
        caption: String,
        hashtagsRaw: String,
        soundTitle: String,
        mediaType: String,
        mediaUrl: String,
        accentHex: Long,
        filterLabel: String,
        templateId: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val curProfile = _profile.value ?: UserProfileDoc(userId = currentUserId)
        val tags = hashtagsRaw
            .split(" ", ",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#")) it else "#$it" }
            .distinct()
            .take(8)
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.publishPost(
                profile = curProfile,
                caption = caption,
                hashtags = tags,
                soundTitle = soundTitle,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                accentHex = accentHex,
                filterLabel = filterLabel,
                templateId = templateId
            )
            _isBusy.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Posted to TikTok Feed!"
                onSuccess()
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed publishing post"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val res = repo.deletePost(postId)
            _statusMessage.value = if (res.isSuccess) "Post deleted" else "Could not delete post"
        }
    }

    fun toggleLike(post: SocialVideoPostDoc) {
        val currentlyLiked = _likedPostIds.value.contains(post.id)
        viewModelScope.launch {
            repo.toggleLikePost(post, currentlyLiked)
        }
    }

    fun ensureLikedOnDoubleTap(post: SocialVideoPostDoc) {
        if (!_likedPostIds.value.contains(post.id)) {
            viewModelScope.launch {
                repo.toggleLikePost(post, currentlyLiked = false)
            }
        }
    }

    fun toggleSave(post: SocialVideoPostDoc) {
        val currentlySaved = _savedPostIds.value.contains(post.id)
        viewModelScope.launch {
            val res = repo.toggleSavePost(post, currentlySaved)
            if (res.isSuccess) {
                _statusMessage.value = if (currentlySaved) "Removed from Saved" else "Saved to Profile"
            }
        }
    }

    fun recordShare(post: SocialVideoPostDoc) {
        viewModelScope.launch {
            repo.recordSharePost(post)
        }
    }

    fun toggleFollow(targetUserId: String) {
        val currentlyFollowing = _followingIds.value.contains(targetUserId)
        viewModelScope.launch {
            repo.toggleFollowCreator(targetUserId, currentlyFollowing)
        }
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

    fun addComment(post: SocialVideoPostDoc, text: String, onSent: () -> Unit = {}) {
        val curProfile = _profile.value ?: UserProfileDoc(userId = currentUserId)
        viewModelScope.launch {
            val res = repo.addComment(post, curProfile, text)
            if (res.isSuccess) {
                onSent()
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not post comment"
            }
        }
    }

    fun saveCanvaDesign(design: CanvaProject, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.saveCanvaDesign(design)
            _isBusy.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Canva Pro design saved to Cloud"
                onSaved()
            } else {
                _statusMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Save failed"
            }
        }
    }

    fun deleteCanvaDesign(designId: String) {
        viewModelScope.launch {
            val res = repo.deleteCanvaDesign(designId)
            _statusMessage.value = if (res.isSuccess) "Design deleted" else "Delete failed"
        }
    }

    fun decodeCanvaDesign(designJson: String): CanvaProject? {
        return runCatching { json.decodeFromString<CanvaProject>(designJson) }.getOrNull()
    }

    fun syncCapCutProject(project: ProjectData) {
        if (project.clips.isEmpty()) return
        val online = appContext?.let { NetworkAndPowerObserver.isCurrentlyOnline(it) } ?: true
        if (!online) {
            pendingOfflineDrafts[project.id] = project
            _cloudSyncState.update {
                it.copy(
                    isOnline = false,
                    status = CloudSyncStatus.OFFLINE_QUEUED,
                    pendingCount = pendingOfflineDrafts.size
                )
            }
            return
        }
        viewModelScope.launch {
            _cloudSyncState.update { it.copy(status = CloudSyncStatus.SYNCING, errorMessage = null) }
            val res = repo.syncProjectToCloud(project)
            if (res.isSuccess) {
                pendingOfflineDrafts.remove(project.id)
                val now = System.currentTimeMillis()
                _cloudSyncState.update {
                    it.copy(
                        status = CloudSyncStatus.SYNCED,
                        pendingCount = pendingOfflineDrafts.size,
                        lastSyncedMs = now,
                        errorMessage = null
                    )
                }
            } else {
                pendingOfflineDrafts[project.id] = project
                _cloudSyncState.update {
                    it.copy(
                        status = CloudSyncStatus.ERROR,
                        pendingCount = pendingOfflineDrafts.size,
                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Sync queued locally"
                    )
                }
            }
        }
    }

    fun syncAllProjectsNow(
        localProjects: List<ProjectData> = latestLocalProjects,
        onSyncedTimestamp: (Long) -> Unit = {}
    ) {
        val online = appContext?.let { NetworkAndPowerObserver.isCurrentlyOnline(it) } ?: true
        if (!online) {
            localProjects.forEach { if (it.clips.isNotEmpty()) pendingOfflineDrafts[it.id] = it }
            _cloudSyncState.update {
                it.copy(
                    isOnline = false,
                    status = CloudSyncStatus.OFFLINE_QUEUED,
                    pendingCount = pendingOfflineDrafts.size,
                    errorMessage = "Device offline — drafts saved locally and queued for Cloud Sync"
                )
            }
            _statusMessage.value = "Offline — queued ${pendingOfflineDrafts.size} draft(s) for auto-sync"
            return
        }
        val combined = (localProjects + pendingOfflineDrafts.values)
            .filter { it.clips.isNotEmpty() }
            .associateBy { it.id }
            .values
            .toList()
        if (combined.isEmpty()) {
            val now = System.currentTimeMillis()
            _cloudSyncState.update {
                it.copy(status = CloudSyncStatus.SYNCED, lastSyncedMs = now, errorMessage = null)
            }
            onSyncedTimestamp(now)
            _statusMessage.value = "Cloud Sync up to date"
            return
        }
        viewModelScope.launch {
            _cloudSyncState.update { it.copy(status = CloudSyncStatus.SYNCING, errorMessage = null) }
            var firstError: String? = null
            combined.forEach { project ->
                val res = repo.syncProjectToCloud(project)
                if (res.isSuccess) {
                    pendingOfflineDrafts.remove(project.id)
                } else if (firstError == null) {
                    firstError = res.exceptionOrNull()?.localizedMessage ?: "Cloud sync failed"
                }
            }
            val now = System.currentTimeMillis()
            if (firstError == null) {
                onSyncedTimestamp(now)
                _cloudSyncState.update {
                    it.copy(
                        status = CloudSyncStatus.SYNCED,
                        pendingCount = 0,
                        lastSyncedMs = now,
                        errorMessage = null
                    )
                }
                _statusMessage.value = "Synced ${combined.size} project(s) to Firebase Cloud"
            } else {
                _cloudSyncState.update {
                    it.copy(
                        status = CloudSyncStatus.ERROR,
                        pendingCount = pendingOfflineDrafts.size,
                        errorMessage = firstError
                    )
                }
                _statusMessage.value = firstError
            }
        }
    }
}
