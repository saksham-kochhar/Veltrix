package com.example.cherryai.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

class MicCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Captures 16 kHz mono PCM frames and always releases platform audio resources. */
class AudioRecorder {
    fun frames(): Flow<ShortArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            VoiceConfig.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            throw MicCaptureException("This device does not support 16 kHz mono capture")
        }
        val bufferBytes = minBuffer * 2
        val record = openInitializedRecord(bufferBytes)
        val effects = attachOptionalEffects(record)
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicCaptureException("Microphone did not enter recording state")
            }
            val frame = ShortArray(VoiceConfig.sampleRate * VoiceConfig.frameMs / 1000)
            while (coroutineContext.isActive) {
                coroutineContext.ensureActive()
                when (val read = record.read(frame, 0, frame.size)) {
                    AudioRecord.ERROR_INVALID_OPERATION ->
                        throw MicCaptureException("Microphone read failed (invalid operation)")
                    AudioRecord.ERROR_BAD_VALUE ->
                        throw MicCaptureException("Microphone read failed (bad buffer)")
                    in 1..frame.size -> emit(frame.copyOf(read))
                }
            }
        } finally {
            runCatching { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() }
            effects.forEach { runCatching { it.release() } }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun openInitializedRecord(bufferBytes: Int): AudioRecord {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
        )
        var lastError: String? = null
        for (source in sources) {
            val record = AudioRecord(
                source,
                VoiceConfig.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes,
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) return record
            lastError = "source=$source state=${record.state}"
            record.release()
        }
        throw MicCaptureException("Could not open microphone (${lastError ?: "unknown"})")
    }

    private fun attachOptionalEffects(record: AudioRecord): List<android.media.audiofx.AudioEffect> {
        val sessionId = record.audioSessionId
        val created = mutableListOf<android.media.audiofx.AudioEffect>()
        if (NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(sessionId) }.getOrNull()?.let { created.add(it) }
        }
        if (AcousticEchoCanceler.isAvailable()) {
            runCatching { AcousticEchoCanceler.create(sessionId) }.getOrNull()?.let { created.add(it) }
        }
        if (AutomaticGainControl.isAvailable()) {
            runCatching { AutomaticGainControl.create(sessionId) }.getOrNull()?.let { created.add(it) }
        }
        return created
    }
}

fun frameRms(frame: ShortArray): Float {
    if (frame.isEmpty()) return 0f
    val meanSq = frame.sumOf { sample ->
        val n = sample.toFloat() / Short.MAX_VALUE
        (n * n).toDouble()
    } / frame.size
    return sqrt(meanSq.toFloat())
}

/** Maps raw RMS into 0..1 for the on-screen meter (speech typically sits near [energyThreshold]). */
fun normalizeMeterLevel(rms: Float): Float {
    if (rms <= 0f) return 0f
    val normalized = (rms / VoiceConfig.energyThreshold) * 0.42f
    return normalized.coerceIn(0f, 1f)
}

fun frameWaveformBands(frame: ShortArray, barCount: Int = VoiceConfig.waveformBarCount): FloatArray {
    if (frame.isEmpty() || barCount <= 0) return FloatArray(barCount.coerceAtLeast(0))
    val bands = FloatArray(barCount)
    val chunkSize = (frame.size / barCount).coerceAtLeast(1)
    for (i in 0 until barCount) {
        val start = i * chunkSize
        if (start >= frame.size) break
        val end = minOf(start + chunkSize, frame.size)
        var sumSq = 0.0
        for (j in start until end) {
            val n = frame[j].toFloat() / Short.MAX_VALUE
            sumSq += n * n
        }
        bands[i] = normalizeMeterLevel(sqrt((sumSq / (end - start)).toFloat()))
    }
    return bands
}

fun smoothWaveformBands(previous: FloatArray, next: FloatArray): FloatArray {
    val size = maxOf(previous.size, next.size)
    return FloatArray(size) { index ->
        val prev = previous.getOrElse(index) { 0f }
        val target = next.getOrElse(index) { 0f }
        val factor = if (target > prev) 0.62f else 0.28f
        prev + (target - prev) * factor
    }
}

fun meterLevelToWaveformBands(level: Float, barCount: Int = VoiceConfig.waveformBarCount): FloatArray {
    val clamped = level.coerceIn(0f, 1f)
    return FloatArray(barCount) { index ->
        val wobble = 0.72f + 0.28f * kotlin.math.sin(index * 0.65f + clamped * 2.4f)
        (clamped * wobble).coerceIn(0f, 1f)
    }
}
