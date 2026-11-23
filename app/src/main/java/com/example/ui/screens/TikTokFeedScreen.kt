package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.media.StudioCatalog
import com.example.model.PostCommentDoc
import com.example.model.SocialVideoPostDoc
import com.example.ui.components.AudioWaveformStrip
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import kotlinx.coroutines.delay

data class PendingTikTokPostDraft(
    val caption: String = "",
    val hashtagsRaw: String = "#ssstudio #capcutpro #viral",
    val soundTitle: String = "Original Sound - s.s Studio",
    val mediaType: String = "TEMPLATE_REEL",
    val mediaUrl: String = "",
    val accentHex: Long = 0xFF00E5FFL,
    val filterLabel: String = "A1 Studio",
    val templateId: String = "tpl_alpine_cinema"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTokFeedScreen(
    currentUserId: String,
    posts: List<SocialVideoPostDoc>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followingIds: Set<String>,
    activeComments: List<PostCommentDoc>,
    onToggleLike: (SocialVideoPostDoc) -> Unit,
    onDoubleTapLike: (SocialVideoPostDoc) -> Unit,
    onToggleSave: (SocialVideoPostDoc) -> Unit,
    onRecordShare: (SocialVideoPostDoc) -> Unit,
    onToggleFollow: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onCloseComments: () -> Unit,
    onAddComment: (SocialVideoPostDoc, String) -> Unit,
    onDeletePost: (String) -> Unit,
    onUseTemplateInCapCut: (templateId: String) -> Unit,
    onPublishPost: (
        caption: String,
        hashtagsRaw: String,
        soundTitle: String,
        mediaType: String,
        mediaUrl: String,
        accentHex: Long,
        filterLabel: String,
        templateId: String
    ) -> Unit,
    pendingDraft: PendingTikTokPostDraft? = null,
    onConsumedPendingDraft: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var feedFilter by remember { mutableStateOf("For You") }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var commentSheetPost by remember { mutableStateOf<SocialVideoPostDoc?>(null) }
    var showPublishSheet by remember { mutableStateOf(pendingDraft != null) }
    var activeDraft by remember { mutableStateOf(pendingDraft ?: PendingTikTokPostDraft()) }

    LaunchedEffect(pendingDraft) {
        if (pendingDraft != null) {
            activeDraft = pendingDraft
            showPublishSheet = true
            onConsumedPendingDraft()
        }
    }

    val visiblePosts = remember(posts, feedFilter, followingIds, searchQuery, currentUserId) {
        val tabFiltered = if (feedFilter == "Following") {
            posts.filter { followingIds.contains(it.authorId) || it.authorId == currentUserId }
        } else {
            posts
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) tabFiltered
        else tabFiltered.filter { p ->
            p.caption.contains(q, ignoreCase = true) ||
                p.authorName.contains(q, ignoreCase = true) ||
                p.authorHandle.contains(q, ignoreCase = true) ||
                p.hashtags.any { it.contains(q, ignoreCase = true) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (visiblePosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioBackground)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.MovieCreation, null, tint = ElectricCyan, modifier = Modifier.size(54.dp))
                    Text(
                        text = if (feedFilter == "Following") "Follow creators or post your first reel" else "No reels match your search",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            activeDraft = PendingTikTokPostDraft()
                            showPublishSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create First Post", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { visiblePosts.size })
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = visiblePosts[page]
                val isCurrentPage = pagerState.currentPage == page
                TikTokReelPage(
                    post = post,
                    isCurrentPage = isCurrentPage,
                    isOwnPost = post.authorId == currentUserId,
                    isLiked = likedPostIds.contains(post.id),
                    isSaved = savedPostIds.contains(post.id),
                    isFollowing = followingIds.contains(post.authorId),
                    onDoubleTapLike = { onDoubleTapLike(post) },
                    onToggleLike = { onToggleLike(post) },
                    onOpenComments = {
                        commentSheetPost = post
                        onOpenComments(post.id)
                    },
                    onToggleSave = { onToggleSave(post) },
                    onShare = {
                        onRecordShare(post)
                        val shareText = "${post.caption} — by ${post.authorHandle} on s.s Studio Pro ${post.hashtags.joinToString(" ")}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                    },
                    onToggleFollow = { onToggleFollow(post.authorId) },
                    onDeletePost = { onDeletePost(post.id) },
                    onUseTemplate = {
                        val tid = post.templateId.ifBlank { StudioCatalog.templates.first().id }
                        onUseTemplateInCapCut(tid)
                    }
                )
            }
        }

        // Top TikTok Navigation Overlay (Following | For You | Search | + Post)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .testTag("tiktok_post_button")
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            activeDraft = PendingTikTokPostDraft()
                            showPublishSheet = true
                        },
                    color = NeonRose,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Post Reel", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Post", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Following", "For You").forEach { tab ->
                        val active = feedFilter == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { feedFilter = tab }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium
                                ),
                                color = if (active) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (active) 24.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { searchOpen = !searchOpen },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Search feed",
                        tint = Color.White
                    )
                }
            }

            AnimatedVisibility(visible = searchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search reels, creators, #tags...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.78f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.65f),
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }

    // Live Comments Bottom Sheet
    commentSheetPost?.let { targetPost ->
        var commentText by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = {
                commentSheetPost = null
                onCloseComments()
            },
            containerColor = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 500.dp)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Comments (${activeComments.size.coerceAtLeast(targetPost.commentsCount.toInt())})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (activeComments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Be the first to comment on this reel!", color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeComments, key = { it.id }) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = comment.authorName.take(1).uppercase().ifBlank { "C" },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${comment.authorName} · ${comment.authorHandle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(targetPost, commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send comment", tint = Color(0xFF041418))
                    }
                }
            }
        }
    }

    // Publish Reel / Design Bottom Sheet
    if (showPublishSheet) {
        PublishTikTokPostSheet(
            initialDraft = activeDraft,
            onDismiss = { showPublishSheet = false },
            onPublish = { caption, hashtags, sound, mType, mUrl, accentHex, filterLabel, tplId ->
                onPublishPost(caption, hashtags, sound, mType, mUrl, accentHex, filterLabel, tplId)
                showPublishSheet = false
            }
        )
    }
}

