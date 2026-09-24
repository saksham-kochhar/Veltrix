package com.example.veltrix


import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veltrix.chathistorry.COMPACTION_WINDOW
import com.example.veltrix.chathistorry.CONTEXT_SUMMARY_MAX_WORDS
import com.example.veltrix.chathistorry.ChatHistoryStore
import com.example.veltrix.chathistorry.ChatSession
import com.example.veltrix.chathistorry.ChatSessionSummary
import com.example.veltrix.chathistorry.PROFILE_CHAT_BATCH
import com.example.veltrix.chathistorry.PROFILE_SUMMARY_MAX_WORDS
import com.example.veltrix.chathistorry.SUMMARY_INSTRUCTION
import com.example.veltrix.chathistorry.TITLE_INSTRUCTION
import com.example.veltrix.chathistorry.buildInitialTitlePrompt
import com.example.veltrix.chathistorry.buildRefinedTitlePrompt
import com.example.veltrix.chathistorry.buildSummaryPrompt
import com.example.veltrix.chathistorry.clampWords
import com.example.veltrix.chathistorry.compactionChunk
import com.example.veltrix.chathistorry.conversationText
import com.example.veltrix.chathistorry.countUserMessages
import com.example.veltrix.chathistorry.firstUserMessage
import com.example.veltrix.chathistorry.heuristicTitle
import com.example.veltrix.chathistorry.messagesUpToNthUser
import com.example.veltrix.chathistorry.needsInitialTitle
import com.example.veltrix.chathistorry.sanitizeSummary
import com.example.veltrix.chathistorry.sanitizeTitle
import com.example.veltrix.chathistorry.shouldCompact
import com.example.veltrix.chathistorry.shouldRefreshProfile
import com.example.veltrix.chathistorry.toSummary
import com.example.veltrix.chathistorry.unsummarizedTail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
import java.util.UUID

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

    /** OpenRouter model id; server enforces tier allow-lists. */
    var selectedModelId by mutableStateOf(AiModels.DEFAULT_ONLINE_ID)

    var walletSnapshot by mutableStateOf(WalletSnapshot())
        private set

    /** null = not loaded yet; use static catalog until set. */
    var allowedOnlineModelIds by mutableStateOf<Set<String>?>(null)
        private set

    companion object {
        private const val BACKEND_BASE = "https://veltrix-backend-nmvy.onrender.com"
        private const val TAG = "ChatHistorySync"
    }

    var currentSessionId by mutableStateOf(UUID.randomUUID().toString())
        private set

    private var sessionCreatedAt: Long = System.currentTimeMillis()
    private var compactedSummary by mutableStateOf("")
    private var summarizedUntilMessage by mutableStateOf(0)
    private var messagesSinceSummary by mutableStateOf(0)
    private var profileSummary by mutableStateOf("")
    private var summarizedChatCount by mutableStateOf(0)
    private var profileBatchInFlight = false

    val sessionSummaries = mutableStateListOf<ChatSessionSummary>()

    var historyStatusMessage by mutableStateOf<String?>(null)

    private var historyAppContext: Context? = null
    private var chatHistoryInitialized = false

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun fetchWallet() {
        refreshOnlineAccount()
    }

    fun refreshOnlineAccount() {
        viewModelScope.launch {
            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token ?: return@launch
            withContext(Dispatchers.IO) {
                runCatching {
                    val wallet = fetchWalletJson(idToken)
                    val models = fetchModelsJson(idToken)
                    Pair(wallet, models)
                }.getOrNull()
            }?.let { (wallet, modelIds) ->
                walletSnapshot = wallet
                allowedOnlineModelIds = modelIds
                if (OnlineMode && modelIds.isNotEmpty() && selectedModelId !in modelIds) {
                    selectedModelId = modelIds.firstOrNull { it == AiModels.DEFAULT_ONLINE_ID }
                        ?: modelIds.first()
                }
            }
        }
    }

    private fun fetchWalletJson(idToken: String): WalletSnapshot {
        val url = java.net.URL("$BACKEND_BASE/v1/wallet")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $idToken")
        if (connection.responseCode != 200) {
            throw IllegalStateException("wallet ${connection.responseCode}")
        }
        val body = connection.inputStream.bufferedReader().readText()
        return WalletSnapshot.fromJson(org.json.JSONObject(body))
    }

    private fun fetchModelsJson(idToken: String): Set<String> {
        val url = java.net.URL("$BACKEND_BASE/v1/models")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $idToken")
        if (connection.responseCode != 200) {
            throw IllegalStateException("models ${connection.responseCode}")
        }
        val body = connection.inputStream.bufferedReader().readText()
        val root = org.json.JSONObject(body)
        val arr = root.optJSONArray("models") ?: return emptySet()
        val ids = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val id = arr.optJSONObject(i)?.optString("id") ?: continue
            if (id.isNotBlank()) ids.add(id)
        }
        return ids
    }

    private fun applyWalletFromChatJson(response: String) {
        runCatching {
            val obj = org.json.JSONObject(response)
            walletSnapshot = WalletSnapshot.fromJson(obj)
        }
    }

    fun String.escapeJson(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    fun initChatHistory(context: Context) {
        historyAppContext = context.applicationContext
        val uid = auth.currentUser?.uid ?: return
        if (chatHistoryInitialized) return
        chatHistoryInitialized = true
        viewModelScope.launch {
            val appContext = context.applicationContext
            val local = withContext(Dispatchers.IO) {
                ChatHistoryStore.loadLocal(appContext, uid)
            }
            mergeSummaries(local.map { it.toSummary(cachedLocally = true) })

            try {
                val snapshot = withContext(Dispatchers.IO) {
                    ChatHistoryStore.chatsCollection(uid)
                        .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()
                }
                val localIds = local.map { it.id }.toSet()
                val remote = snapshot.documents.map { doc ->
                    val data = (doc.data ?: emptyMap()).toMutableMap()
                    data["id"] = doc.id
                    ChatHistoryStore.summaryFromMap(
                        data,
                        cachedLocally = localIds.contains(doc.id)
                    )
                }
                mergeSummaries(remote)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load chat list from Firestore", e)
                historyStatusMessage = "Showing local chats only"
            }
        }
    }

    fun startNewChat(context: Context) {
        val appContext = context.applicationContext
        historyAppContext = appContext
        viewModelScope.launch {
            if (messagelist.isNotEmpty()) {
                finalizeAndPersistSession(appContext)
            }
            resetSessionState()
        }
    }

    private fun resetSessionState() {
        messagelist.clear()
        currentSessionId = UUID.randomUUID().toString()
        sessionCreatedAt = System.currentTimeMillis()
        compactedSummary = ""
        summarizedUntilMessage = 0
        messagesSinceSummary = 0
        historyStatusMessage = null
    }

    fun openSession(context: Context, sessionId: String) {
        if (sessionId == currentSessionId) {
            historyStatusMessage = null
            return
        }
        val appContext = context.applicationContext
        historyAppContext = appContext
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            if (messagelist.isNotEmpty()) {
                finalizeAndPersistSession(appContext)
            }

            val localSession = withContext(Dispatchers.IO) {
                ChatHistoryStore.findLocal(appContext, uid, sessionId)
            }
            if (localSession != null) {
                applySession(localSession)
                return@launch
            }

            try {
                val doc = withContext(Dispatchers.IO) {
                    ChatHistoryStore.chatsCollection(uid).document(sessionId).get().await()
                }
                if (!doc.exists()) {
                    historyStatusMessage = "Chat not found"
                    return@launch
                }
                val data = (doc.data ?: emptyMap()).toMutableMap()
                data["id"] = doc.id
                val session = ChatHistoryStore.sessionFromMap(data)
                withContext(Dispatchers.IO) {
                    ChatHistoryStore.upsertLocal(appContext, uid, session)
                }
                applySession(session)
                val localIds = withContext(Dispatchers.IO) {
                    ChatHistoryStore.loadLocal(appContext, uid).map { it.id }.toSet()
                }
                applyLocalFlags(localIds)
            } catch (_: Exception) {
                historyStatusMessage = "Connect to load this chat"
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Connect to load this chat", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun persistCurrentSession(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        if (messagelist.isEmpty()) return
        val appContext = context.applicationContext
        historyAppContext = appContext

        viewModelScope.launch {
            persistSessionSnapshot(appContext, uid, finalizeForProfile = false)
        }
    }

    private suspend fun finalizeAndPersistSession(appContext: Context) {
        val uid = auth.currentUser?.uid ?: return
        if (messagelist.isEmpty()) return
        persistSessionSnapshot(appContext, uid, finalizeForProfile = true)
    }

    private suspend fun persistSessionSnapshot(
        appContext: Context,
        uid: String,
        finalizeForProfile: Boolean
    ) {
        val messages = messagelist.toList()
        val existing = withContext(Dispatchers.IO) {
            ChatHistoryStore.findLocal(appContext, uid, currentSessionId)
        }
        val metadata = resolveSessionMetadata(existing, messages)
        if (finalizeForProfile && OnlineMode) {
            maybeCompactContext()
        }
        var finalSummary = metadata.summary.ifBlank { compactedSummary }
        if (finalizeForProfile && finalSummary.isBlank() && messages.isNotEmpty()) {
            val raw = callGeminiMeta(
                task = "compact",
                prompt = conversationText(messages),
                priorSummary = compactedSummary
            )
            if (!raw.isNullOrBlank()) {
                compactedSummary = clampWords(raw, CONTEXT_SUMMARY_MAX_WORDS)
                summarizedUntilMessage = messages.size
                messagesSinceSummary = 0
                finalSummary = compactedSummary
            } else {
                finalSummary = clampWords(conversationText(messages.takeLast(20)), 40)
            }
        } else if (finalizeForProfile) {
            finalSummary = finalSummary.ifBlank { compactedSummary }
        }

        val session = ChatSession(
            id = currentSessionId,
            title = metadata.title,
            summary = finalSummary,
            lastMessage = messages.lastOrNull()?.message.orEmpty(),
            updatedAt = System.currentTimeMillis(),
            titleRefined = metadata.titleRefined,
            compactedSummary = compactedSummary.ifBlank { finalSummary },
            summarizedUntilMessage = summarizedUntilMessage,
            messagesSinceSummary = messagesSinceSummary,
            createdAt = existing?.createdAt?.takeIf { it > 0 } ?: sessionCreatedAt,
            messages = messages
        )

        val syncError = withContext(Dispatchers.IO) {
            ChatHistoryStore.upsertLocal(appContext, uid, session)
            ChatHistoryStore.syncSessionToFirestore(uid, session)
        }
        if (syncError != null) {
            Log.e(TAG, "Firestore sync failed for session ${session.id}: $syncError")
            historyStatusMessage = syncError
            Toast.makeText(appContext, syncError, Toast.LENGTH_LONG).show()
        } else {
            historyStatusMessage = null
        }
        upsertSummary(session.toSummary(cachedLocally = true))
        val localIds = withContext(Dispatchers.IO) {
            ChatHistoryStore.loadLocal(appContext, uid).map { it.id }.toSet()
        }
        applyLocalFlags(localIds)

        if (finalizeForProfile && session.compactedSummary.isNotBlank()) {
            maybeUpdateProfileSummary(uid, session.compactedSummary)
        }
    }

    private data class SessionMetadata(
        val title: String,
        val summary: String,
        val titleRefined: Boolean
    )

    private suspend fun resolveSessionMetadata(
        existing: ChatSession?,
        messages: List<Response>
    ): SessionMetadata {
        var title = existing?.title.orEmpty()
        var summary = existing?.summary.orEmpty()
        var titleRefined = existing?.titleRefined ?: false
        val userCount = countUserMessages(messages)

        if (userCount >= 1 && needsInitialTitle(title)) {
            val firstMsg = firstUserMessage(messages)
            title = generateTitle(
                instruction = TITLE_INSTRUCTION,
                prompt = buildInitialTitlePrompt(firstMsg),
                fallback = heuristicTitle(firstMsg)
            )
        }

        if (userCount >= 10 && !titleRefined) {
            val excerpt = messagesUpToNthUser(messages, 10)
            val refined = generateTitle(
                instruction = TITLE_INSTRUCTION,
                prompt = buildRefinedTitlePrompt(excerpt),
                fallback = title.ifBlank { heuristicTitle(firstUserMessage(messages)) }
            )
            if (refined.isNotBlank()) title = refined
            titleRefined = true
        }

        if (userCount >= 20 && summary.isBlank()) {
            val excerpt = messagesUpToNthUser(messages, 20)
            summary = generateSummary(
                prompt = buildSummaryPrompt(excerpt)
            )
        }

        if (title.isBlank()) title = "New chat"
        return SessionMetadata(title = title, summary = summary, titleRefined = titleRefined)
    }

    private suspend fun generateTitle(
        instruction: String,
        prompt: String,
        fallback: String
    ): String {
        val raw = if (OnlineMode) {
            callGeminiMeta(task = "title", prompt = "$instruction\n\n$prompt")
        } else {
            callOfflineMeta("$instruction\n\n$prompt")
        } ?: return fallback
        val cleaned = sanitizeTitle(raw)
        return cleaned.ifBlank { fallback }
    }

    private suspend fun generateSummary(prompt: String): String {
        val raw = if (OnlineMode) {
            callGeminiMeta(
                task = "compact",
                prompt = prompt,
                priorSummary = ""
            )
        } else {
            callOfflineMeta("$SUMMARY_INSTRUCTION\n\n$prompt")
        } ?: return ""
        return sanitizeSummary(raw)
    }

    private suspend fun callGeminiMeta(
        task: String,
        prompt: String,
        priorSummary: String? = null
    ): String? {
        return try {
            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token ?: return null
            withContext(Dispatchers.IO) {
                val priorJson = if (priorSummary.isNullOrBlank()) {
                    "null"
                } else {
                    "\"${priorSummary.escapeJson()}\""
                }
                val requestBody = """
                    {
                    "task": "${task.escapeJson()}",
                    "prompt": "${prompt.escapeJson()}",
                    "prior_summary": $priorJson
                    }
                    """.trimIndent()

                val url = java.net.URL("https://veltrix-backend-nmvy.onrender.com/v1/meta")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.doOutput = true
                connection.outputStream.write(requestBody.toByteArray())

                when (connection.responseCode) {
                    200 -> {
                        val response = connection.inputStream.bufferedReader().readText()
                        org.json.JSONObject(response).getString("text").trim()
                    }
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun maybeCompactContext() {
        if (!OnlineMode) return
        if (!shouldCompact(messagesSinceSummary)) return
        val messages = messagelist.toList()
        val chunk = compactionChunk(messages, summarizedUntilMessage)
        if (chunk.size < COMPACTION_WINDOW) return

        val chunkText = conversationText(chunk)
        val updated = callGeminiMeta(
            task = "compact",
            prompt = chunkText,
            priorSummary = compactedSummary
        ) ?: return

        compactedSummary = clampWords(updated, CONTEXT_SUMMARY_MAX_WORDS)
        summarizedUntilMessage += chunk.size
        messagesSinceSummary = (messages.size - summarizedUntilMessage).coerceAtLeast(0)
    }

    private suspend fun maybeUpdateProfileSummary(uid: String, chatSummary: String) {
        if (profileBatchInFlight || chatSummary.isBlank()) return
        val nextCount = summarizedChatCount + 1
        summarizedChatCount = nextCount

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "summarizedChatCount" to nextCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // still try local profile refresh below if batch ready
        }

        if (!shouldRefreshProfile(nextCount)) {
            _userProfile.value = _userProfile.value?.copy(summarizedChatCount = nextCount)
            return
        }

        profileBatchInFlight = true
        try {
            val recent = (
                listOf(chatSummary) + sessionSummaries
                    .sortedByDescending { it.updatedAt }
                    .mapNotNull { s ->
                        s.compactedSummary.ifBlank { s.summary }.takeIf { it.isNotBlank() }
                    }
                )
                .distinct()
                .take(PROFILE_CHAT_BATCH)
            if (recent.isEmpty()) return

            val merged = callGeminiMeta(
                task = "profile",
                prompt = recent.joinToString("\n---\n")
            ) ?: return
            val profile = clampWords(merged, PROFILE_SUMMARY_MAX_WORDS)
            profileSummary = profile
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "profileSummary" to profile,
                        "summarizedChatCount" to nextCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
            }
            _userProfile.value = _userProfile.value?.copy(
                profileSummary = profile,
                summarizedChatCount = nextCount
            )
        } finally {
            profileBatchInFlight = false
        }
    }

    private suspend fun callOfflineMeta(prompt: String): String? {
        val model = llmInference ?: return null
        return try {
            withContext(Dispatchers.IO) {
                model.generateResponse(prompt).trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applySession(session: ChatSession) {
        currentSessionId = session.id
        sessionCreatedAt = session.createdAt.takeIf { it > 0 } ?: session.updatedAt
        compactedSummary = session.compactedSummary
        summarizedUntilMessage = session.summarizedUntilMessage.coerceIn(0, session.messages.size)
        messagelist.clear()
        messagelist.addAll(session.messages)
        messagesSinceSummary = (session.messages.size - summarizedUntilMessage).coerceAtLeast(0)
        historyStatusMessage = null
        upsertSummary(session.toSummary(cachedLocally = true))
    }

    private fun mergeSummaries(incoming: List<ChatSessionSummary>) {
        val byId = linkedMapOf<String, ChatSessionSummary>()
        sessionSummaries.forEach { byId[it.id] = it }
        incoming.forEach { next ->
            val prev = byId[next.id]
            byId[next.id] = if (prev == null) next else next.copy(
                cachedLocally = prev.cachedLocally || next.cachedLocally,
                title = if (next.updatedAt >= prev.updatedAt) next.title else prev.title,
                summary = if (next.updatedAt >= prev.updatedAt) next.summary else prev.summary,
                compactedSummary = if (next.updatedAt >= prev.updatedAt) next.compactedSummary else prev.compactedSummary,
                lastMessage = if (next.updatedAt >= prev.updatedAt) next.lastMessage else prev.lastMessage,
                updatedAt = maxOf(prev.updatedAt, next.updatedAt)
            )
        }
        sessionSummaries.clear()
        sessionSummaries.addAll(byId.values.sortedByDescending { it.updatedAt })
    }

    private fun upsertSummary(summary: ChatSessionSummary) {
        val index = sessionSummaries.indexOfFirst { it.id == summary.id }
        if (index >= 0) sessionSummaries[index] = summary else sessionSummaries.add(summary)
        sessionSummaries.sortByDescending { it.updatedAt }
    }

    private fun applyLocalFlags(localIds: Set<String>) {
        for (i in sessionSummaries.indices) {
            val s = sessionSummaries[i]
            sessionSummaries[i] = s.copy(cachedLocally = localIds.contains(s.id))
        }
    }

    private fun clearChatHistoryState() {
        messagelist.clear()
        sessionSummaries.clear()
        currentSessionId = UUID.randomUUID().toString()
        sessionCreatedAt = System.currentTimeMillis()
        compactedSummary = ""
        summarizedUntilMessage = 0
        messagesSinceSummary = 0
        profileSummary = ""
        summarizedChatCount = 0
        chatHistoryInitialized = false
        historyStatusMessage = null
        historyAppContext = null
    }

    fun switchToOfflineMode(context: Context) {
        OnlineMode = false
        refreshModelStatus(context)
        if (isModelDownloaded) {
            loadLocalModel(context)
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
                    val prior = messagelist.dropLast(1)
                    val tail = unsummarizedTail(prior, summarizedUntilMessage)
                    val history = tail
                        .map {
                            val role = if (it.Role == "User") "user" else "model"
                            """{"role":"$role","content":"${it.message.escapeJson()}"}"""
                        }
                        .joinToString(",")

                    val profileJson = if (profileSummary.isBlank()) {
                        "null"
                    } else {
                        "\"${profileSummary.escapeJson()}\""
                    }
                    val compactedJson = if (compactedSummary.isBlank()) {
                        "null"
                    } else {
                        "\"${compactedSummary.escapeJson()}\""
                    }

                    val requestBody = """
                        {
                        "message": "${question.escapeJson()}",
                        "history": [$history],
                        "instruction": "${instruction.escapeJson()}",
                        "model": "${selectedModelId.escapeJson()}",
                        "profile_summary": $profileJson,
                        "compacted_summary": $compactedJson
                        }
                        """.trimIndent()

                    val url = java.net.URL("$BACKEND_BASE/chat")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $idToken")
                    connection.doOutput = true
                    connection.outputStream.write(requestBody.toByteArray())

                    when (connection.responseCode) {
                        200 -> {
                            val response = connection.inputStream.bufferedReader().readText()
                            applyWalletFromChatJson(response)
                            org.json.JSONObject(response).getString("reply")
                        }
                        402, 403 -> {
                            val error = connection.errorStream?.bufferedReader()?.readText()
                            val detail = error?.let {
                                runCatching { org.json.JSONObject(it).optString("detail") }.getOrNull()
                            }
                            detail?.takeIf { it.isNotBlank() }
                                ?: "Allowance exhausted or model not on your plan. Upgrade to continue."
                        }
                        429 -> LIMIT_EXHAUSTED_MESSAGE
                        else -> {
                            val error = connection.errorStream?.bufferedReader()?.readText()
                            "Error ${connection.responseCode}: $error"
                        }
                    }
                }

                messagelist.add(Response(reply, "Model"))
                messagesSinceSummary =
                    (messagelist.size - summarizedUntilMessage).coerceAtLeast(0)
                maybeCompactContext()
                historyAppContext?.let { persistCurrentSession(it) }

            } catch (e: Exception) {
                messagelist.add(Response("Error: ${e.message}", "Model"))
                historyAppContext?.let { persistCurrentSession(it) }
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
                email = user.email ?: ""
                loadOrCreateUserProfile(user.uid, email)
            }
        }
    }


    fun login(email: String, password: String) {
        _authstate.value = Authstate.Loading
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    this.email = user.email ?: email
                    loadOrCreateUserProfile(user.uid, this.email)
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
        val ctx = historyAppContext
        viewModelScope.launch {
            if (ctx != null && messagelist.isNotEmpty()) {
                finalizeAndPersistSession(ctx)
            }
            auth.signOut()
            clearChatHistoryState()
            _userProfile.value = null
            _authstate.value = Authstate.Unauthenticated
        }
    }
    fun resetState() {
        _authstate.value = Authstate.Unauthenticated
    }

    fun setError(message: String) {
        _authstate.value = Authstate.Error(message)
    }

    fun signInWithGoogle(idToken: String) {
        _authstate.value = Authstate.Loading
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser ?: return@addOnCompleteListener
                email = user.email ?: ""
                // Pre-fill name from Google profile so the onboarding fields aren't blank
                val nameParts = user.displayName?.trim()?.split(" ") ?: emptyList()
                if (nameParts.isNotEmpty() && firstname.isBlank()) firstname = nameParts.first()
                if (nameParts.size > 1 && lastname.isBlank()) lastname = nameParts.drop(1).joinToString(" ")
                loadOrCreateUserProfile(user.uid, email)
            } else {
                _authstate.value = Authstate.Error(
                    task.exception?.message ?: "Google sign-in failed"
                )
            }
        }
    }

    private fun loadOrCreateUserProfile(uid: String, email: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                _userProfile.value = profile
                profileSummary = profile?.profileSummary.orEmpty()
                summarizedChatCount = profile?.summarizedChatCount ?: 0
                fetchWallet()
                _authstate.value = if (profile?.onboardcomplete == true)
                    Authstate.Authenticated else Authstate.ProfileIncomplete
            } else {
                val newProfile = UserProfile(email = email, firstname = firstname, lastname = lastname)
                userRef.set(newProfile)
                    .addOnSuccessListener {
                        _userProfile.value = newProfile
                        profileSummary = ""
                        summarizedChatCount = 0
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

                val conversation = if (history.isNotEmpty()) {
                    "$history\nUser: $question\nAssistant:"
                } else {
                    "User: $question\nAssistant:"
                }
                val prompt = "${instruction.trim()}\n\n$conversation"

                val reply = withContext(Dispatchers.IO) {
                    model.generateResponse(prompt)
                }

                messagelist.add(Response(reply, "Model"))
                historyAppContext?.let { persistCurrentSession(it) }

            } catch (e: Exception) {
                messagelist.add(Response("Error: ${e.localizedMessage}", "Model"))
                historyAppContext?.let { persistCurrentSession(it) }
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
    val onboardcomplete : Boolean = false,
    val selectedplan : String = "",
    val profileSummary: String = "",
    val summarizedChatCount: Int = 0
)
