package com.example.cherryai.voice

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Transcribing(val pending: Int) : VoiceState
    data object DeviceListening : VoiceState
    data class Error(val message: String) : VoiceState
}

/** Coordinates microphone capture, Groq backend transcription, and Google on-device fallback. */
class VoiceInputViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state = _state.asStateFlow()
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel = _audioLevel.asStateFlow()
    private val _waveformBands = MutableStateFlow(FloatArray(VoiceConfig.waveformBarCount))
    val waveformBands = _waveformBands.asStateFlow()
    private val _pasteToInput = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val pasteToInput = _pasteToInput.asSharedFlow()
    private val _sendDirect = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val sendDirect = _sendDirect.asSharedFlow()

    private var recording: Job? = null
    private var deviceSpeech: DeviceSpeechRecognizer? = null
    private val transcribeMutex = Mutex()
    private var deviceMode = false
    private val captureBuffer = Collections.synchronizedList(ArrayList<ShortArray>())
    private val deviceFinalParts = mutableListOf<String>()
    private var devicePartial = ""

    fun start() {
        if (_state.value.showVoiceBar()) return
        resetSessionBuffers()
        deviceMode = !NetworkReachability.hasInternet(getApplication())
        if (deviceMode) {
            startDeviceSession()
        } else {
            startBackendSession()
        }
    }

    fun cancel() {
        recording?.cancel()
        recording = null
        deviceSpeech?.stop()
        deviceSpeech = null
        resetSessionBuffers()
        deviceMode = false
        _state.value = VoiceState.Idle
    }

    fun finishPaste() {
        finishInternal(sendDirect = false)
    }

    fun finishSend() {
        finishInternal(sendDirect = true)
    }

    fun clearError() {
        if (_state.value is VoiceState.Error) _state.value = VoiceState.Idle
    }

    private fun finishInternal(sendDirect: Boolean) {
        if (deviceMode) {
            val text = combinedDeviceText()
            deviceSpeech?.stop()
            deviceSpeech = null
            deviceMode = false
            resetSessionBuffers()
            if (text.isBlank()) {
                _state.value = VoiceState.Error("No speech captured. Try again.")
                return
            }
            viewModelScope.launch { emitResult(text, sendDirect) }
            _state.value = VoiceState.Idle
            return
        }
        recording?.cancel()
        recording = null
        val pcm = synchronized(captureBuffer) {
            captureBuffer.flatMap { frame -> frame.asIterable() }.toShortArray().also { captureBuffer.clear() }
        }.let { samples ->
            if (samples.size <= VoiceConfig.maxSamplesPerRequest) samples
            else samples.copyOfRange(samples.size - VoiceConfig.maxSamplesPerRequest, samples.size)
        }
        viewModelScope.launch {
            if (pcm.size < VoiceConfig.minSamplesOnStop) {
                _state.value = VoiceState.Error("No speech captured. Try again.")
                return@launch
            }
            val text = transcribeCapturedAudio(pcm, allowDeviceFallback = true)
            if (text.isNullOrBlank()) return@launch
            emitResult(text, sendDirect)
            if (_state.value !is VoiceState.Error) _state.value = VoiceState.Idle
        }
    }

    private suspend fun emitResult(text: String, sendDirect: Boolean) {
        if (sendDirect) _sendDirect.emit(text.trim())
        else _pasteToInput.emit(text.trim())
    }

    private fun resetSessionBuffers() {
        captureBuffer.clear()
        deviceFinalParts.clear()
        devicePartial = ""
        _audioLevel.value = 0f
        _waveformBands.value = FloatArray(VoiceConfig.waveformBarCount)
    }

    private fun combinedDeviceText(): String =
        (deviceFinalParts + devicePartial).filter { it.isNotBlank() }.joinToString(" ").trim()

    private fun startBackendSession() {
        deviceMode = false
        _state.value = VoiceState.Listening
        recording = viewModelScope.launch {
            try {
                AudioRecorder().frames().collect { frame ->
                    captureBuffer.add(frame)
                    val bands = frameWaveformBands(frame)
                    _waveformBands.value = smoothWaveformBands(_waveformBands.value, bands)
                    _audioLevel.value = _waveformBands.value.maxOrNull() ?: 0f
                }
            } catch (_: SecurityException) {
                _state.value = VoiceState.Error("Microphone permission is required")
            } catch (e: CancellationException) {
                throw e
            } catch (e: MicCaptureException) {
                Log.w(TAG, "Mic capture failed, falling back to device STT", e)
                switchToDeviceFallback()
            } catch (e: Exception) {
                Log.w(TAG, "Mic capture failed, falling back to device STT", e)
                switchToDeviceFallback()
            } finally {
                _audioLevel.value = 0f
                _waveformBands.value = FloatArray(VoiceConfig.waveformBarCount)
            }
        }
    }

    private fun startDeviceSession() {
        deviceMode = true
        resetSessionBuffers()
        _state.value = VoiceState.DeviceListening
        deviceSpeech?.stop()
        deviceSpeech = DeviceSpeechRecognizer(
            app = getApplication(),
            onFinalText = { text ->
                deviceFinalParts.add(text.trim())
                devicePartial = ""
            },
            onPartialText = { partial ->
                devicePartial = partial.trim()
            },
            onLevel = { level ->
                val normalized = level.coerceIn(0f, 1f)
                _audioLevel.value = normalized
                _waveformBands.value = meterLevelToWaveformBands(normalized)
            },
            onFatalError = { message -> _state.value = VoiceState.Error(message) },
        ).also { it.start() }
    }

    private suspend fun switchToDeviceFallback() {
        recording?.cancel()
        recording = null
        captureBuffer.clear()
        _audioLevel.value = 0f
        _waveformBands.value = FloatArray(VoiceConfig.waveformBarCount)
        withContext(Dispatchers.Main) {
            startDeviceSession()
        }
    }

    private suspend fun transcribeCapturedAudio(pcm: ShortArray, allowDeviceFallback: Boolean): String? {
        return transcribeMutex.withLock {
            if (deviceMode) return@withLock null
            _state.value = VoiceState.Transcribing(1)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    GroqTranscriptionClient.transcribe(WavEncoder.encode(pcm))
                }
            }
            result.onFailure { error ->
                when (error) {
                    is TranscriptionFailure -> {
                        if (allowDeviceFallback && error.useDeviceFallback) {
                            switchToDeviceFallback()
                            return@withLock null
                        }
                        _state.value = VoiceState.Error(error.message ?: "Transcription failed")
                        return@withLock null
                    }
                    else -> {
                        if (allowDeviceFallback) {
                            switchToDeviceFallback()
                            return@withLock null
                        }
                        _state.value = VoiceState.Error("Transcription failed")
                        return@withLock null
                    }
                }
            }
            val text = result.getOrNull()?.trim().orEmpty()
            if (text.isBlank()) {
                _state.value = VoiceState.Error("Could not transcribe audio")
                return@withLock null
            }
            text
        }
    }

    override fun onCleared() {
        deviceSpeech?.stop()
        recording?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "VoiceInput"
    }
}

fun VoiceState.isMicSessionActive(): Boolean = when (this) {
    VoiceState.Idle, is VoiceState.Error -> false
    else -> true
}

fun VoiceState.showVoiceBar(): Boolean = when (this) {
    VoiceState.Idle, is VoiceState.Error -> false
    else -> true
}
