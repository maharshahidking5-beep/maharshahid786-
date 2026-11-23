package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.SsLogoMark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val AUTH_TAG = "Auth"

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Google Sign-In requires an Activity context.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }
}

fun signOutWithCleanup(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

private enum class AuthEntryMode(val label: String) {
    SIGN_UP("Sign Up"),
    RE_LOGIN("Re-Login (10 Mobiles)")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (PendingSignUpProfile?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var authMode by remember { mutableStateOf(AuthEntryMode.SIGN_UP) }
    var gmailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var wantsPakistaniPro by remember { mutableStateOf(false) }
    var pakPhoneInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess(null)
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07131E),
                        StudioBackground,
                        Color(0xFF1A091B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SsLogoMark(size = 76.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "s.s Studio Pro",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CapCut Pro · Canva Pro · TikTok Feed · Multi-Device Sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioSurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sign Up vs Re-Login Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StudioSurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthEntryMode.entries.forEach { mode ->
                            val selected = authMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(if (mode == AuthEntryMode.SIGN_UP) "auth_tab_signup" else "auth_tab_relogin")
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        authMode = mode
                                        errorMessage = null
                                    },
                                color = if (selected) ElectricCyan.copy(alpha = 0.18f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                border = if (selected) BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)) else null
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) ElectricCyan else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    if (authMode == AuthEntryMode.SIGN_UP) {
                        Text(
                            text = "Create Account (Gmail, Password & @Username)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = gmailInput,
                            onValueChange = { gmailInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_email_input"),
                            label = { Text("Gmail Address") },
                            placeholder = { Text("creator@gmail.com") },
                            leadingIcon = { Icon(Icons.Filled.Email, null, tint = ElectricCyan) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_username_input"),
                            label = { Text("Creator @Username") },
                            placeholder = { Text("@ali_studio") },
                            leadingIcon = { Icon(Icons.Filled.AlternateEmail, null, tint = NeonRose) },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_signup_password_input"),
                            label = { Text("Studio Password (min 6 chars)") },
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = VioletPulse) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        // Pakistani Pro vs Without Pro (Free - No Phone Needed)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose Plan (Phone only required for Pakistani Pro)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_tier_free_chip")
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            wantsPakistaniPro = false
                                            errorMessage = null
                                        },
                                    color = if (!wantsPakistaniPro) ElectricCyan.copy(alpha = 0.16f) else StudioSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (!wantsPakistaniPro) ElectricCyan else StudioBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Without Pro",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (!wantsPakistaniPro) ElectricCyan else TextPrimary
                                        )
                                        Text(
                                            text = "No Phone Needed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_tier_pro_chip")
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            wantsPakistaniPro = true
                                            errorMessage = null
                                        },
                                    color = if (wantsPakistaniPro) StudioEmerald.copy(alpha = 0.18f) else StudioSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (wantsPakistaniPro) StudioEmerald else StudioBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Pakistani Pro",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (wantsPakistaniPro) StudioEmerald else TextPrimary
                                        )
                                        Text(
                                            text = "+92 Number Required",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (wantsPakistaniPro) {
                            OutlinedTextField(
                                value = pakPhoneInput,
                                onValueChange = { pakPhoneInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_signup_phone_input"),
                                label = { Text("Pakistani Mobile Number (+92)") },
                                placeholder = { Text("0300 1234567 or +92 300 1234567") },
                                leadingIcon = { Icon(Icons.Filled.PhoneAndroid, null, tint = StudioEmerald) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                        }
                    } else {
                        Text(
                            text = "Re-Login on Any Mobile (Up to $MAX_MULTI_DEVICE_LOGINS Devices)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.38f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Devices, null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Use your same Gmail account on up to $MAX_MULTI_DEVICE_LOGINS mobiles at once. Your username, Pakistani Pro status, CapCut Cloud drafts, Canva designs & TikTok posts sync automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FeaturePill("1080p+ CapCut Pro", ElectricCyan)
                        FeaturePill("Canva Pro", VioletPulse)
                        FeaturePill("TikTok Feed", NeonRose)
                        FeaturePill("10-Device Sync", StudioEmerald)
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = NeonRose.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NeonRose.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDA4AF),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            var pendingProfile: PendingSignUpProfile? = null
                            if (authMode == AuthEntryMode.SIGN_UP) {
                                if (!AccountSecurityHelpers.isValidGmailOrEmail(gmailInput)) {
                                    errorMessage = "Please enter a valid Gmail or email address."
                                    return@Button
                                }
                                if (!AccountSecurityHelpers.isValidUsername(usernameInput)) {
                                    errorMessage = "Please enter a valid @username (at least 2 letters/numbers)."
                                    return@Button
                                }
                                if (!AccountSecurityHelpers.isValidPassword(passwordInput)) {
                                    errorMessage = "Studio Password must be at least 6 characters."
                                    return@Button
                                }
                                val normalizedPhone = if (wantsPakistaniPro) {
                                    AccountSecurityHelpers.normalizePakistaniPhone(pakPhoneInput) ?: run {
                                        errorMessage = "Enter a valid Pakistani number (e.g. 0300 1234567 or +92 300 1234567), or select Without Pro."
                                        return@Button
                                    }
                                } else ""

                                val normalizedHandle = AccountSecurityHelpers.normalizeUsername(usernameInput)
                                val (pwdHash, pwdSalt) = AccountSecurityHelpers.hashStudioPassword(passwordInput)
                                pendingProfile = PendingSignUpProfile(
                                    gmail = gmailInput.trim(),
                                    username = normalizedHandle,
                                    displayName = normalizedHandle.removePrefix("@").replaceFirstChar { it.uppercase() },
                                    passwordHash = pwdHash,
                                    passwordSalt = pwdSalt,
                                    wantsPakistaniPro = wantsPakistaniPro,
                                    pakistaniPhone = normalizedPhone
                                )
                            }

                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess(pendingProfile)
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMessage = "Sign-in was cancelled. Tap to verify with your Gmail account."
                                }
                            )
                        },
                        enabled = !isLoading && !isCheckingAutoSignIn,
                        modifier = Modifier
                            .testTag(if (authMode == AuthEntryMode.SIGN_UP) "google_sign_in_button" else "auth_relogin_button")
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF041418)
                        )
                    ) {
                        if (isLoading || isCheckingAutoSignIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF041418),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCheckingAutoSignIn) "Checking Google session..." else "Verifying Gmail...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = if (authMode == AuthEntryMode.SIGN_UP) Icons.Filled.Verified else Icons.Filled.Login,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (authMode == AuthEntryMode.SIGN_UP) {
                                    "Sign Up & Verify with Gmail"
                                } else {
                                    "Re-Login with Gmail / Google"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Secured by Google Auth · Online Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        OutlinedButton(
                            onClick = { AppShareHelper.shareAppWithFriends(context) },
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("auth_share_app_button")
                        ) {
                            Icon(Icons.Filled.Share, null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Share App", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(label: String, tint: Color) {
    Surface(
        color = StudioSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}
