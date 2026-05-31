package com.example.veltrix


import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class veltrixviewmodel : ViewModel(){

    var email by mutableStateOf("")

    val messagelist by lazy {
        mutableStateListOf<Response>()
    }
    var loading by mutableStateOf(false)

    var instruction by mutableStateOf(Instruction.normal)

    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)
    var isModelDownloaded by mutableStateOf(false)

    var OnlineMode by mutableStateOf(true)



    fun sendmessageboth(question: String) {
        if (OnlineMode == false ) {
            sendMessageOffline(question)
        } else {
            sendmessage(question)
        }
    }
    fun sendmessage(question: String) {
        viewModelScope.launch {
            loading = true
            try {
                messagelist.add(Response(question, "User"))

                val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
                    ?: throw Exception("Not logged in")

                val reply = withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://veltrix-backend-production.up.railway.app/chat")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $idToken")
                    connection.doOutput = true

                    val requestBody = """{"message": "${question.replace("\"", "\\\"")}"}"""
                    connection.outputStream.write(requestBody.toByteArray())

                    val responseCode = connection.responseCode
                    when (responseCode) {
                        200 -> {
                            val response = connection.inputStream.bufferedReader().readText()
                            org.json.JSONObject(response).getString("reply")
                        }
                        429 -> "You have reached your free limit. You can still Use offline Model"
                        else -> {
                            val error = connection.errorStream?.bufferedReader()?.readText()
                            "Error $responseCode: $error"
                        }
                    }
                }

                messagelist.add(Response(reply, "Model"))

            } catch (e: Exception) {
                messagelist.add(Response("Error: ${e.message}", "Model"))
            } finally {
                loading = false
            }
        }
    }

    //Authentication

    val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val _authstate = MutableLiveData<Authstate>()
    val authstate : MutableLiveData<Authstate> = _authstate

    init {
        checkauthstate()
    }

    fun checkauthstate() {
        val user = auth.currentUser
        when {
            user == null -> _authstate.value = Authstate.Unauthenticated
            !user.isEmailVerified -> _authstate.value = Authstate.VerificationSent
            else -> _authstate.value = Authstate.Authenticated
        }
    }


    fun login(email: String, password: String) {
        _authstate.value = Authstate.Loading
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    createUserProfileIfNotExists(user.uid, email)
                    _authstate.value = Authstate.Authenticated
                } else {
                    auth.signOut()
                    _authstate.value = Authstate.EmailNotVerified
                }
            } else {
                _authstate.value = Authstate.Error(task.exception?.message ?: "Something Went Wrong!")
            }
        }
    }

    fun signup(email: String, password: String) {
        _authstate.value = Authstate.Loading
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                    if (verifyTask.isSuccessful) {
                        _authstate.value = Authstate.VerificationSent
                    } else {
                        _authstate.value = Authstate.Error("Could not send verification email")
                    }
                }
            } else {
                val message = when {
                    task.exception?.message?.contains("already in use") == true ->
                        "An account with this email already exists"
                    task.exception?.message?.contains("badly formatted") == true ->
                        "Please enter a valid email address"
                    task.exception?.message?.contains("weak password") == true ->
                        "Password must be at least 8 characters"
                    else -> task.exception?.message ?: "Something went wrong"
                }
                _authstate.value = Authstate.Error(message)
            }
        }
    }

    fun resendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _authstate.value = Authstate.VerificationSent
            } else {
                _authstate.value = Authstate.Error("Failed to resend. Try logging in again.")
            }
        }
    }

    fun signout() {
        auth.signOut()
        _authstate.value = Authstate.Unauthenticated
    }
    fun resetState() {
        _authstate.value = Authstate.Unauthenticated
    }

    private fun createUserProfileIfNotExists(uid: String, email: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                userRef.set(
                    mapOf(
                        "email" to email,
                        "plan" to "free",
                        "callsUsed" to 0,
                        "callsLimit" to 15
                    )
                )
            }
        }
    }

    fun openEmailApp(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No email app found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startVerificationPolling() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    auth.currentUser?.reload()?.await()
                    if (auth.currentUser?.isEmailVerified == true) {
                        createUserProfileIfNotExists(auth.currentUser!!.uid, email)
                        _authstate.value = Authstate.Authenticated
                        break
                    }
                } catch (e: Exception) {
                }
            }
        }
    }


    //Offline Model

    fun checkModelDownloaded(context: Context) {
        val file = File(context.filesDir, "qwen2.5-0.5b-q4.gguf")
        isModelDownloaded = file.exists()
    }

    fun downloadModel(context: Context) {
        viewModelScope.launch {
            isDownloading = true
            withContext(Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    val totalSize = connection.contentLength
                    val input = connection.inputStream
                    val file = File(context.filesDir, "qwen2.5-0.5b-q4.gguf")
                    var downloaded = 0L
                    val buffer = ByteArray(8192)
                    file.outputStream().use { output ->
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes
                            downloadProgress = downloaded.toFloat() / totalSize
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        downloadProgress = 0f
                        File(context.filesDir, "qwen2.5-0.5b-q4.gguf").delete()
                        messagelist.add(Response("Download failed: ${e.message}", "Model"))
                }
            }
            isDownloading = false
            isModelDownloaded = true
        }
    }

}

    private var llmInference: LlmInference? = null

    fun loadLocalModel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelPath = File(context.filesDir, "qwen2.5-0.5b-q4.gguf").absolutePath
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.7f)
                .setRandomSeed(42)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    fun sendMessageOffline(question: String) {
        viewModelScope.launch {
            loading = true
            try {
                messagelist.add(Response(question, "User"))
                val reply = withContext(Dispatchers.IO) {
                    llmInference?.generateResponse(question) ?: "Model not loaded"
                }
                messagelist.add(Response(reply, "Model"))
            } catch (e: Exception) {
                messagelist.add(Response("Error: ${e.message}", "Model"))
            } finally {
                loading = false
            }
        }
    }
}
sealed class Authstate{
    object Authenticated : Authstate()
    object Unauthenticated : Authstate()
    object Loading : Authstate()
    data class Error(val message : String) : Authstate()
    object VerificationSent : Authstate()
    object EmailNotVerified : Authstate()
}