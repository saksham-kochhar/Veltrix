package com.example.cherryai.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed interface VoiceState { data object Idle : VoiceState; data object Listening : VoiceState; data class Transcribing(val pending: Int) : VoiceState; data class Error(val message: String) : VoiceState }

/** Coordinates one microphone session and ordered backend transcription. */
class VoiceInputViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state = _state.asStateFlow()
    private val _append = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val append = _append.asSharedFlow()
    private var recording: Job? = null
    private var cancelled = false

    fun start() { if (recording?.isActive == true) return; cancelled = false; _state.value = VoiceState.Listening; recording = viewModelScope.launch { val vad = VadSegmenter(); try { AudioRecorder().frames().collect { frame -> vad.accept(frame).forEachIndexed { _, pcm -> transcribe(pcm) }; if (recording?.isActive != true) return@collect } } catch (_: SecurityException) { _state.value = VoiceState.Error("Microphone permission is required") } catch (_: Exception) { if (!cancelled) _state.value = VoiceState.Error("Microphone unavailable") } } }
    fun stop() { recording?.cancel(); recording = null; if (!cancelled) _state.value = VoiceState.Idle }
    fun cancel() { cancelled = true; recording?.cancel(); recording = null; _state.value = VoiceState.Idle }
    private fun transcribe(pcm: ShortArray) { if (cancelled) return; viewModelScope.launch { _state.value = VoiceState.Transcribing(1); runCatching { upload(WavEncoder.encode(pcm)) }.onSuccess { if (it.isNotBlank() && !cancelled) _append.tryEmit(it.trim()) }.onFailure { if (!cancelled) _state.value = VoiceState.Error("Transcription failed") }; if (!cancelled) _state.value = VoiceState.Listening } }
    private suspend fun upload(wav: ByteArray): String = withContext(Dispatchers.IO) {
        val token = FirebaseAuth.getInstance().currentUser?.let { Tasks.await(it.getIdToken(false)).token } ?: error("Not signed in")
        val boundary = "CherryAIVoice${System.nanoTime()}"; val conn = (URL("https://veltrix-backend-nmvy.onrender.com/v1/transcribe").openConnection() as HttpURLConnection)
        conn.connectTimeout = 20_000; conn.readTimeout = 20_000; conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("Authorization", "Bearer $token"); conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.outputStream.use { it.write("--$boundary\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n".toByteArray()); it.write(wav); it.write("\r\n--$boundary--\r\n".toByteArray()) }
        if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
        val body = conn.inputStream.bufferedReader().readText(); Regex("\\\"text\\\"\\s*:\\s*\\\"(.*?)\\\"").find(body)?.groupValues?.get(1)?.replace("\\n", "\n") ?: ""
    }
    override fun onCleared() { recording?.cancel(); super.onCleared() }
}
