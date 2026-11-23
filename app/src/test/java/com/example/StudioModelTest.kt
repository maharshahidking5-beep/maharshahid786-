package com.example

import com.example.data.CloudSyncStatus
import com.example.data.CloudSyncUiState
import com.example.data.NetworkAndPowerObserver
import com.example.media.AudioSynthLibrary
import com.example.media.SmartMediaHelpers
import com.example.media.StudioCatalog
import com.example.media.WorkflowHelpersCatalog
import com.example.model.AspectRatioOption
import com.example.model.CanvaProCatalog
import com.example.model.ClipAnimationType
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.MainNavTab
import com.example.model.ProjectData
import com.example.model.SafeZoneMode
import com.example.model.SpeedCurvePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VideoClip
import com.example.model.VideoEffectPreset
import com.example.model.VoiceEnhancePreset
import com.example.model.VoiceEnhanceSettings
import com.example.ai.AiVideoVisualStyle
import com.example.auth.AccountSecurityHelpers
import com.example.model.BillingConfigDoc
import com.example.model.PakistanPaymentMethod
import com.example.model.ProPackageTier
import com.example.model.STUDIO_ADMIN_EMAIL
import com.example.model.isStudioAdminEmail
import com.example.auth.AppShareHelper
import com.example.auth.MAX_MULTI_DEVICE_LOGINS
import com.example.auth.ProfileCustomizationCatalog
import com.example.auth.SS_SHARED_APP_URL
import com.example.ui.components.formatShortDuration
import com.example.ui.components.formatTimecode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelTest {

    @Test
    fun `clip effective duration scales with trim and speed`() {
        val clip = VideoClip(
            uriString = "file:///sample.mp4",
            displayName = "Clip 1",
            totalDurationMs = 10000L,
            trimStartMs = 2000L,
            trimEndMs = 8000L,
            speed = 2.0f
        )
        assertEquals(3000L, clip.effectiveDurationMs)
    }

    @Test
    fun `project total duration sums clip effective durations`() {
        val project = ProjectData(
            title = "s.s Reel",
            clips = listOf(
                VideoClip(uriString = "a", displayName = "A", totalDurationMs = 4000L, trimStartMs = 0L, trimEndMs = 4000L, speed = 1f),
                VideoClip(uriString = "b", displayName = "B", totalDurationMs = 6000L, trimStartMs = 1000L, trimEndMs = 5000L, speed = 0.5f)
            )
        )
        assertEquals(12000L, project.totalDurationMs)
    }

    @Test
    fun `timecode formatting outputs mm_ss_tenths`() {
        assertEquals("00:04.2", formatTimecode(4250L))
        assertEquals("01:05", formatShortDuration(65000L))
    }

    @Test
    fun `default project uses 1080p plus 60fps`() {
        val project = ProjectData()
        assertEquals(ExportResolution.RES_1080P_PLUS, project.resolution)
        assertEquals(60, project.fps)
        assertEquals(26_000_000, project.resolution.targetBitrateBps)
    }

    @Test
    fun `voice enhancer settings detect active presets and sliders`() {
        val neutral = VoiceEnhanceSettings()
        assertFalse(neutral.isActive)

        val studioMic = VoiceEnhanceSettings(
            preset = VoiceEnhancePreset.STUDIO_VOICE,
            clarityBoost = VoiceEnhancePreset.STUDIO_VOICE.clarityBoost,
            noiseReduction = VoiceEnhancePreset.STUDIO_VOICE.noiseReduction,
            loudnessGain = VoiceEnhancePreset.STUDIO_VOICE.loudnessGain
        )
        assertTrue(studioMic.isActive)
        assertTrue(VoiceEnhanceSettings(pitchShift = 1.15f).isActive)
    }

    @Test
    fun `readymade templates cover categories with captions and tags`() {
        assertTrue(StudioCatalog.templates.size >= 12)
        assertTrue(StudioCatalog.templates.all { it.defaultCaption.isNotBlank() && it.badgeTag.isNotBlank() })
    }

    @Test
    fun `capcut effects filters transitions animations curves sfx and stickers catalogs are complete`() {
        assertTrue(VideoEffectPreset.entries.size >= 30)
        assertTrue(FilterPreset.entries.size >= 24)
        assertTrue(TransitionType.entries.size >= 12)
        assertTrue(ClipAnimationType.entries.size >= 10)
        assertTrue(SpeedCurvePreset.entries.size >= 6)
        assertTrue(AudioSynthLibrary.sfxPresets.size >= 8)
        assertTrue(StudioCatalog.stickers.size >= 16)
    }

    @Test
    fun `clip crop and total rotation helpers track transform edits`() {
        val defaultClip = VideoClip(uriString = "a", displayName = "Clip")
        assertFalse(defaultClip.hasCrop)
        assertEquals(0f, defaultClip.totalRotationDegrees, 0.01f)

        val editedClip = defaultClip.copy(
            cropLeft = 0.1f,
            cropRight = 0.85f,
            rotationDegrees = 90,
            freeRotation = -12.5f
        )
        assertTrue(editedClip.hasCrop)
        assertEquals(77.5f, editedClip.totalRotationDegrees, 0.01f)
    }

    @Test
    fun `canva pro catalog and navigation tabs are properly populated`() {
        assertTrue(CanvaProCatalog.brandKits.size >= 5)
        assertTrue(CanvaProCatalog.templates.size >= 6)
        assertTrue(CanvaProCatalog.templates.all { it.project.elements.isNotEmpty() })
        assertEquals(5, MainNavTab.entries.size)
        assertTrue(MainNavTab.entries.containsAll(listOf(MainNavTab.EDIT, MainNavTab.CANVA, MainNavTab.FEED, MainNavTab.TEMPLATES, MainNavTab.PROFILE)))
    }

    @Test
    fun `workflow helpers catalog has quick preset packs guides and multilingual phrases`() {
        val packTitles = WorkflowHelpersCatalog.quickPresetPacks.map { it.title }
        assertTrue(packTitles.containsAll(listOf("Vlog Ready", "Wedding Glow", "Gaming Punch", "Product Ad")))
        assertTrue(WorkflowHelpersCatalog.miniGuides.size >= 6)

        val languages = WorkflowHelpersCatalog.bilingualStarterPhrases.map { it.languageTag }.toSet()
        assertTrue(languages.containsAll(listOf("EN", "UR", "HI")))
        assertTrue(WorkflowHelpersCatalog.bilingualStarterPhrases.any { it.languageTag == "UR" && it.isRtl })
    }

    @Test
    fun `smart auto crop keeps normalized rect bounds around detected focus`() {
        val resultPortrait = SmartMediaHelpers.computeCropRectAroundFocus(
            sourceWidth = 1920f,
            sourceHeight = 1080f,
            focusX = 0.72f,
            focusY = 0.38f,
            targetRatio = AspectRatioOption.RATIO_9_16
        )
        assertTrue(resultPortrait.cropLeft in 0f..0.9f)
        assertTrue(resultPortrait.cropRight in (resultPortrait.cropLeft + 0.2f)..1f)
        assertTrue(resultPortrait.cropTop in 0f..0.9f)
        assertTrue(resultPortrait.cropBottom in (resultPortrait.cropTop + 0.2f)..1f)
    }

    @Test
    fun `auto beat markers and rhythm cut produce ordered beat markers and trimmed clips`() {
        val markers = SmartMediaHelpers.generateBeatMarkers(totalDurationMs = 8000L, bpm = 120)
        assertTrue(markers.size >= 4)
        assertTrue(markers.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })

        val rawClips = listOf(
            VideoClip(uriString = "c1", displayName = "C1", totalDurationMs = 7000L, trimStartMs = 0L, trimEndMs = 7000L),
            VideoClip(uriString = "c2", displayName = "C2", totalDurationMs = 6000L, trimStartMs = 500L, trimEndMs = 6000L)
        )
        val cut = SmartMediaHelpers.applyRhythmCutToClips(rawClips, bpm = 124)
        assertEquals(2, cut.size)
        assertTrue(cut.all { it.effectiveDurationMs in 700L..4500L })
    }

    @Test
    fun `export resolution includes 360p 480p and safe zone modes`() {
        assertTrue(ExportResolution.entries.containsAll(listOf(ExportResolution.RES_360P, ExportResolution.RES_480P, ExportResolution.RES_1080P_PLUS)))
        assertEquals(listOf(SafeZoneMode.NONE, SafeZoneMode.TIKTOK, SafeZoneMode.REELS, SafeZoneMode.SHORTS), SafeZoneMode.entries)

        val rtlTitle = TextOverlayItem(
            text = "خوش آمدید",
            languageTag = "UR",
            isRtl = true,
            isCurved = true,
            is3D = true,
            isNeonGlow = true
        )
        assertTrue(rtlTitle.isRtl && rtlTitle.isCurved && rtlTitle.is3D && rtlTitle.isNeonGlow)
        assertEquals("Not synced yet", NetworkAndPowerObserver.formatLastSyncedTime(0L))
        assertEquals(CloudSyncStatus.IDLE, CloudSyncUiState().status)
    }

    @Test
    fun `pakistani pro phone validation normalizes valid numbers and rejects invalid ones`() {
        assertEquals("+92 300 1234567", AccountSecurityHelpers.normalizePakistaniPhone("03001234567"))
        assertEquals("+92 312 9876543", AccountSecurityHelpers.normalizePakistaniPhone("+92 312 9876543"))
        assertEquals("+92 345 1122334", AccountSecurityHelpers.normalizePakistaniPhone("923451122334"))
        assertEquals("+92 333 4455667", AccountSecurityHelpers.normalizePakistaniPhone("3334455667"))
        assertNull(AccountSecurityHelpers.normalizePakistaniPhone("04212345678"))
        assertNull(AccountSecurityHelpers.normalizePakistaniPhone("12345"))
    }

    @Test
    fun `sign up gmail username password hashing and 10 device session cap work reliably`() {
        assertTrue(AccountSecurityHelpers.isValidGmailOrEmail("creator.pk@gmail.com"))
        assertFalse(AccountSecurityHelpers.isValidGmailOrEmail("not-an-email"))

        assertEquals("@ss_creator", AccountSecurityHelpers.normalizeUsername("  ss_creator! "))
        assertTrue(AccountSecurityHelpers.isValidUsername("ali_khan"))
        assertFalse(AccountSecurityHelpers.isValidUsername("a"))

        assertTrue(AccountSecurityHelpers.isValidPassword("studio99"))
        assertFalse(AccountSecurityHelpers.isValidPassword("12345"))

        val (hash, salt) = AccountSecurityHelpers.hashStudioPassword("studio99")
        assertTrue(AccountSecurityHelpers.verifyStudioPassword("studio99", hash, salt))
        assertFalse(AccountSecurityHelpers.verifyStudioPassword("wrongpass", hash, salt))

        var devicesJson = ""
        repeat(12) { idx ->
            devicesJson = AccountSecurityHelpers.registerDeviceInList(
                existingJson = devicesJson,
                deviceId = "dev_$idx",
                deviceLabel = "Mobile $idx",
                nowMs = 1000L + idx
            )
        }
        val decoded = AccountSecurityHelpers.decodeDevices(devicesJson)
        assertEquals(MAX_MULTI_DEVICE_LOGINS, decoded.size)
        assertTrue(decoded.any { it.deviceId == "dev_11" })
        assertFalse(decoded.any { it.deviceId == "dev_0" })
    }

    @Test
    fun `profile customization catalog and friend share message contain expected data`() {
        assertEquals(6, ProfileCustomizationCatalog.avatarPresets.size)
        assertEquals(6, ProfileCustomizationCatalog.coverThemes.size)
        assertNotNull(ProfileCustomizationCatalog.findAvatarPreset("ss://avatar/pak_pro"))

        val shareMessage = AppShareHelper.buildAppShareMessage(senderHandle = "@sara_edits", isPakPro = true)
        assertTrue(shareMessage.contains(SS_SHARED_APP_URL))
        assertTrue(shareMessage.contains("@sara_edits"))
        assertTrue(shareMessage.contains("Pakistani Pro"))
        assertTrue(shareMessage.contains("10 mobile devices"))
    }

    @Test
    fun `admin email check and jazzcash easypaisa billing packages resolve accurately`() {
        assertTrue(isStudioAdminEmail(STUDIO_ADMIN_EMAIL))
        assertTrue(isStudioAdminEmail("  MOMILOVEYOUSOMUCH7@GMAIL.COM "))
        assertFalse(isStudioAdminEmail("random.creator@gmail.com"))
        assertFalse(isStudioAdminEmail(null))

        val cfg = BillingConfigDoc(
            jazzCashNumber = "+92 301 5556677",
            jazzCashTitle = "Admin Jazz",
            easyPaisaNumber = "+92 345 8889900",
            easyPaisaTitle = "Admin Easy",
            weeklyPricePkr = 350L,
            monthlyPricePkr = 999L,
            yearlyPricePkr = 2999L
        )
        assertEquals(350L, cfg.priceFor(ProPackageTier.WEEKLY_PRO))
        assertEquals(999L, cfg.priceFor(ProPackageTier.MONTHLY_PRO))
        assertEquals(2999L, cfg.priceFor(ProPackageTier.YEARLY_PRO))
        assertEquals("+92 301 5556677", cfg.numberFor(PakistanPaymentMethod.JAZZCASH))
        assertEquals("Admin Easy", cfg.titleFor(PakistanPaymentMethod.EASYPAISA))
        assertEquals(ProPackageTier.WEEKLY_PRO, ProPackageTier.fromId("WEEKLY_PRO"))
        assertEquals(PakistanPaymentMethod.EASYPAISA, PakistanPaymentMethod.fromId("EASYPAISA"))
        assertTrue(AiVideoVisualStyle.entries.size >= 6)
    }
}
