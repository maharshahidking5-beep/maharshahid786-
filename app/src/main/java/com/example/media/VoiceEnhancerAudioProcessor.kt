package com.example.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.VoiceEnhanceSettings
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.tanh

/**
 * Real-time PCM DSP AudioProcessor for Voice Enhancer:
 * - High-pass rumble/hum filter (noise reduction)
 * - Vocal presence/clarity boost
 * - Warm low-end shelf
 * - Soft-limiting loudness normalization
 */
@UnstableApi
class VoiceEnhancerAudioProcessor(
    @Volatile var settings: VoiceEnhanceSettings = VoiceEnhanceSettings()
) : BaseAudioProcessor() {

    private var prevInputSample = 0f
    private var prevHighPassSample = 0f
    private var lowPassState = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return super.isActive() && settings.isActive
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val current = settings
        if (!current.isActive) {
            val remaining = inputBuffer.remaining()
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val remaining = inputBuffer.remaining()
        val out = replaceOutputBuffer(remaining)

        val hpAlpha = (0.85f + current.noiseReduction * 0.12f).coerceIn(0.85f, 0.97f)
        val clarity = current.clarityBoost.coerceIn(0f, 1f) * 0.65f
        val warmth = current.warmthBass.coerceIn(0f, 1f) * 0.45f
        val gain = current.loudnessGain.coerceIn(0.4f, 2.2f)
        val noiseGateThreshold = current.noiseReduction * 0.018f

        while (inputBuffer.hasRemaining()) {
            val rawShort = inputBuffer.short
            var sample = rawShort / 32768f

            // 1. Noise reduction: high-pass DC/rumble removal + soft noise gate
            if (current.noiseReduction > 0.05f) {
                val hp = hpAlpha * (prevHighPassSample + sample - prevInputSample)
                prevInputSample = sample
                prevHighPassSample = hp
                sample = if (abs(hp) < noiseGateThreshold) hp * 0.35f else hp
            }

            // 2. Warmth (low-frequency body) & Clarity (vocal edge presence)
            lowPassState += 0.14f * (sample - lowPassState)
            val highBand = sample - lowPassState
            val shaped = sample + (lowPassState * warmth) + (highBand * clarity)

            // 3. Gain + smooth soft-clipper limiter to avoid digital distortion
            val boosted = tanh((shaped * gain).toDouble()).toFloat()
            val outShort = (boosted * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            out.putShort(outShort)
        }
        out.flip()
    }

    override fun onReset() {
        prevInputSample = 0f
        prevHighPassSample = 0f
        lowPassState = 0f
    }
}
