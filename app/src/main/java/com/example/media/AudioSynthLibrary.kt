package com.example.media

import android.content.Context
import android.net.Uri
import com.example.model.AudioTrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class StudioMusicPreset(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val durationMs: Long,
    val waveform: List<Float>
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
        )
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

    private fun writeSynthesizedWav(file: File, preset: StudioMusicPreset) {
        val sampleRate = 22050
        val durationSec = (preset.durationMs / 1000.0).coerceAtLeast(4.0)
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcmBytes = ByteArray(totalSamples * 2)
        val beatDurationSec = 60.0 / preset.bpm

        val chords = when (preset.id) {
            "neon_pulse" -> listOf(
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
            "cinema_rise" -> listOf(
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

            // Kick drum envelope on every beat
            val kickEnv = exp(-beatPos * 9.0)
            val kickFreq = 52.0 + 95.0 * exp(-beatPos * 18.0)
            val kick = sin(2.0 * PI * kickFreq * t) * kickEnv * 0.45

            // Musical chord pad / pluck
            var harmony = 0.0
            for (nIdx in chord.indices) {
                val note = chord[nIdx]
                val arpPhase = ((t * 2.0 + nIdx * 0.25) % 1.0)
                val pluck = if (preset.id == "morning_acoustic" || preset.id == "neon_pulse") {
                    exp(-arpPhase * 4.5) * 0.22
                } else {
                    0.13
                }
                harmony += sin(2.0 * PI * note * t) * pluck
            }

            // Smooth fade-in and fade-out at edges
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
