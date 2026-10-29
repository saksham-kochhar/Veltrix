package com.example.cherryai.voice

import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed class TranscriptionFailure(message: String, val useDeviceFallback: Boolean) : Exception(message) {
    class Network(cause: Throwable? = null) :
        TranscriptionFailure(cause?.message ?: "Network unavailable", true)

    class Service(msg: String) : TranscriptionFailure(msg, true)
    class Auth(msg: String) : TranscriptionFailure(msg, false)
    class Rejected(msg: String) : TranscriptionFailure(msg, false)
}

object GroqTranscriptionClient {
    private const val TRANSCRIBE_URL = "https://veltrix-backend-nmvy.onrender.com/v1/transcribe"

    fun transcribe(wav: ByteArray): String {
        val token = FirebaseAuth.getInstance().currentUser?.let {
            Tasks.await(it.getIdToken(false)).token
        } ?: throw TranscriptionFailure.Auth("Sign in to use voice input")

        val boundary = "CherryAIVoice${System.nanoTime()}"
        val conn = (URL(TRANSCRIBE_URL).openConnection() as HttpURLConnection)
        try {
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.outputStream.use { out ->
                out.write(
                    (
                        "--$boundary\r\n" +
                            "Content-Disposition: form-data; name=\"audio\"; filename=\"speech.wav\"\r\n" +
                            "Content-Type: audio/wav\r\n\r\n"
                        ).toByteArray(),
                )
                out.write(wav)
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.readText()
                .orEmpty()

            when (code) {
                in 200..299 -> {
                    val text = runCatching { JSONObject(body).optString("text", "") }.getOrDefault("")
                    return text
                }
                401, 403 -> throw TranscriptionFailure.Auth("Voice input requires a signed-in account")
                404 -> throw TranscriptionFailure.Service(
                    "Voice transcription is not deployed on the server yet — redeploy the backend",
                )
                503, 502, 504, 429 -> throw TranscriptionFailure.Service(
                    JSONObject(body).optString("error", "Transcription service unavailable"),
                )
                else -> throw TranscriptionFailure.Rejected(
                    JSONObject(body).optString("error", "Transcription failed (HTTP $code)"),
                )
            }
        } catch (e: TranscriptionFailure) {
            throw e
        } catch (e: IOException) {
            throw TranscriptionFailure.Network(e)
        } finally {
            conn.disconnect()
        }
    }
}
