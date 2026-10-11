package com.example.cherryai.voice

import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive

/** Captures 16 kHz mono PCM frames and always releases platform audio resources. */
class AudioRecorder {
    fun frames(): Flow<ShortArray> = flow {
        val size = AudioRecord.getMinBufferSize(VoiceConfig.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(VoiceConfig.sampleRate / 10)
        val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, VoiceConfig.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size * 2)
        val effects = listOfNotNull(
            if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(record.audioSessionId) else null,
            if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(record.audioSessionId) else null,
            if (AutomaticGainControl.isAvailable()) AutomaticGainControl.create(record.audioSessionId) else null,
        )
        try {
            record.startRecording()
            val frame = ShortArray(VoiceConfig.sampleRate * VoiceConfig.frameMs / 1000)
            while (coroutineContext.isActive) {
                coroutineContext.ensureActive()
                if (record.read(frame, 0, frame.size) > 0) emit(frame.copyOf())
            }
        } finally {
            runCatching { record.stop() }
            effects.forEach { runCatching { it.release() } }
            record.release()
        }
    }.flowOn(Dispatchers.IO)
}
