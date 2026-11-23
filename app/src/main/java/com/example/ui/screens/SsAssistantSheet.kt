package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AssistantChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SsAssistantBottomSheet(
    messages: List<AssistantChatMessage>,
    isStreaming: Boolean,
    errorMessage: String?,
    linkedPhone: String,
    onAskQuestion: (String) -> Unit,
    onClearChat: () -> Unit,
    onSavePhone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var voiceFeedback by remember { mutableStateOf<String?>(null) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var pendingCallLabel by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                voiceFeedback = null
                onAskQuestion(spoken)
            }
        }
    }

    val startVoiceCapture: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask SS Assistant...")
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure { voiceFeedback = "Voice speech input is unavailable on this device." }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            voiceFeedback = "Microphone permission is needed for voice questions."
        }
    }

    val quickPrompts = remember {
        listOf(
            "How to remove video BG?",
            "Fix shaky clips",
            "Why is 4K export slow?",
            "Monetization rules",
            "Urdu Nastaliq text tips"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = ElectricCyan)
                    Column {
                        Text("SS Assistant", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            text = if (linkedPhone.isBlank()) "Voice & text helper · Link phone for calls"
                            else "Calling enabled: $linkedPhone",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (linkedPhone.isBlank()) TextSecondary else StudioEmerald
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (linkedPhone.isBlank()) {
                                pendingCallLabel = "Audio Call"
                                showPhoneDialog = true
                            } else {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$linkedPhone")))
                                }
                            }
                        },
                        modifier = Modifier.testTag("assistant_audio_call_btn").minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Call, "Audio call", tint = StudioEmerald)
                    }
                    IconButton(
                        onClick = {
                            if (linkedPhone.isBlank()) {
                                pendingCallLabel = "Video Call"
                                showPhoneDialog = true
                            } else {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$linkedPhone")))
                                }
                            }
                        },
                        modifier = Modifier.testTag("assistant_video_call_btn").minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Videocam, "Video call", tint = ElectricCyan)
                    }
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("assistant_clear_btn").minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, "Clear chat", tint = TextSecondary)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickPrompts) { prompt ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !isStreaming) { onAskQuestion(prompt) },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (msg.isUser) ElectricCyan else StudioSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = if (msg.isUser) null else BorderStroke(1.dp, StudioBorder)
                        ) {
                            Text(
                                text = msg.text.ifBlank { "..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.isUser) Color(0xFF041418) else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (isStreaming) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Text("SS Assistant is replying...", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
            }

            val err = errorMessage ?: voiceFeedback
            if (!err.isNullOrBlank()) {
                Text(text = err, style = MaterialTheme.typography.labelSmall, color = NeonRose)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) startVoiceCapture() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    enabled = !isStreaming,
                    modifier = Modifier.testTag("assistant_mic_btn").size(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, NeonRose),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Mic, "Voice question", tint = NeonRose)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask SS Assistant...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input_field")
                )

                Button(
                    onClick = {
                        val q = inputText.trim()
                        if (q.isNotBlank()) {
                            inputText = ""
                            onAskQuestion(q)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isStreaming,
                    modifier = Modifier.testTag("assistant_send_btn").size(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF041418)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send question")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showPhoneDialog) {
        var phoneDraft by remember(linkedPhone) { mutableStateOf(linkedPhone) }
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            containerColor = StudioSurfaceElevated,
            title = {
                Text(
                    text = "${pendingCallLabel ?: "Calling"} · Link Phone",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your mobile number with country code to enable Audio & Video calls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = phoneDraft,
                        onValueChange = { phoneDraft = it },
                        singleLine = true,
                        label = { Text("+92 300 1234567") },
                        modifier = Modifier.testTag("call_phone_input")
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = phoneDraft.trim()
                    if (clean.length >= 6) {
                        onSavePhone(clean)
                        showPhoneDialog = false
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
                        }
                    }
                }) {
                    Text("Save & Call", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
