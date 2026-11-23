package com.example.media

import android.content.Context
import android.net.Uri
import com.example.model.AudioTrackItem
import com.example.model.SfxOverlayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class StudioMusicPreset(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val durationMs: Long,
    val waveform: List<Float>
)

data class SfxPreset(
    val id: String,
    val title: String,
    val category: String,
    val durationMs: Long
)

object AudioSynthLibrary {

    val presets: List<StudioMusicPreset> = listOf(
        StudioMusicPreset(
            id = "neon_pulse",
            title = "Neon Pulse",
            artist = "s.s Synth Lab",
            genre = "Cyber / Phonk",
            bpm = 128,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                val beat = if (idx % 4 == 0) 0.95f else if (idx % 2 == 0) 0.65f else 0.38f
                (beat * (0.85f + 0.15f * sin(idx * 0.7f))).coerceIn(0.2f, 1f)
            }
        ),
        StudioMusicPreset(
            id = "lofi_sunset",
            title = "Lo-Fi Sunset",
            artist = "s.s Chill Beats",
            genre = "Lo-Fi / Vlog",
            bpm = 85,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                (0.45f + 0.35f * sin(idx * 0.4f) * sin(idx * 0.15f) + if (idx % 6 == 0) 0.25f else 0f)
                    .coerceIn(0.2f, 0.95f)
            }
        ),
        StudioMusicPreset(
            id = "cinema_rise",
            title = "Cinema Horizon",
            artist = "s.s Score",
            genre = "Cinematic",
            bpm = 96,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                val swell = (idx / 48f) * 0.55f + 0.28f
                (swell + 0.15f * sin(idx * 0.5f)).coerceIn(0.2f, 1f)
            }
        ),
        StudioMusicPreset(
            id = "trap_bounce",
            title = "Velocity Bounce",
            artist = "s.s 808",
            genre = "Beat Sync",
            bpm = 140,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                if (idx % 3 == 0) 0.98f else if (idx % 3 == 1) 0.55f else 0.30f
            }
        ),
        StudioMusicPreset(
            id = "morning_acoustic",
            title = "Morning Cafe",
            artist = "s.s Acoustic",
            genre = "Lifestyle",
            bpm = 104,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                (0.35f + 0.45f * kotlin.math.abs(sin(idx * 0.45f))).coerceIn(0.2f, 0.9f)
            }
        ),
        StudioMusicPreset(
            id = "midnight_drill",
            title = "Midnight 808",
            artist = "s.s Bass",
            genre = "Club / Edit",
            bpm = 136,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                if (idx % 4 == 0) 1.0f else if (idx % 4 == 2) 0.78f else 0.28f
            }
        ),
        StudioMusicPreset(
            id = "cinematic_strings",
            title = "Golden Strings",
            artist = "s.s Orchestra",
            genre = "Travel / Story",
            bpm = 90,
            durationMs = 12000L,
            waveform = List(48) { idx ->
                (0.30f + 0.50f * kotlin.math.abs(sin(idx * 0.22f))).coerceIn(0.2f, 0.92f)
            }
        )
    )

    val sfxPresets: List<SfxPreset> = listOf(
        SfxPreset("sfx_whoosh", "Whoosh", "Transition", 750L),
        SfxPreset("sfx_bass_drop", "Bass Drop", "Beat", 1100L),
        SfxPreset("sfx_shutter", "Camera Snap", "Photo", 350L),
        SfxPreset("sfx_pop_ding", "Pop Ding", "UI / Text", 450L),
        SfxPreset("sfx_glitch_zap", "Glitch Zap", "Cyber", 650L),
        SfxPreset("sfx_riser", "Riser Swell", "Build", 1400L),
        SfxPreset("sfx_sparkle", "Magic Chime", "Light", 900L),
        SfxPreset("sfx_boom", "Cinema Boom", "Impact", 1200L)
    )

    suspend fun ensureTrackFile(context: Context, presetId: String): AudioTrackItem =
        withContext(Dispatchers.IO) {
            val preset = presets.find { it.id == presetId } ?: presets.first()
            val dir = File(context.filesDir, "audio").apply { mkdirs() }
            val wavFile = File(dir, "${preset.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 1024L) {
                writeSynthesizedWav(wavFile, preset)
            }
            AudioTrackItem(
                id = preset.id,
                title = preset.title,
                artist = "${preset.artist} · ${preset.bpm} BPM",
                uriString = Uri.fromFile(wavFile).toString(),
                durationMs = preset.durationMs,
                volume = 0.85f
            )
        }

    suspend fun ensureSfxFile(context: Context, sfxId: String, startMs: Long): SfxOverlayItem =
        withContext(Dispatchers.IO) {
            val preset = sfxPresets.find { it.id == sfxId } ?: sfxPresets.first()
            val dir = File(context.filesDir, "sfx").apply { mkdirs() }
            val wavFile = File(dir, "${preset.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 512L) {
                writeSynthesizedSfxWav(wavFile, preset)
            }
            SfxOverlayItem(
                sfxId = preset.id,
                title = preset.title,
                uriString = Uri.fromFile(wavFile).toString(),
                startMs = startMs.coerceAtLeast(0L),
                durationMs = preset.durationMs,
                volume = 0.95f
            )
        }

    private fun writeSynthesizedWav(file: File, preset: StudioMusicPreset) {
        val sampleRate = 22050
        val durationSec = (preset.durationMs / 1000.0).coerceAtLeast(4.0)
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcmBytes = ByteArray(totalSamples * 2)
        val beatDurationSec = 60.0 / preset.bpm

        val chords = when (preset.id) {
            "neon_pulse", "midnight_drill" -> listOf(
                doubleArrayOf(110.0, 164.81, 220.0, 261.63),
                doubleArrayOf(98.0, 146.83, 196.0, 246.94),
                doubleArrayOf(87.31, 130.81, 174.61, 220.0),
                doubleArrayOf(130.81, 196.0, 261.63, 329.63)
            )
            "lofi_sunset" -> listOf(
                doubleArrayOf(261.63, 329.63, 392.0, 493.88),
                doubleArrayOf(220.0, 261.63, 329.63, 392.0),
                doubleArrayOf(174.61, 220.0, 261.63, 349.23),
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)
            )
            "cinema_rise", "cinematic_strings" -> listOf(
                doubleArrayOf(130.81, 196.0, 261.63, 311.13),
                doubleArrayOf(116.54, 174.61, 233.08, 293.66),
                doubleArrayOf(103.83, 155.56, 207.65, 261.63),
                doubleArrayOf(98.0, 146.83, 196.0, 246.94)
            )
            "trap_bounce" -> listOf(
                doubleArrayOf(55.0, 110.0, 220.0, 329.63),
                doubleArrayOf(55.0, 110.0, 207.65, 311.13),
                doubleArrayOf(65.41, 130.81, 261.63, 392.0),
                doubleArrayOf(61.74, 123.47, 246.94, 369.99)
            )
            else -> listOf(
                doubleArrayOf(293.66, 369.99, 440.0, 587.33),
                doubleArrayOf(220.0, 277.18, 329.63, 440.0),
                doubleArrayOf(246.94, 293.66, 369.99, 493.88),
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)
            )
        }

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val beatPos = (t % beatDurationSec) / beatDurationSec
            val barIdx = ((t / (beatDurationSec * 4)).toInt()) % chords.size
            val chord = chords[barIdx]

            val kickEnv = exp(-beatPos * 9.0)
            val kickFreq = 52.0 + 95.0 * exp(-beatPos * 18.0)
            val kick = sin(2.0 * PI * kickFreq * t) * kickEnv * 0.45

            var harmony = 0.0
            for (nIdx in chord.indices) {
                val note = chord[nIdx]
                val arpPhase = ((t * 2.0 + nIdx * 0.25) % 1.0)
                val pluck = if (preset.id == "morning_acoustic" || preset.id == "neon_pulse" || preset.id == "midnight_drill") {
                    exp(-arpPhase * 4.5) * 0.22
                } else {
                    0.13
                }
                harmony += sin(2.0 * PI * note * t) * pluck
            }

            val edgeEnv = when {
                t < 0.15 -> t / 0.15
                t > durationSec - 0.25 -> ((durationSec - t) / 0.25).coerceAtLeast(0.0)
                else -> 1.0
            }

            val sampleVal = ((kick + harmony) * edgeEnv * 0.72).coerceIn(-0.98, 0.98)
            val pcm16 = (sampleVal * Short.MAX_VALUE).toInt().toShort()
            pcmBytes[i * 2] = (pcm16.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((pcm16.toInt() shr 8) and 0xFF).toByte()
        }

        val header = createWavHeader(pcmBytes.size, sampleRate, 1, 16)
        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
    }

    private fun writeSynthesizedSfxWav(file: File, preset: SfxPreset) {
        val sampleRate = 22050
        val durationSec = (preset.durationMs / 1000.0).coerceAtLeast(0.25)
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcmBytes = ByteArray(totalSamples * 2)
        val rng = Random(preset.id.hashCode())
        var smoothNoise = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val norm = (i.toDouble() / totalSamples).coerceIn(0.0, 1.0)
            val rawNoise = rng.nextDouble(-1.0, 1.0)
            smoothNoise += 0.25 * (rawNoise - smoothNoise)

            val sampleVal = when (preset.id) {
                "sfx_whoosh" -> {
                    val env = sin(PI * norm)
                    val sweep = sin(2.0 * PI * (180.0 + 700.0 * norm) * t) * 0.35
                    (smoothNoise * 0.75 + sweep) * env
                }
                "sfx_bass_drop" -> {
                    val env = exp(-norm * 3.8)
                    val freq = 140.0 * exp(-norm * 2.2) + 38.0
                    sin(2.0 * PI * freq * t) * env * 0.95
                }
                "sfx_shutter" -> {
                    val click1 = exp(-norm * 28.0)
                    val click2 = if (norm > 0.35) exp(-(norm - 0.35) * 32.0) else 0.0
                    (rawNoise * 0.65 + sin(2.0 * PI * 1400.0 * t) * 0.35) * (click1 + click2)
                }
                "sfx_pop_ding" -> {
                    val env = exp(-norm * 7.5)
                    (sin(2.0 * PI * 880.0 * t) + 0.5 * sin(2.0 * PI * 1320.0 * t)) * env * 0.75
                }
                "sfx_glitch_zap" -> {
                    val env = exp(-norm * 4.5)
                    val mod = if ((i / 220) % 2 == 0) 920.0 else 310.0
                    (sin(2.0 * PI * mod * t) * 0.6 + rawNoise * 0.35) * env
                }
                "sfx_riser" -> {
                    val env = norm * norm
                    val freq = 220.0 + 1100.0 * norm * norm
                    (sin(2.0 * PI * freq * t) * 0.65 + smoothNoise * 0.35) * env
                }
                "sfx_sparkle" -> {
                    val env = exp(-norm * 4.2)
                    val note = when {
                        norm < 0.25 -> 1046.5
                        norm < 0.5 -> 1318.5
                        norm < 0.75 -> 1568.0
                        else -> 2093.0
                    }
                    sin(2.0 * PI * note * t) * env * 0.75
                }
                else -> { // sfx_boom
                    val env = exp(-norm * 3.0)
                    val sub = sin(2.0 * PI * (68.0 * exp(-norm * 1.4) + 32.0) * t)
                    (sub * 0.85 + smoothNoise * 0.45) * env
                }
            }.coerceIn(-0.98, 0.98)

            val pcm16 = (sampleVal * Short.MAX_VALUE).toInt().toShort()
            pcmBytes[i * 2] = (pcm16.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((pcm16.toInt() shr 8) and 0xFF).toByte()
        }

        val header = createWavHeader(pcmBytes.size, sampleRate, 1, 16)
        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
    }

    private fun createWavHeader(
        pcmDataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + pcmDataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(pcmDataSize)
        return buffer.array()
    }
}
