package com.example.veltrix


import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class veltrixviewmodel : ViewModel(){
    var firstname by mutableStateOf("")
    var lastname by mutableStateOf("")

    var email by mutableStateOf("")

    val messagelist by lazy {
        mutableStateListOf<Response>()
    }
    var loading by mutableStateOf(false)

    var instruction by mutableStateOf(Instruction.normal)

    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)
    var isModelDownloaded by mutableStateOf(false)
        private set

    var OnlineMode by mutableStateOf(true)


    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun String.escapeJson(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")


    fun sendmessage(question: String) {
        viewModelScope.launch {
            loading = true
            try {
                messagelist.add(Response(question, "User"))

                val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
                    ?: throw Exception("Not logged in")

                val reply = withContext(Dispatchers.IO) {

                    val history = messagelist
                        .dropLast(1)
                        .takeLast(10)
                        .map {
                            val role = if (it.Role == "User") "user" else "model"
                            """{"role":"$role","content":"${it.message.escapeJson()}"}"""
                        }
                        .joinToString(",")

                    val requestBody = """
                        {
                        "message": "${question.escapeJson()}",
                        "history": [$history]
                        }
                        """.trimIndent()

                    val url = java.net.URL("https://veltrix-backend-production.up.railway.app/chat")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $idToken")
                    connection.doOutput = true
                    connection.outputStream.write(requestBody.toByteArray())

                    when (connection.responseCode) {
                        200 -> {
                            val response = connection.inputStream.bufferedReader().readText()
                            org.json.JSONObject(response).getString("reply")
                        }
                        429 -> "You have reached your free limit. You can still use offline model"
                        else -> {
                            val error = connection.errorStream?.bufferedReader()?.readText()
                            "Error ${connection.responseCode}: $error"
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
            else -> {
                _authstate.value = Authstate.Loading  // show loading while Firestore fetches
                loadOrCreateUserProfile(user.uid, user.email ?: "")
            }
        }
    }


    fun login(email: String, password: String) {
        _authstate.value = Authstate.Loading
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    loadOrCreateUserProfile(user.uid, email)
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

    private fun loadOrCreateUserProfile(uid: String, email: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                _userProfile.value = profile
                _authstate.value = if (profile?.onboardcomplete == true)
                    Authstate.Authenticated else Authstate.ProfileIncomplete
            } else {
                val newProfile = UserProfile(email = email, firstname = firstname, lastname = lastname)
                userRef.set(newProfile)
                    .addOnSuccessListener {
                        _userProfile.value = newProfile
                        _authstate.value = Authstate.ProfileIncomplete
                    }
                    .addOnFailureListener {
                        _userProfile.value = null
                        _authstate.value = Authstate.Error("Failed to load profile")
                    }
            }
        }.addOnFailureListener {
            _userProfile.value = null
            _authstate.value = Authstate.Error("Failed to load profile")
        }
    }

    fun completeOnboarding(plan: String, onDone: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update(mapOf(
                "firstname" to firstname,
                "lastname" to lastname,
                "plan" to plan,
                "onboardcomplete" to true
            ))
            .addOnSuccessListener {
                _userProfile.value = _userProfile.value?.copy(
                    firstname = firstname, lastname = lastname,
                    plan = plan, onboardcomplete = true
                )
                _authstate.value = Authstate.Authenticated
                onDone()
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
                        loadOrCreateUserProfile(auth.currentUser!!.uid, email)

                        break
                    }
                } catch (e: Exception) {
                }
            }
        }
    }


    //Offline Model

    fun refreshModelStatus(context: Context) {
        val file = File(context.filesDir, "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task")

        val isValid = file.exists() && file.length() > 1_400_000_000L

        if (file.exists() && !isValid) {
            file.delete()
        }

        isModelDownloaded = isValid

        if (isValid && llmInference == null) {
            loadLocalModel(context)
        }
    }

    fun downloadModel(context: Context) {
        viewModelScope.launch {
            isDownloading = true
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task")
                try {
                    val url = java.net.URL("https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    val totalSize = connection.contentLength
                    val input = connection.inputStream
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
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        isModelDownloaded = true
                    }
                    loadLocalModel(context)

                } catch (e: Exception) {
                    if (file.exists()) file.delete()
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        downloadProgress = 0f
                        isModelDownloaded = false
                        messagelist.add(Response("Download failed: ${e.message}", "Model"))
                    }
                }
            }
        }
    }

    private var llmInference: LlmInference? = null

    fun loadLocalModel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val modelPath =
                    File(context.filesDir, "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task").absolutePath

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)

            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    messagelist.add(
                        Response(
                            "Offline model is not compatible with MediaPipe.",
                            "Model"
                        )
                    )
                }

                return@launch
            }
        }
    }

    fun sendMessageOffline(question: String) {
        if (question.isBlank() || loading) return

        viewModelScope.launch {
            loading = true
            try {
                messagelist.add(Response(question, "User"))

                val model = llmInference
                if (model == null) {
                    messagelist.add(Response("Model not loaded", "Model"))
                    return@launch
                }

                val history = messagelist
                    .takeLast(10)
                    .dropLast(1)
                    .joinToString("\n") {
                        if (it.Role == "User") "User: ${it.message}"
                        else "Assistant: ${it.message}"
                    }

                val prompt = if (history.isNotEmpty()) {
                    "$history\nUser: $question\nAssistant:"
                } else {
                    "User: $question\nAssistant:"
                }

                val reply = withContext(Dispatchers.IO) {
                    model.generateResponse(prompt)
                }

                messagelist.add(Response(reply, "Model"))

            } catch (e: Exception) {
                messagelist.add(Response("Error: ${e.localizedMessage}", "Model"))
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
    object ProfileIncomplete : Authstate()
}

data class UserProfile(
    val email: String = "",
    val firstname: String = "",
    val lastname : String = "",
    val plan: String = "",
    val callsUsed: Int = 0,
    val callsLimit: Int = 15,
    val onboardcomplete : Boolean = false
)
