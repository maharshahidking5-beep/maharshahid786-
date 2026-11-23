package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.OperationType
import com.example.auth.handleFirestoreError
import com.example.model.CanvaDesignDoc
import com.example.model.CanvaProject
import com.example.model.CloudProjectSummaryDoc
import com.example.model.PostCommentDoc
import com.example.model.ProjectData
import com.example.model.SocialVideoPostDoc
import com.example.model.UserProfileDoc
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class CloudSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // ==========================================
    // USER PROFILE & PRIVATE GMAIL PII SYNC
    // ==========================================

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        if (uid.isBlank()) return Result.failure(IllegalArgumentException("Blank UID"))
        val profilePath = "users/$uid"
        return try {
            val profileRef = db.collection("users").document(uid)
            val snap = profileRef.get().await()
            val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Creator").take(60)
            val rawHandle = "@" + (user.email?.substringBefore("@") ?: safeName.lowercase())
                .replace(Regex("[^a-zA-Z0-9_]"), "")
                .ifBlank { "creator" }
                .take(28)
            val photoUrl = user.photoUrl?.toString().orEmpty().take(500)

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "handle" to rawHandle,
                    "bio" to "Creating reels & posters on s.s Studio Pro",
                    "photoUrl" to photoUrl,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "likesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.set(payload).await()
            }

            // Store private Gmail email isolated in /users/{uid}/private/account
            val email = user.email?.trim().orEmpty()
            if (email.isNotBlank()) {
                val privateRef = profileRef.collection("private").document("account")
                val privSnap = privateRef.get().await()
                if (!privSnap.exists()) {
                    privateRef.set(
                        mapOf(
                            "userId" to uid,
                            "email" to email.take(160),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    privateRef.update(
                        mapOf(
                            "email" to email.take(160),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfileDoc?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observePrivateEmail(userId: String): Flow<String> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/private/account"
        emitAll(
            db.collection("users").document(userId).collection("private").document("account")
                .snapshots()
                .map { it.getString("email").orEmpty() }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun updateUserProfile(
        displayName: String,
        handle: String,
        bio: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = displayName.trim().take(60)
        val cleanHandle = ("@" + handle.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_]"), "")).take(32)
        val cleanBio = bio.trim().take(200)
        if (cleanName.isEmpty() || cleanHandle.length < 2) {
            return Result.failure(IllegalArgumentException("Name and @handle are required."))
        }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "bio" to cleanBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // TIKTOK PUBLIC SHORT-VIDEO & CANVA FEED
    // ==========================================

    fun observePublicFeedPosts(): Flow<List<SocialVideoPostDoc>> = flow {
        requireUserId()
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialVideoPostDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserPosts(userId: String): Flow<List<SocialVideoPostDoc>> = flow {
        requireUserId()
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("authorId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialVideoPostDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun seedStarterFeedIfEmpty(userProfile: UserProfileDoc): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val existing = db.collection("posts")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return Result.success(Unit)

            val starterPosts = listOf(
                mapOf(
                    "authorId" to uid,
                    "authorName" to userProfile.displayName.ifBlank { "s.s Studio Official" },
                    "authorHandle" to userProfile.handle.ifBlank { "@ss_studio" },
                    "authorAvatarUrl" to userProfile.photoUrl,
                    "caption" to "Cinematic Neon Beat Edit made in CapCut Pro Mode · 1080p+ 60FPS 🔥",
                    "hashtags" to listOf("#ssstudio", "#capcutpro", "#1080pplus", "#viral"),
                    "soundTitle" to "Cyber Neon Pulse - s.s Audio Lab",
                    "mediaType" to "TEMPLATE_REEL",
                    "mediaUrl" to "",
                    "accentHex" to 0xFF00E5FFL,
                    "filterLabel" to "A1 Studio",
                    "templateId" to "tpl_neon_pulse",
                    "visibility" to "PUBLIC",
                    "likesCount" to 128L,
                    "commentsCount" to 1L,
                    "sharesCount" to 14L,
                    "savesCount" to 32L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "authorId" to uid,
                    "authorName" to userProfile.displayName.ifBlank { "s.s Studio Official" },
                    "authorHandle" to userProfile.handle.ifBlank { "@ss_studio" },
                    "authorAvatarUrl" to userProfile.photoUrl,
                    "caption" to "Canva Pro Thumbnail + Poster Workflow ✨ Tap 'Use Template' to remix!",
                    "hashtags" to listOf("#canvapro", "#design", "#poster", "#reels"),
                    "soundTitle" to "Sunset Lo-Fi Groove",
                    "mediaType" to "CANVA_DESIGN",
                    "mediaUrl" to "",
                    "accentHex" to 0xFFFF3B6BL,
                    "filterLabel" to "A1 Ultra Glow",
                    "templateId" to "tpl_vlog_sunset",
                    "visibility" to "PUBLIC",
                    "likesCount" to 94L,
                    "commentsCount" to 0L,
                    "sharesCount" to 9L,
                    "savesCount" to 21L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "authorId" to uid,
                    "authorName" to userProfile.displayName.ifBlank { "s.s Studio Official" },
                    "authorHandle" to userProfile.handle.ifBlank { "@ss_studio" },
                    "authorAvatarUrl" to userProfile.photoUrl,
                    "caption" to "Smooth Zoom On + AI Translator Captions (Urdu / English) 🌍",
                    "hashtags" to listOf("#zoomon", "#ailab", "#captions", "#trending"),
                    "soundTitle" to "Phonk Drift 1080p+",
                    "mediaType" to "TEMPLATE_REEL",
                    "mediaUrl" to "",
                    "accentHex" to 0xFF8B5CF6L,
                    "filterLabel" to "A1 Velvet",
                    "templateId" to "tpl_velocity_beat",
                    "visibility" to "PUBLIC",
                    "likesCount" to 215L,
                    "commentsCount" to 0L,
                    "sharesCount" to 27L,
                    "savesCount" to 54L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            starterPosts.forEach { payload ->
                db.collection("posts").document(UUID.randomUUID().toString().replace("-", "_")).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts")
            Result.failure(e)
        }
    }

    suspend fun publishPost(
        profile: UserProfileDoc,
        caption: String,
        hashtags: List<String>,
        soundTitle: String,
        mediaType: String,
        mediaUrl: String,
        accentHex: Long,
        filterLabel: String,
        templateId: String = ""
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanCaption = caption.trim().ifBlank { "New edit on s.s Studio" }.take(300)
        val cleanTags = hashtags.map { it.trim().take(32) }.filter { it.isNotBlank() }.take(8)
        val postId = "post_${UUID.randomUUID().toString().replace("-", "_")}"
        val path = "posts/$postId"
        return try {
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to profile.displayName.ifBlank { "Creator" }.take(60),
                "authorHandle" to profile.handle.ifBlank { "@creator" }.take(32),
                "authorAvatarUrl" to profile.photoUrl.take(500),
                "caption" to cleanCaption,
                "hashtags" to cleanTags,
                "soundTitle" to soundTitle.trim().ifBlank { "Original Sound" }.take(80),
                "mediaType" to mediaType,
                "mediaUrl" to mediaUrl.take(1000),
                "accentHex" to accentHex,
                "filterLabel" to filterLabel.take(40),
                "templateId" to templateId.take(64),
                "visibility" to "PUBLIC",
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "savesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).set(payload).await()
            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        requireUserId()
        val path = "posts/$postId"
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // LIKES, SAVES, SHARES, FOLLOWS & COMMENTS
    // ==========================================

    fun observeLikedPostIds(userId: String): Flow<Set<String>> =
        observeUserInteractionSet(userId, "likedPosts")

    fun observeSavedPostIds(userId: String): Flow<Set<String>> =
        observeUserInteractionSet(userId, "savedPosts")

    fun observeFollowingIds(userId: String): Flow<Set<String>> =
        observeUserInteractionSet(userId, "following")

    private fun observeUserInteractionSet(userId: String, subcollection: String): Flow<Set<String>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/$subcollection"
        emitAll(
            db.collection("users").document(userId).collection(subcollection)
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun toggleLikePost(post: SocialVideoPostDoc, currentlyLiked: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likeRef = db.collection("users").document(uid).collection("likedPosts").document(post.id)
        val postRef = db.collection("posts").document(post.id)
        return try {
            val delta = if (currentlyLiked) -1L else 1L
            val nextLikes = (post.likesCount + delta).coerceAtLeast(0L)
            if (currentlyLiked) {
                likeRef.delete().await()
            } else {
                likeRef.set(
                    mapOf("targetId" to post.id, "createdAt" to FieldValue.serverTimestamp())
                ).await()
            }
            postRef.update(
                mapOf("likesCount" to nextLikes, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (post.authorId.isNotBlank()) {
                val authorRef = db.collection("users").document(post.authorId)
                val authorSnap = authorRef.get().await()
                if (authorSnap.exists()) {
                    val curAuthorLikes = authorSnap.getLong("likesCount") ?: 0L
                    authorRef.update(
                        mapOf(
                            "likesCount" to (curAuthorLikes + delta).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(post: SocialVideoPostDoc, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val saveRef = db.collection("users").document(uid).collection("savedPosts").document(post.id)
        val postRef = db.collection("posts").document(post.id)
        return try {
            val delta = if (currentlySaved) -1L else 1L
            val nextSaves = (post.savesCount + delta).coerceAtLeast(0L)
            if (currentlySaved) {
                saveRef.delete().await()
            } else {
                saveRef.set(
                    mapOf("targetId" to post.id, "createdAt" to FieldValue.serverTimestamp())
                ).await()
            }
            postRef.update(
                mapOf("savesCount" to nextSaves, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun recordSharePost(post: SocialVideoPostDoc): Result<Unit> {
        requireUserId()
        return try {
            db.collection("posts").document(post.id).update(
                mapOf("sharesCount" to (post.sharesCount + 1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleFollowCreator(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == targetUserId || targetUserId.isBlank()) return Result.success(Unit)
        val followRef = db.collection("users").document(uid).collection("following").document(targetUserId)
        val myProfileRef = db.collection("users").document(uid)
        val targetProfileRef = db.collection("users").document(targetUserId)
        return try {
            val delta = if (currentlyFollowing) -1L else 1L
            if (currentlyFollowing) {
                followRef.delete().await()
            } else {
                followRef.set(
                    mapOf("targetId" to targetUserId, "createdAt" to FieldValue.serverTimestamp())
                ).await()
            }
            val mySnap = myProfileRef.get().await()
            if (mySnap.exists()) {
                val curFollowing = mySnap.getLong("followingCount") ?: 0L
                myProfileRef.update(
                    mapOf(
                        "followingCount" to (curFollowing + delta).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val targetSnap = targetProfileRef.get().await()
            if (targetSnap.exists()) {
                val curFollowers = targetSnap.getLong("followersCount") ?: 0L
                targetProfileRef.update(
                    mapOf(
                        "followersCount" to (curFollowers + delta).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/following/$targetUserId")
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<PostCommentDoc>> = flow {
        requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostCommentDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addComment(post: SocialVideoPostDoc, profile: UserProfileDoc, text: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(300)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty"))
        val commentId = "c_${UUID.randomUUID().toString().replace("-", "_")}"
        val path = "posts/${post.id}/comments/$commentId"
        return try {
            db.collection("posts").document(post.id).collection("comments").document(commentId).set(
                mapOf(
                    "postId" to post.id,
                    "authorId" to uid,
                    "authorName" to profile.displayName.ifBlank { "Creator" }.take(60),
                    "authorHandle" to profile.handle.ifBlank { "@creator" }.take(32),
                    "text" to cleanText,
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("posts").document(post.id).update(
                mapOf(
                    "commentsCount" to (post.commentsCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // CLOUD SYNC: CAPCUT PROJECTS & CANVA DESIGNS
    // ==========================================

    fun observeCloudProjects(userId: String): Flow<List<CloudProjectSummaryDoc>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/cloudProjects"
        emitAll(
            db.collection("users").document(userId).collection("cloudProjects")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudProjectSummaryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun syncProjectToCloud(project: ProjectData): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeId = project.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "users/$uid/cloudProjects/$safeId"
        return try {
            val encoded = json.encodeToString(project).take(290_000)
            val docRef = db.collection("users").document(uid).collection("cloudProjects").document(safeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to project.title.ifBlank { "Untitled Edit" }.take(100),
                        "aspectLabel" to project.aspectRatio.label.take(24),
                        "resolutionLabel" to project.resolution.label.take(32),
                        "clipCount" to project.clips.size.toLong().coerceIn(0L, 100L),
                        "totalDurationMs" to project.totalDurationMs.coerceAtLeast(0L),
                        "filterLabel" to project.filterPreset.label.take(40),
                        "projectJson" to encoded,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to project.title.ifBlank { "Untitled Edit" }.take(100),
                        "aspectLabel" to project.aspectRatio.label.take(24),
                        "resolutionLabel" to project.resolution.label.take(32),
                        "clipCount" to project.clips.size.toLong().coerceIn(0L, 100L),
                        "totalDurationMs" to project.totalDurationMs.coerceAtLeast(0L),
                        "filterLabel" to project.filterPreset.label.take(40),
                        "projectJson" to encoded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed syncing CapCut project", e)
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeCanvaDesigns(userId: String): Flow<List<CanvaDesignDoc>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/designs"
        emitAll(
            db.collection("users").document(userId).collection("designs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CanvaDesignDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveCanvaDesign(design: CanvaProject): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeId = design.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "users/$uid/designs/$safeId"
        return try {
            val encoded = json.encodeToString(design).take(240_000)
            val headline = design.elements.firstOrNull { it.type.name == "TEXT" }?.text?.take(160).orEmpty()
            val accent = design.elements.firstOrNull()?.colorHex ?: 0xFF00E5FFL
            val docRef = db.collection("users").document(uid).collection("designs").document(safeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to design.title.ifBlank { "Untitled Design" }.take(100),
                        "category" to design.category.ifBlank { "Poster" }.take(40),
                        "aspectLabel" to design.aspectOption.label.take(24),
                        "bgHex" to design.bgColorHex,
                        "accentHex" to accent,
                        "headline" to headline,
                        "designJson" to encoded,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to design.title.ifBlank { "Untitled Design" }.take(100),
                        "category" to design.category.ifBlank { "Poster" }.take(40),
                        "aspectLabel" to design.aspectOption.label.take(24),
                        "bgHex" to design.bgColorHex,
                        "accentHex" to accent,
                        "headline" to headline,
                        "designJson" to encoded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCanvaDesign(designId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeId = designId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "users/$uid/designs/$safeId"
        return try {
            db.collection("users").document(uid).collection("designs").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
