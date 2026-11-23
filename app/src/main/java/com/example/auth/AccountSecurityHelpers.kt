package com.example.auth

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

const val SS_SHARED_APP_URL = "https://ais-pre-ga4hlpaok5ljoew7df2kya-841710153459.asia-southeast1.run.app"
const val MAX_MULTI_DEVICE_LOGINS = 10

@Serializable
data class PendingSignUpProfile(
    val gmail: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val wantsPakistaniPro: Boolean,
    val pakistaniPhone: String
)

@Serializable
data class RegisteredDeviceEntry(
    val deviceId: String,
    val deviceLabel: String,
    val lastActiveMs: Long
)

data class StudioAvatarPreset(
    val id: String,
    val tokenUrl: String,
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long
)

data class ProfileCoverTheme(
    val id: String,
    val label: String,
    val startHex: Long,
    val endHex: Long,
    val accentHex: Long
)

object ProfileCustomizationCatalog {
    val avatarPresets: List<StudioAvatarPreset> = listOf(
        StudioAvatarPreset("cyber", "ss://avatar/cyber", "Cyber Cut", 0xFF00E5FF, 0xFF0E2936),
        StudioAvatarPreset("pak_pro", "ss://avatar/pak_pro", "Pak Pro", 0xFF10B981, 0xFF064E3B),
        StudioAvatarPreset("neon_rose", "ss://avatar/neon_rose", "Neon Reel", 0xFFFF2E88, 0xFF3B0764),
        StudioAvatarPreset("gold_vip", "ss://avatar/gold_vip", "Gold VIP", 0xFFF59E0B, 0xFF451A03),
        StudioAvatarPreset("canva_art", "ss://avatar/canva_art", "Canva Pro", 0xFF8B5CF6, 0xFF1E1B4B),
        StudioAvatarPreset("cinema", "ss://avatar/cinema", "Cinema 4K", 0xFF38BDF8, 0xFF0F172A)
    )

    val coverThemes: List<ProfileCoverTheme> = listOf(
        ProfileCoverTheme("CYBER_CYAN", "Cyber Neon", 0xFF0B2533, 0xFF190F2B, 0xFF00E5FF),
        ProfileCoverTheme("PAK_EMERALD", "Pak Emerald Pro", 0xFF042F22, 0xFF0C4A33, 0xFF10B981),
        ProfileCoverTheme("ROSE_PULSE", "Rose Pulse", 0xFF2D0A22, 0xFF11131A, 0xFFFF2E88),
        ProfileCoverTheme("ROYAL_VIOLET", "Canva Violet", 0xFF1D1138, 0xFF0D1426, 0xFF8B5CF6),
        ProfileCoverTheme("GOLD_LUXE", "Creator Gold", 0xFF2A1905, 0xFF14110B, 0xFFF59E0B),
        ProfileCoverTheme("OBSIDIAN", "Obsidian Dark", 0xFF12141B, 0xFF0A0B0F, 0xFF94A3B8)
    )

    fun findAvatarPreset(photoUrl: String): StudioAvatarPreset? {
        return avatarPresets.find { it.tokenUrl == photoUrl }
    }

    fun findCoverTheme(styleId: String): ProfileCoverTheme {
        return coverThemes.find { it.id == styleId } ?: coverThemes.first()
    }
}

object AccountSecurityHelpers {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val deviceJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun isValidGmailOrEmail(email: String): Boolean {
        return emailRegex.matches(email.trim())
    }

    fun normalizeUsername(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("@")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .take(30)
        return if (cleaned.isEmpty()) "" else "@$cleaned"
    }

