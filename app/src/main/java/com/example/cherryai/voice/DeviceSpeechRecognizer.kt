package com.example.cherryai.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Android SpeechRecognizer fallback when the Groq backend path is unreachable. */
class DeviceSpeechRecognizer(
    private val app: Application,
    private val onFinalText: (String) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onLevel: (Float) -> Unit = {},
    private val onFatalError: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false

    fun start() {
        main.post {
            if (!SpeechRecognizer.isRecognitionAvailable(app)) {
                onFatalError("On-device speech recognition is not available")
                return@post
            }
            active = true
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                setRecognitionListener(listener)
            }
            listen()
        }
    }

    fun stop() {
        main.post {
            active = false
            recognizer?.stopListening()
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun listen() {
        if (!active) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            onLevel(((rmsdB + 2f) / 8f).coerceIn(0f, 1f))
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            if (!active) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> listen()

                SpeechRecognizer.ERROR_CLIENT -> if (active) listen()
                else -> onFatalError(recognizerErrorMessage(error))
            }
        }

        override fun onResults(results: Bundle?) {
            if (!active) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) onFinalText(text.trim())
            listen()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) onPartialText(text.trim())
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun recognizerErrorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out"
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error"
        else -> "Speech recognition failed"
    }
}