@Composable
private fun TikTokReelPage(
    post: SocialVideoPostDoc,
    isCurrentPage: Boolean,
    isOwnPost: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowing: Boolean,
    onDoubleTapLike: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onToggleFollow: () -> Unit,
    onDeletePost: () -> Unit,
    onUseTemplate: () -> Unit
) {
    val context = LocalContext.current
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(showDoubleTapHeart) {
        if (showDoubleTapHeart) {
            delay(700L)
            showDoubleTapHeart = false
        }
    }

    val isVideo = post.mediaType == "VIDEO" && post.mediaUrl.isNotBlank()
    val isPlayableImage = (post.mediaType == "PHOTO" || post.mediaType == "CANVA_DESIGN") && post.mediaUrl.isNotBlank()

    val exoPlayer = remember(post.id, post.mediaUrl, isVideo) {
        if (isVideo) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(post.mediaUrl)))
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
            }
        } else null
    }

    LaunchedEffect(isCurrentPage, isPaused, exoPlayer) {
        exoPlayer?.let { player ->
            if (isCurrentPage && !isPaused) player.play() else player.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "reel_anim")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "disc"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glow"
    )

    val accentColor = Color(post.accentHex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06080E))
            .pointerInput(post.id) {
                detectTapGestures(
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        onDoubleTapLike()
                    },
                    onTap = {
                        if (isVideo) isPaused = !isPaused
                    }
                )
            }
    ) {
        // Background / Media Renderer
        when {
            exoPlayer != null -> {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            isPlayableImage -> {
                AsyncImage(
                    model = Uri.parse(post.mediaUrl),
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Studio Kinetic Reel Showcase
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    Color(0xFF090D16),
                                    Color(0xFF1A091B)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .scale(glowPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 36.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${post.mediaType.replace("_", " ")} · ${post.filterLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = post.caption,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        AudioWaveformStrip(
                            waveform = listOf(0.3f, 0.8f, 0.55f, 0.95f, 0.4f, 0.75f, 0.9f, 0.5f, 0.7f, 0.35f),
                            barColor = accentColor,
                            modifier = Modifier
                                .width(150.dp)
                                .height(26.dp)
                        )
                    }
                }
            }
        }

        // Bottom readability scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        if (isPaused && isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Paused",
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
            )
        }

        // Double-Tap Heart Animation
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn(initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 1.4f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = NeonRose,
                modifier = Modifier.size(110.dp)
            )
        }

        // Right-Side TikTok Interaction Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Creator Avatar + Follow "+"
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceElevated)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = post.authorName.take(1).uppercase().ifBlank { "S" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricCyan
                        )
                    }
                }
                if (!isOwnPost && post.authorId.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isFollowing) ElectricCyan else NeonRose)
                            .clickable(onClick = onToggleFollow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = if (isFollowing) "Following" else "Follow",
                            tint = if (isFollowing) Color(0xFF041418) else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            TikTokActionItem(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(post.likesCount),
                tint = if (isLiked) NeonRose else Color.White,
                testTag = "tiktok_like_${post.id}",
                onClick = onToggleLike
            )
            TikTokActionItem(
                icon = Icons.Filled.ChatBubble,
                label = formatCount(post.commentsCount),
                tint = Color.White,
                testTag = "tiktok_comment_${post.id}",
                onClick = onOpenComments
            )
            TikTokActionItem(
                icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = formatCount(post.savesCount),
                tint = if (isSaved) StudioAmber else Color.White,
                testTag = "tiktok_save_${post.id}",
                onClick = onToggleSave
            )
            TikTokActionItem(
                icon = Icons.Filled.Share,
                label = formatCount(post.sharesCount),
                tint = Color.White,
                testTag = "tiktok_share_${post.id}",
                onClick = onShare
            )

            // Spinning Sound Disc
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(if (isCurrentPage && !isPaused) discRotation else 0f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accentColor, Color(0xFF111827))))
                    .border(3.dp, Color(0xFF1F2937), CircleShape)
                    .clickable(onClick = onUseTemplate),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, "Sound disc", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Left Caption, Hashtags, Sound & CapCut Template Remix Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = post.authorHandle.ifBlank { "@creator" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                if (isOwnPost) {
                    IconButton(
                        onClick = onDeletePost,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, "Delete post", tint = NeonRose, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (post.hashtags.isNotEmpty()) {
                Text(
                    text = post.hashtags.joinToString(" "),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text(
                    text = post.soundTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // CapCut Pro Remix / Use Template CTA
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onUseTemplate),
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.MovieCreation, null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                    Text(
                        text = "Use Template in CapCut Pro",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TikTokActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishTikTokPostSheet(
    initialDraft: PendingTikTokPostDraft,
    onDismiss: () -> Unit,
    onPublish: (
        caption: String,
        hashtagsRaw: String,
        soundTitle: String,
        mediaType: String,
        mediaUrl: String,
        accentHex: Long,
        filterLabel: String,
        templateId: String
    ) -> Unit
) {
    val context = LocalContext.current
    var caption by remember(initialDraft) { mutableStateOf(initialDraft.caption) }
    var hashtags by remember(initialDraft) { mutableStateOf(initialDraft.hashtagsRaw) }
    var soundTitle by remember(initialDraft) { mutableStateOf(initialDraft.soundTitle) }
    var mediaType by remember(initialDraft) { mutableStateOf(initialDraft.mediaType) }
    var mediaUrl by remember(initialDraft) { mutableStateOf(initialDraft.mediaUrl) }
    var accentHex by remember(initialDraft) { mutableStateOf(initialDraft.accentHex) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            mediaType = if (mime.startsWith("video/")) "VIDEO" else "PHOTO"
            mediaUrl = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Post to s.s TikTok Feed",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            OutlinedButton(
                onClick = {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mediaUrl.isBlank()) "Pick Video or Photo from Gallery" else "Media attached ($mediaType) — Change",
                    color = TextPrimary
                )
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                placeholder = { Text("Describe your CapCut edit or Canva poster...") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text("Hashtags") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = soundTitle,
                onValueChange = { soundTitle = it },
                label = { Text("Sound / Music Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0xFF00E5FFL, 0xFFFF3B6BL, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFF10B981L)) { hex ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (accentHex == hex) 2.5.dp else 1.dp,
                                color = if (accentHex == hex) Color.White else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { accentHex = hex }
                    )
                }
            }

            Button(
                onClick = {
                    onPublish(
                        caption.ifBlank { "Fresh edit created with s.s Studio Pro 🔥" },
                        hashtags,
                        soundTitle,
                        mediaType,
                        mediaUrl,
                        accentHex,
                        initialDraft.filterLabel,
                        initialDraft.templateId
                    )
                },
                modifier = Modifier
                    .testTag("confirm_publish_tiktok_button")
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRose, contentColor = Color.White)
            ) {
                Text("Publish Now", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

private fun formatCount(value: Long): String {
    return when {
        value >= 1_000_000L -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000L -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