    fun isValidUsername(raw: String): Boolean {
        val normalized = normalizeUsername(raw)
        return normalized.length >= 3 && normalized.length <= 32
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Validates and normalizes Pakistani mobile numbers:
     * accepts 03XXXXXXXXX (11 digits), 3XXXXXXXXX (10 digits), +923XXXXXXXXX, 923XXXXXXXXX.
     * Formats cleanly as "+92 3XX XXXXXXX" or returns null if invalid.
     */
    fun normalizePakistaniPhone(raw: String): String? {
        val digitsOnly = raw.trim().replace(Regex("[^0-9+]"), "")
        val localTenDigits = when {
            digitsOnly.startsWith("+92") && digitsOnly.length == 13 -> digitsOnly.substring(3)
            digitsOnly.startsWith("92") && digitsOnly.length == 12 -> digitsOnly.substring(2)
            digitsOnly.startsWith("03") && digitsOnly.length == 11 -> digitsOnly.substring(1)
            digitsOnly.startsWith("3") && digitsOnly.length == 10 -> digitsOnly
            else -> return null
        }
        if (localTenDigits.length != 10 || !localTenDigits.startsWith("3") || !localTenDigits.all { it.isDigit() }) {
            return null
        }
        val opCode = localTenDigits.substring(0, 3)
        val subscriber = localTenDigits.substring(3)
        return "+92 $opCode $subscriber"
    }

    fun hashStudioPassword(password: String, existingSaltHex: String? = null): Pair<String, String> {
        val saltBytes = if (!existingSaltHex.isNullOrBlank()) {
            hexToBytes(existingSaltHex)
        } else {
            ByteArray(16).also { SecureRandom().nextBytes(it) }
        }
        val hashBytes = try {
            val spec = PBEKeySpec(password.toCharArray(), saltBytes, 10_000, 256)
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } catch (_: Exception) {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(saltBytes)
            md.digest(password.toByteArray(Charsets.UTF_8))
        }
        return bytesToHex(hashBytes) to bytesToHex(saltBytes)
    }

    fun verifyStudioPassword(password: String, expectedHashHex: String, saltHex: String): Boolean {
        if (expectedHashHex.isBlank() || saltHex.isBlank()) return false
        val (computedHash, _) = hashStudioPassword(password, saltHex)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    fun decodeDevices(jsonRaw: String): List<RegisteredDeviceEntry> {
        if (jsonRaw.isBlank()) return emptyList()
        return runCatching { deviceJson.decodeFromString<List<RegisteredDeviceEntry>>(jsonRaw) }
            .getOrDefault(emptyList())
    }

    fun registerDeviceInList(
        existingJson: String,
        deviceId: String,
        deviceLabel: String,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val current = decodeDevices(existingJson).toMutableList()
        val idx = current.indexOfFirst { it.deviceId == deviceId }
        val updatedEntry = RegisteredDeviceEntry(
            deviceId = deviceId,
            deviceLabel = deviceLabel.take(40).ifBlank { "Android Mobile" },
            lastActiveMs = nowMs
        )
        if (idx >= 0) {
            current[idx] = updatedEntry
        } else {
            if (current.size >= MAX_MULTI_DEVICE_LOGINS) {
                current.sortBy { it.lastActiveMs }
                current.removeAt(0)
            }
            current.add(updatedEntry)
        }
        return deviceJson.encodeToString(current.take(MAX_MULTI_DEVICE_LOGINS))
    }

    fun currentDeviceLabel(): String {
        val brand = Build.MANUFACTURER?.trim().orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL?.trim().orEmpty()
        return "$brand $model".trim().ifBlank { "Android Mobile" }.take(40)
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.length % 2 == 0) hex else "0$hex"
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: 0
        }
    }
}

object AppShareHelper {
    fun buildAppShareMessage(senderHandle: String?, isPakPro: Boolean): String {
        val creatorTag = senderHandle?.takeIf { it.isNotBlank() }?.let { " by $it" }.orEmpty()
        val proTag = if (isPakPro) " [Pakistani Pro Creator]" else ""
        return buildString {
            appendLine("Create reels, CapCut Pro edits & Canva Pro posters with s.s Studio Pro$proTag$creatorTag!")
            appendLine("Sign in with Gmail on up to 10 mobile devices & share directly to the TikTok feed:")
            append(SS_SHARED_APP_URL)
        }
    }

    fun shareAppWithFriends(
        context: Context,
        senderHandle: String? = null,
        isPakPro: Boolean = false
    ) {
        val text = buildAppShareMessage(senderHandle, isPakPro)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Try s.s Studio Pro")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share s.s Studio with Friends"))
    }

    fun shareCreatorProfile(
        context: Context,
        displayName: String,
        handle: String,
        followers: Long,
        isPakPro: Boolean
    ) {
        val badge = if (isPakPro) " · Pakistani Pro 🇵🇰" else ""
        val text = "Follow $displayName ($handle$badge) on s.s Studio Pro · $followers followers! " +
            "Download & join on Android: $SS_SHARED_APP_URL"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$displayName ($handle) on s.s Studio")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Creator Profile"))
    }
}
