package com.example.cherryai


import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Hub
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cherryai.chathistorry.COMPACTION_WINDOW
import com.example.cherryai.chathistorry.CONTEXT_SUMMARY_MAX_WORDS
import com.example.cherryai.chathistorry.ChatHistoryStore
import com.example.cherryai.chathistorry.ChatSession
import com.example.cherryai.chathistorry.ChatSessionSummary
import com.example.cherryai.chathistorry.PROFILE_CHAT_BATCH
import com.example.cherryai.chathistorry.PROFILE_MERGE_INSTRUCTION
import com.example.cherryai.chathistorry.PROFILE_SUMMARY_MAX_WORDS
import com.example.cherryai.chathistorry.profileContextForNewChat
import com.example.cherryai.chathistorry.buildStructuredProfileContext
import com.example.cherryai.chathistorry.SUMMARY_INSTRUCTION
import com.example.cherryai.chathistorry.TITLE_INSTRUCTION
import com.example.cherryai.chathistorry.buildInitialTitlePrompt
import com.example.cherryai.chathistorry.buildRefinedTitlePrompt
import com.example.cherryai.chathistorry.buildSummaryPrompt
import com.example.cherryai.chathistorry.clampWords
import com.example.cherryai.chathistorry.compactionChunk
import com.example.cherryai.chathistorry.conversationText
import com.example.cherryai.chathistorry.countUserMessages
import com.example.cherryai.chathistorry.firstUserMessage
import com.example.cherryai.chathistorry.heuristicTitle
import com.example.cherryai.chathistorry.messagesUpToNthUser
import com.example.cherryai.chathistorry.needsInitialTitle
import com.example.cherryai.chathistorry.sanitizeSummary
import com.example.cherryai.chathistorry.sanitizeTitle
import com.example.cherryai.chathistorry.shouldCompact
import com.example.cherryai.chathistorry.toSummary
import com.example.cherryai.chathistorry.unsummarizedTail
import com.example.cherryai.security.DeviceSession
import com.example.cherryai.security.LinkedAuthProvider
import com.example.cherryai.security.LoginEvent
import com.example.cherryai.security.SecurityEvent
import com.example.cherryai.security.SecurityRepository
import com.example.cherryai.notifications.AppForegroundHolder
import com.example.cherryai.notifications.LocalNotificationHelper
import com.example.cherryai.notifications.NotificationPrefsCache
import com.example.cherryai.notifications.NotificationPreferencesRepository
import com.example.cherryai.notifications.PushTokenRegistrar
import com.example.cherryai.security.SessionStore
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.util.UUID
import org.json.JSONObject

class cherryAiViewModel : ViewModel(){
    private var profileStorageContext: Context? = null
    private val profilePrefsName = "cherryai_profile_cache"

    fun attachProfileStorage(context: Context) {
        profileStorageContext = context.applicationContext
        auth.currentUser?.let { loadOrCreateUserProfile(it.uid, it.email.orEmpty()) }
    }

    private fun cacheProfile(profile: UserProfile, uid: String) {
        val context = profileStorageContext ?: return
        val json = JSONObject().apply {
            put("email", profile.email)
            put("firstname", profile.firstname)
            put("lastname", profile.lastname)
            put("phoneNumber", profile.phoneNumber)
            put("gender", profile.gender)
            put("age", profile.age)
            put("plan", profile.plan)
            put("onboardcomplete", profile.onboardcomplete)
            put("selectedplan", profile.selectedplan)
            put("profileSummary", profile.profileSummary)
            put("summarizedChatCount", profile.summarizedChatCount)
            profile.planExpiresAt?.let { put("planExpiresAt", it.toDate().time) }
        }
        context.getSharedPreferences(profilePrefsName, Context.MODE_PRIVATE)
            .edit().putString(uid, json.toString()).apply()
    }

    private fun readCachedProfile(uid: String, email: String): UserProfile? {
        val context = profileStorageContext ?: return null
        val raw = context.getSharedPreferences(profilePrefsName, Context.MODE_PRIVATE)
            .getString(uid, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            UserProfile(
                email = email.ifBlank { json.optString("email") },
                firstname = json.optString("firstname"), lastname = json.optString("lastname"),
                phoneNumber = json.optString("phoneNumber"), gender = json.optString("gender"),
                age = json.optInt("age"), plan = json.optString("plan"),
                onboardcomplete = json.optBoolean("onboardcomplete"),
                selectedplan = json.optString("selectedplan"),
                profileSummary = json.optString("profileSummary"),
                summarizedChatCount = json.optInt("summarizedChatCount"),
                planExpiresAt = if (json.has("planExpiresAt")) Timestamp(java.util.Date(json.getLong("planExpiresAt"))) else null
            )
        }.getOrNull()
    }
    data class OfflineModelFile(val name: String, val sizeBytes: Long, val path: String)

    var offlineModelFiles by mutableStateOf<List<OfflineModelFile>>(emptyList())
        private set
    var firstname by mutableStateOf("")
    var lastname by mutableStateOf("")

    var email by mutableStateOf("")

    val messagelist by lazy {
        mutableStateListOf<Response>()
    }
    var loading by mutableStateOf(false)

    /** Next message to send once the user taps Send now while a reply is in flight. */
    var queuedMessage by mutableStateOf<String?>(null)
        private set

    /** User message to resend after picking a replacement model from the unavailable dialog. */
    private var pendingRetryQuestionAfterModelPick: String? = null

    private var chatJob: Job? = null
    private var chatGenerationId = 0

    @Volatile
    private var activeChatConnection: java.net.HttpURLConnection? = null

    var chatMode by mutableStateOf(ChatMode.Normal)
        private set

    var instruction by mutableStateOf(Instruction.forMode(ChatMode.Normal))

    fun selectChatMode(mode: ChatMode) {
        chatMode = mode
        instruction = Instruction.forMode(mode)
    }

    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)
    var isModelDownloaded by mutableStateOf(false)
        private set

    var OnlineMode by mutableStateOf(true)

    /** OpenRouter model id; server enforces tier allow-lists. */
    var selectedModelId by mutableStateOf(AiModels.DEFAULT_ONLINE_ID)

    var walletSnapshot by mutableStateOf(WalletSnapshot())
        private set

    var pendingUsageAlert by mutableStateOf<UsageAlertPrompt?>(null)
        private set

    var pendingChatDialogMessage by mutableStateOf<String?>(null)
        private set

    var pendingChatDialogTitle by mutableStateOf<String?>(null)
        private set

    var modelPickerOpenNonce by mutableStateOf(0)
        private set

    /** Catalog model id -> server allowed (null = not loaded yet). */
    var onlineModelAccess by mutableStateOf<Map<String, Boolean>?>(null)
        private set

    /** Catalog model id -> live on OpenRouter (merged with client health; null = not loaded). */
    var onlineModelAvailability by mutableStateOf<Map<String, Boolean>?>(null)
        private set

    /** Catalog model id -> supports image input according to /v1/models/OpenRouter metadata. */
    var onlineModelImageInputSupport by mutableStateOf<Map<String, Boolean>?>(null)
        private set

    /** Live OpenRouter catalog grouped by provider for the model picker. */
    var onlineProviders by mutableStateOf<List<AiProvider>?>(null)
        private set

    companion object {
        private const val BACKEND_BASE = "https://veltrix-backend-nmvy.onrender.com"
        private const val TAG = "ChatHistorySync"
        private const val GENERIC_CHAT_ERROR = "Something went wrong."
        /** Qwen task bundle: input + output must stay below this (MediaPipe maxTokens). */
        private const val OFFLINE_MODEL_MAX_TOKENS = 512
        private const val OFFLINE_RESERVED_OUTPUT_TOKENS = 128
        private const val OFFLINE_MAX_PROMPT_TOKENS =
            OFFLINE_MODEL_MAX_TOKENS - OFFLINE_RESERVED_OUTPUT_TOKENS
        private const val OFFLINE_CONTEXT_TOO_LONG =
            "This message is too long for the on-device model (512-token limit). " +
                "Start a new chat or send a shorter message to continue offline."
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

    var pendingNotificationRoute by mutableStateOf<String?>(null)
        private set
    var pendingNotificationSessionId by mutableStateOf<String?>(null)
        private set

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
            }?.let { (wallet, models) ->
                applyWallet(wallet, historyAppContext ?: profileStorageContext)
                val access = models.access
                onlineModelAccess = access
                onlineModelImageInputSupport = models.imageInputSupport
                onlineProviders = models.providers
                val ctx = historyAppContext ?: profileStorageContext
                if (ctx != null) {
                    ModelAvailabilityStore.pruneExpired(ctx)
                    models.serverAvailability.forEach { (id, live) ->
                        if (live) ModelAvailabilityStore.clear(ctx, id)
                    }
                }
                onlineModelAvailability = ModelAvailabilityStore.mergeWithServer(ctx, models.serverAvailability)
                if (OnlineMode) {
                    reconcileSelectedOnlineModel()
                }
            }
        }
    }

    private fun reconcileSelectedOnlineModel() {
        val access = onlineModelAccess
        val availability = onlineModelAvailability
        val providers = onlineProviders ?: return
        if (!AiModels.isSelectableOnlineModel(selectedModelId, access, availability)) {
            selectedModelId = AiModels.firstSelectableModelId(
                AiModels.standaloneOnline,
                providers,
                access,
                availability
            ) ?: AiModels.DEFAULT_ONLINE_ID
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

    private data class OnlineModelMetadata(
        val access: Map<String, Boolean>,
        val serverAvailability: Map<String, Boolean>,
        val imageInputSupport: Map<String, Boolean>,
        val providers: List<AiProvider>
    )

    private fun fetchModelsJson(idToken: String): OnlineModelMetadata {
        val url = java.net.URL("$BACKEND_BASE/v1/models")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $idToken")
        if (connection.responseCode != 200) {
            throw IllegalStateException("models ${connection.responseCode}")
        }
        val body = connection.inputStream.bufferedReader().readText()
        val root = org.json.JSONObject(body)
        val arr = root.optJSONArray("models") ?: return OnlineModelMetadata(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val access = mutableMapOf<String, Boolean>()
        val serverAvailability = mutableMapOf<String, Boolean>()
        val imageInputSupport = mutableMapOf<String, Boolean>()
        val modelsByProvider = linkedMapOf<String, MutableList<AiModelOption>>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            val allowed = obj.optBoolean("allowed", false)
            access[id] = allowed
            serverAvailability[id] = AiModels.readOpenRouterAvailability(obj)
            imageInputSupport[id] = modelSupportsImageInput(obj)
            val providerId = id.substringBefore('/', missingDelimiterValue = "openrouter")
            val providerName = providerId
                .replace('-', ' ')
                .replace('_', ' ')
                .split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
            val model = AiModelOption(
                id = id,
                displayName = obj.optString("display_name", id.substringAfterLast('/')),
                icon = Icons.Outlined.AutoAwesome,
                tint = Color(0xFF5B4DFF),
                minTier = obj.optString("tier_min", "free"),
                providerId = providerId,
                supportsImageInput = modelSupportsImageInput(obj)
            )
            modelsByProvider.getOrPut(providerId) { mutableListOf() }.add(model)
        }
        val providers = modelsByProvider.map { (providerId, models) ->
            AiProvider(
                id = providerId,
                displayName = providerName(providerId),
                icon = Icons.Outlined.Hub,
                tint = Color(0xFF5B4DFF),
                models = models.sortedBy { it.displayName.lowercase() }
            )
        }.sortedBy { it.displayName.lowercase() }
        return OnlineModelMetadata(access, serverAvailability, imageInputSupport, providers)
    }

    private fun providerName(id: String): String = id
        .replace('-', ' ')
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

    private fun modelSupportsImageInput(model: org.json.JSONObject): Boolean {
        if (model.optBoolean("supports_image_input", false)) return true
        if (jsonArrayContainsImage(model.optJSONArray("input_modalities"))) return true
        if (jsonArrayContainsImage(model.optJSONArray("inputModalities"))) return true
        if (jsonArrayContainsImage(model.optJSONObject("architecture")?.optJSONArray("input_modalities"))) return true
        if (jsonArrayContainsImage(model.optJSONObject("architecture")?.optJSONArray("inputModalities"))) return true
        val modalities = model.optJSONObject("modalities")
        return jsonArrayContainsImage(modalities?.optJSONArray("input"))
    }

    private fun jsonArrayContainsImage(array: org.json.JSONArray?): Boolean {
        if (array == null) return false
        for (i in 0 until array.length()) {
            if (array.optString(i).equals("image", ignoreCase = true)) return true
        }
        return false
    }

    fun applyWallet(snapshot: WalletSnapshot, context: Context? = null) {
        walletSnapshot = snapshot
        val ctx = (context ?: historyAppContext ?: profileStorageContext)?.applicationContext
        if (ctx != null) {
            pendingUsageAlert = UsageAlertStore.nextAlert(ctx, snapshot)
        }
    }

    fun dismissUsageAlert(context: Context) {
        pendingUsageAlert?.let { UsageAlertStore.acknowledge(context.applicationContext, it) }
        pendingUsageAlert = null
        UsageAlertStore.nextAlert(context.applicationContext, walletSnapshot)?.let {
            pendingUsageAlert = it
        }
    }

    private fun applyWalletFromChatJson(response: String) {
        runCatching {
            applyWallet(WalletSnapshot.fromJson(org.json.JSONObject(response)))
        }
    }

    fun clearChatDialog() {
        pendingChatDialogMessage = null
        pendingChatDialogTitle = null
        clearPendingChatRetryAfterModelPick()
    }

    fun clearModelUnavailable() {
        clearChatDialog()
    }

    fun requestOpenModelPicker() {
        clearChatDialog()
        pendingRetryQuestionAfterModelPick = messagelist.lastOrNull()
            ?.takeIf { it.Role == "User" }
            ?.message
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        modelPickerOpenNonce++
    }

    fun clearPendingChatRetryAfterModelPick() {
        pendingRetryQuestionAfterModelPick = null
    }

    fun onOnlineModelSelected(modelId: String) {
        selectedModelId = modelId
        val question = pendingRetryQuestionAfterModelPick
        pendingRetryQuestionAfterModelPick = null
        if (!question.isNullOrBlank()) {
            sendOnlineChat(question, appendUserMessage = false)
        }
    }

    private fun showChatDialog(message: String, title: String? = null) {
        pendingChatDialogTitle = title
        pendingChatDialogMessage = message
    }

    private fun showOfflineDownloadFailedDialog() {
        showChatDialog(
            message = "Could not download the offline model. Check your connection and try again on Wi‑Fi.",
            title = "Download failed"
        )
    }

    private fun ingestChatErrorPayload(raw: String) {
        applyWalletFromChatJson(raw)
        val obj = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return
        val detailObj = obj.opt("detail")
        val code = when (detailObj) {
            is org.json.JSONObject -> detailObj.optString("code")
            else -> obj.optString("code")
        }
        if (code == "model_unavailable") {
            if (AiModels.isAutoSelection(selectedModelId)) return
            historyAppContext?.let { ModelAvailabilityStore.markUnavailable(it, selectedModelId) }
            onlineModelAvailability = onlineModelAvailability?.toMutableMap()?.apply {
                this[selectedModelId] = false
            } ?: mapOf(selectedModelId to false)
            reconcileSelectedOnlineModel()
        }
    }

    private fun readHttpErrorBody(connection: java.net.HttpURLConnection): String? {
        return connection.errorStream?.bufferedReader()?.use { it.readText() }
            ?.takeIf { it.isNotBlank() }
    }

    private fun isSafeUserFacingApiMessage(text: String): Boolean {
        if (text.length > 600) return false
        val lower = text.lowercase()
        return !lower.contains("<!doctype") &&
            !lower.contains("<html") &&
            !lower.contains("@font-face") &&
            !lower.contains("data:font/")
    }

    private fun parseChatErrorFromBody(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        ingestChatErrorPayload(raw)
        val obj = runCatching { org.json.JSONObject(raw) }.getOrNull()
        val detail = when (val field = obj?.opt("detail")) {
            is org.json.JSONObject -> {
                field.optString("detail").ifBlank { field.optString("message") }
            }
            is String -> field
            else -> obj?.optString("detail")
        }
        return detail?.takeIf { it.isNotBlank() && isSafeUserFacingApiMessage(it) }
    }

    private fun inferCreditUnlockIso(reason: String, periodEnd: String): String {
        val utc = java.time.ZoneOffset.UTC
        val now = java.time.ZonedDateTime.now(utc)
        return when (reason) {
            "12h_credits" -> {
                val unlock = if (now.hour < 12) {
                    now.withHour(12).withMinute(0).withSecond(0).withNano(0)
                } else {
                    now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                }
                unlock.toInstant().toString()
            }
            "daily_credits" -> {
                val unlock = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                unlock.toInstant().toString()
            }
            "monthly_credits" -> periodEnd
            else -> ""
        }
    }

    private fun formatCreditLimitReachedMessage(unlockAtIso: String): String {
        if (unlockAtIso.isBlank()) {
            return "Your credit limit has been reached. Please try again later."
        }
        val formatted = runCatching {
            val instant = java.time.Instant.parse(unlockAtIso)
            val zoned = instant.atZone(java.time.ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")
            formatter.format(zoned)
        }.getOrNull()
        return if (formatted.isNullOrBlank()) {
            "Your credit limit has been reached. Please try again later."
        } else {
            "Your credit limit has been reached. It will unlock at $formatted."
        }
    }

    private fun creditLimitMessageFromErrorBody(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val obj = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return null
        val reason = obj.optString("limit_block_reason", "none")
        if (reason !in setOf("12h_credits", "daily_credits", "monthly_credits")) return null
        val periodEnd = obj.optString("period_end", "")
        val unlockAt = obj.optString("credits_unlock_at", "").ifBlank {
            inferCreditUnlockIso(reason, periodEnd)
        }
        return formatCreditLimitReachedMessage(unlockAt)
    }

    private fun userFacingChatHttpError(code: Int, errorBody: String?): String {
        creditLimitMessageFromErrorBody(errorBody)?.let { return it }
        val parsed = parseChatErrorFromBody(errorBody)
        if (
            parsed != null &&
            (parsed.contains("credit limit", ignoreCase = true) ||
                parsed.contains("12-hour", ignoreCase = true))
        ) {
            val obj = runCatching { org.json.JSONObject(errorBody ?: "") }.getOrNull()
            val reason = obj?.optString("limit_block_reason", "")?.takeIf { it.isNotBlank() }
                ?: if (parsed.contains("12-hour", ignoreCase = true)) "12h_credits"
                else if (parsed.contains("daily credit", ignoreCase = true)) "daily_credits"
                else "12h_credits"
            val periodEnd = obj?.optString("period_end", "").orEmpty()
            val unlockAt = obj?.optString("credits_unlock_at", "")?.takeIf { it.isNotBlank() }
                ?: inferCreditUnlockIso(reason, periodEnd)
            return formatCreditLimitReachedMessage(unlockAt)
        }
        return when (code) {
            402, 403 -> {
                parsed ?: "Allowance exhausted or model not on your plan. Upgrade to continue."
            }
            429 -> {
                parsed ?: "Rate or daily limit reached. Try again later or use the offline model."
            }
            503 -> {
                parsed ?: "This model isn't available. Choose another model."
            }
            in 500..599 -> GENERIC_CHAT_ERROR
            else -> parsed ?: GENERIC_CHAT_ERROR
        }
    }

    private fun parseChatErrorResponse(connection: java.net.HttpURLConnection): String? {
        return parseChatErrorFromBody(readHttpErrorBody(connection))
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
        val draft = captureSessionPersistDraft()
        resetSessionState()
        if (draft == null) return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            persistSessionSnapshot(appContext, uid, draft, finalizeForProfile = true)
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
            val draft = captureSessionPersistDraft()
            if (draft != null) {
                launch {
                    persistSessionSnapshot(appContext, uid, draft, finalizeForProfile = true)
                }
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
            val draft = captureSessionPersistDraft() ?: return@launch
            persistSessionSnapshot(appContext, uid, draft, finalizeForProfile = false)
        }
    }

    private suspend fun finalizeAndPersistSession(appContext: Context) {
        val uid = auth.currentUser?.uid ?: return
        val draft = captureSessionPersistDraft() ?: return
        persistSessionSnapshot(appContext, uid, draft, finalizeForProfile = true)
    }

    private data class SessionPersistDraft(
        val sessionId: String,
        val messages: List<Response>,
        var compactedSummary: String,
        var summarizedUntilMessage: Int,
        var messagesSinceSummary: Int,
        val sessionCreatedAt: Long,
    )

    private fun captureSessionPersistDraft(): SessionPersistDraft? {
        if (messagelist.isEmpty()) return null
        return SessionPersistDraft(
            sessionId = currentSessionId,
            messages = messagelist.toList(),
            compactedSummary = compactedSummary,
            summarizedUntilMessage = summarizedUntilMessage,
            messagesSinceSummary = messagesSinceSummary,
            sessionCreatedAt = sessionCreatedAt,
        )
    }

    private suspend fun persistSessionSnapshot(
        appContext: Context,
        uid: String,
        draft: SessionPersistDraft,
        finalizeForProfile: Boolean
    ) {
        val messages = draft.messages
        val sessionId = draft.sessionId
        val existing = withContext(Dispatchers.IO) {
            ChatHistoryStore.findLocal(appContext, uid, sessionId)
        }
        val metadata = resolveSessionMetadata(existing, messages)
        if (finalizeForProfile && OnlineMode) {
            maybeCompactContextForDraft(draft)
        }
        var finalSummary = metadata.summary.ifBlank { draft.compactedSummary }
        if (finalizeForProfile && finalSummary.isBlank() && messages.isNotEmpty()) {
            val raw = callGeminiMeta(
                task = "compact",
                prompt = conversationText(messages),
                priorSummary = draft.compactedSummary
            )
            if (!raw.isNullOrBlank()) {
                draft.compactedSummary = clampWords(raw, CONTEXT_SUMMARY_MAX_WORDS)
                draft.summarizedUntilMessage = messages.size
                draft.messagesSinceSummary = 0
                finalSummary = draft.compactedSummary
            } else {
                finalSummary = clampWords(conversationText(messages.takeLast(20)), 40)
            }
        } else if (finalizeForProfile) {
            finalSummary = finalSummary.ifBlank { draft.compactedSummary }
        }

        val session = ChatSession(
            id = sessionId,
            title = metadata.title,
            summary = finalSummary,
            lastMessage = messages.lastOrNull()?.message.orEmpty(),
            updatedAt = System.currentTimeMillis(),
            titleRefined = metadata.titleRefined,
            compactedSummary = draft.compactedSummary.ifBlank { finalSummary },
            summarizedUntilMessage = draft.summarizedUntilMessage,
            messagesSinceSummary = draft.messagesSinceSummary,
            createdAt = existing?.createdAt?.takeIf { it > 0 } ?: draft.sessionCreatedAt,
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

        if (finalizeForProfile && draft.messages.isNotEmpty()) {
            val chatSnippet = session.compactedSummary.ifBlank {
                clampWords(conversationText(draft.messages.takeLast(10)), 40)
            }
            maybeUpdateProfileSummary(uid, chatSnippet)
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
        val draft = captureSessionPersistDraft() ?: return
        maybeCompactContextForDraft(draft)
        compactedSummary = draft.compactedSummary
        summarizedUntilMessage = draft.summarizedUntilMessage
        messagesSinceSummary = draft.messagesSinceSummary
    }

    private suspend fun maybeCompactContextForDraft(draft: SessionPersistDraft) {
        if (!OnlineMode) return
        if (!shouldCompact(draft.messagesSinceSummary)) return
        val messages = draft.messages
        val chunk = compactionChunk(messages, draft.summarizedUntilMessage)
        if (chunk.size < COMPACTION_WINDOW) return

        val chunkText = conversationText(chunk)
        val updated = callGeminiMeta(
            task = "compact",
            prompt = chunkText,
            priorSummary = draft.compactedSummary
        ) ?: return

        draft.compactedSummary = clampWords(updated, CONTEXT_SUMMARY_MAX_WORDS)
        draft.summarizedUntilMessage += chunk.size
        draft.messagesSinceSummary = (messages.size - draft.summarizedUntilMessage).coerceAtLeast(0)
    }

    private suspend fun maybeUpdateProfileSummary(uid: String, chatSummary: String) {
        if (profileBatchInFlight || chatSummary.isBlank()) return
        val nextCount = summarizedChatCount + 1
        summarizedChatCount = nextCount

        profileBatchInFlight = true
        try {
            val structured = buildStructuredProfileContext(_userProfile.value)
            val priorProfile = profileSummary.ifBlank { structured }
            val recentSummaries = sessionSummaries
                .sortedByDescending { it.updatedAt }
                .mapNotNull { s ->
                    s.compactedSummary.ifBlank { s.summary }.takeIf { it.isNotBlank() }
                }
                .distinct()
                .take(PROFILE_CHAT_BATCH)
            val prompt = buildString {
                append(PROFILE_MERGE_INSTRUCTION)
                append("\n\n")
                if (structured.isNotBlank()) {
                    append("Permanent user facts (never drop):\n")
                    append(structured)
                    append("\n\n")
                }
                if (priorProfile.isNotBlank()) {
                    append("Prior profile summary:\n")
                    append(priorProfile)
                    append("\n\n")
                }
                append("Latest chat summary:\n")
                append(chatSummary)
                if (recentSummaries.isNotEmpty()) {
                    append("\n\nOther recent chat summaries:\n")
                    append(recentSummaries.joinToString("\n---\n"))
                }
            }

            val merged = callGeminiMeta(task = "profile", prompt = prompt) ?: return
            val profile = clampWords(merged, PROFILE_SUMMARY_MAX_WORDS)
            profileSummary = profile

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
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
            _userProfile.value?.let { cacheProfile(it, uid) }
        } finally {
            profileBatchInFlight = false
        }
    }

    private suspend fun callOfflineMeta(prompt: String): String? {
        if (llmInference == null) return null
        return try {
            runOfflineInference(clampOfflineMetaPrompt(prompt)).trim()
        } catch (_: Throwable) {
            null
        }
    }

    private fun estimateOfflineTokenCount(text: String): Int {
        if (text.isBlank()) return 0
        return (text.length / 3.5).toInt().coerceAtLeast(1)
    }

    private fun truncateOfflineText(text: String, maxTokens: Int): String {
        val maxChars = (maxTokens * 3.5).toInt().coerceAtLeast(16)
        if (text.length <= maxChars) return text
        return text.take(maxChars - 1).trimEnd() + "…"
    }

    private fun clampOfflineMetaPrompt(prompt: String): String =
        truncateOfflineText(prompt, OFFLINE_MAX_PROMPT_TOKENS - 32)

    private fun buildOfflineChatPrompt(
        mode: ChatMode,
        priorMessages: List<Response>,
        question: String,
    ): String {
        val system = Instruction.forOffline(mode)
        var remaining = OFFLINE_MAX_PROMPT_TOKENS -
            estimateOfflineTokenCount(system) -
            estimateOfflineTokenCount("User: $question\nAssistant:")

        val historyLines = ArrayList<String>()
        for (message in priorMessages.asReversed()) {
            val line = if (message.Role == "User") {
                "User: ${message.message}"
            } else {
                "Assistant: ${message.message}"
            }
            val lineTokens = estimateOfflineTokenCount(line)
            when {
                lineTokens <= remaining -> {
                    historyLines.add(0, line)
                    remaining -= lineTokens
                }
                remaining >= 24 -> {
                    historyLines.add(0, truncateOfflineText(line, remaining))
                    remaining = 0
                    break
                }
                else -> break
            }
        }

        val conversation = buildString {
            if (historyLines.isNotEmpty()) {
                historyLines.joinTo(this, "\n")
                append('\n')
            }
            append("User: ").append(question).append("\nAssistant:")
        }
        return "${system.trim()}\n\n$conversation"
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

    fun switchToOnlineMode() {
        stopCurrentChat()
        OnlineMode = true
        refreshOnlineAccount()
    }

    fun switchToOfflineMode(context: Context) {
        stopCurrentChat()
        OnlineMode = false
        refreshModelStatus(context)
    }

    /** True when the active chat's messages are in the on-device top-5 cache. */
    fun isCurrentSessionCachedLocally(): Boolean {
        val summary = sessionSummaries.find { it.id == currentSessionId } ?: return true
        return summary.cachedLocally
    }

    fun queueMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        queuedMessage = trimmed
    }

    fun clearQueuedMessage() {
        queuedMessage = null
    }

    fun stopCurrentChat() {
        if (!loading) return
        chatGenerationId++
        chatJob?.cancel()
        chatJob = null
        runCatching { activeChatConnection?.disconnect() }
        activeChatConnection = null
        loading = false
    }

    fun sendQueuedMessageNow() {
        val text = queuedMessage?.trim().orEmpty()
        if (text.isBlank()) return
        queuedMessage = null
        stopCurrentChat()
        if (OnlineMode) {
            sendmessage(text)
        } else {
            sendMessageOffline(text)
        }
    }

    fun sendmessage(question: String) {
        sendOnlineChat(question.trim(), appendUserMessage = true)
    }

    private fun sendOnlineChat(question: String, appendUserMessage: Boolean) {
        if (question.isBlank()) return
        if (loading) {
            if (appendUserMessage) {
                queueMessage(question)
            }
            return
        }
        chatJob = viewModelScope.launch {
            val generationId = ++chatGenerationId
            loading = true
            try {
                performOnlineChat(question, generationId, appendUserMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generationId == chatGenerationId) {
                    showChatDialog(GENERIC_CHAT_ERROR)
                    historyAppContext?.let { persistCurrentSession(it) }
                }
            } finally {
                if (generationId == chatGenerationId) {
                    loading = false
                }
                chatJob = null
            }
        }
    }

    private suspend fun performOnlineChat(
        question: String,
        generationId: Int,
        appendUserMessage: Boolean = true,
    ) {
        if (appendUserMessage) {
            messagelist.add(Response(question, "User"))
        }

        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
        if (idToken == null) {
            _authstate.value = Authstate.Unauthenticated
            messagelist.add(
                Response(
                    "Your session expired. Please sign in again to use online chat.",
                    "Model"
                )
            )
            return
        }

        val reply = withContext(Dispatchers.IO) {
            val prior = messagelist.dropLast(1)
            val tail = unsummarizedTail(prior, summarizedUntilMessage)
            val history = tail
                .map {
                    val role = if (it.Role == "User") "user" else "model"
                    """{"role":"$role","content":"${it.message.escapeJson()}"}"""
                }
                .joinToString(",")

            val isFirstUserMessageInSession = countUserMessages(prior) == 0
            val profileJson = if (isFirstUserMessageInSession) {
                val profileText = profileContextForNewChat(_userProfile.value, profileSummary)
                if (profileText.isBlank()) {
                    "null"
                } else {
                    "\"${profileText.escapeJson()}\""
                }
            } else {
                "null"
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
                "instruction": "${Instruction.forChat(chatMode, selectedModelId).escapeJson()}",
                "chat_mode": "${chatMode.displayName.escapeJson()}",
                "model": "${selectedModelId.escapeJson()}",
                "profile_summary": $profileJson,
                "compacted_summary": $compactedJson
                }
                """.trimIndent()

            val url = java.net.URL("$BACKEND_BASE/chat")
            val connection = url.openConnection() as java.net.HttpURLConnection
            activeChatConnection = connection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.setRequestProperty("X-App-State", AppForegroundHolder.appStateHeader())
                connection.doOutput = true
                connection.connectTimeout = 20_000
                connection.readTimeout = 90_000
                connection.outputStream.write(requestBody.toByteArray())

                val responseCode = connection.responseCode
                when (responseCode) {
                    200 -> {
                        val response = connection.inputStream.bufferedReader().readText()
                        applyWalletFromChatJson(response)
                        refreshOnlineAccount()
                        val obj = org.json.JSONObject(response)
                        val modelUsed = obj.optString("model_used", obj.optString("model", ""))
                        if (modelUsed.isNotBlank() &&
                            !AiModels.isAutoSelection(selectedModelId) &&
                            !AiModels.sameProvider(selectedModelId, modelUsed)
                        ) {
                            showChatDialog(
                                "The provider answered as ${AiModels.displayName(modelUsed)} " +
                                    "instead of your selected ${AiModels.displayName(selectedModelId)} model. " +
                                    "Choose another model?"
                            )
                        }
                        obj.getString("reply")
                    }
                    else -> {
                        val errorBody = readHttpErrorBody(connection)
                        ingestChatErrorPayload(errorBody.orEmpty())
                        val message = userFacingChatHttpError(responseCode, errorBody)
                        showChatDialog(message)
                        null
                    }
                }
            } finally {
                if (activeChatConnection === connection) {
                    activeChatConnection = null
                }
                connection.disconnect()
            }
        }

        if (coroutineContext[Job]?.isActive != true || generationId != chatGenerationId) return

        if (reply != null) {
            messagelist.add(Response(reply, "Model"))
            messagesSinceSummary =
                (messagelist.size - summarizedUntilMessage).coerceAtLeast(0)
            maybeCompactContext()
            historyAppContext?.let { persistCurrentSession(it) }
        } else {
            refreshOnlineAccount()
            historyAppContext?.let { persistCurrentSession(it) }
        }
    }

    //Authentication

    private val securityRepository = SecurityRepository()
    private var securitySessionRegistered = false

    private val _deviceSessions = MutableStateFlow<List<DeviceSession>>(emptyList())
    val deviceSessions: StateFlow<List<DeviceSession>> = _deviceSessions.asStateFlow()

    private val _loginEvents = MutableStateFlow<List<LoginEvent>>(emptyList())
    val loginEvents: StateFlow<List<LoginEvent>> = _loginEvents.asStateFlow()

    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    private val _linkedProviders = MutableStateFlow<List<LinkedAuthProvider>>(emptyList())
    val linkedProviders: StateFlow<List<LinkedAuthProvider>> = _linkedProviders.asStateFlow()

    private val _hasPasswordProvider = MutableStateFlow(false)
    val hasPasswordProvider: StateFlow<Boolean> = _hasPasswordProvider.asStateFlow()

    private val _hasGoogleProvider = MutableStateFlow(false)
    val hasGoogleProvider: StateFlow<Boolean> = _hasGoogleProvider.asStateFlow()

    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    private val _linkGoogleState = MutableStateFlow<SecurityActionState>(SecurityActionState.Idle)
    val linkGoogleState: StateFlow<SecurityActionState> = _linkGoogleState.asStateFlow()

    private var pendingGoogleLinkCredential: AuthCredential? = null

    private val _changePasswordState = MutableStateFlow<SecurityActionState>(SecurityActionState.Idle)
    val changePasswordState: StateFlow<SecurityActionState> = _changePasswordState.asStateFlow()

    private val _revokeSessionState = MutableStateFlow<SecurityActionState>(SecurityActionState.Idle)
    val revokeSessionState: StateFlow<SecurityActionState> = _revokeSessionState.asStateFlow()

    private val _signOutAllState = MutableStateFlow<SecurityActionState>(SecurityActionState.Idle)
    val signOutAllState: StateFlow<SecurityActionState> = _signOutAllState.asStateFlow()

    sealed class SecurityActionState {
        object Idle : SecurityActionState()
        object Loading : SecurityActionState()
        object Success : SecurityActionState()
        data class Error(val message: String) : SecurityActionState()
    }

    fun refreshLinkedProviders() {
        _linkedProviders.value = securityRepository.linkedProviders()
        _hasPasswordProvider.value = securityRepository.hasPasswordProvider()
        _hasGoogleProvider.value = securityRepository.hasGoogleProvider()
        _accountEmail.value = securityRepository.primaryAccountEmail()
    }

    fun clearLinkGoogleState() {
        _linkGoogleState.value = SecurityActionState.Idle
    }

    fun cancelPendingGoogleLink() {
        pendingGoogleLinkCredential = null
        if (_authstate.value is Authstate.LinkGoogleRequiresPassword) {
            _authstate.value = Authstate.Unauthenticated
        }
    }

    fun refreshSecurityFromBackend() {
        refreshLinkedProviders()
        viewModelScope.launch {
            runCatching {
                _deviceSessions.value = securityRepository.fetchSessions()
                _loginEvents.value = securityRepository.fetchLoginEvents()
                _securityEvents.value = securityRepository.fetchSecurityEvents()
            }
        }
    }

    /** Loads security lists from Render backend (not Firestore client SDK). */
    fun startSecurityListeners() {
        refreshSecurityFromBackend()
    }

    private fun resolveAuthMethod(): String {
        val user = auth.currentUser ?: return "password"
        return if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            "google"
        } else {
            "password"
        }
    }

    fun clearSecurityActionState() {
        _signOutAllState.value = SecurityActionState.Idle
    }

    fun clearChangePasswordState() {
        _changePasswordState.value = SecurityActionState.Idle
    }

    fun registerSecuritySessionIfNeeded(context: Context) {
        if (securitySessionRegistered) return
        viewModelScope.launch {
            runCatching {
                securityRepository.registerSessionAfterLogin(
                    context.applicationContext,
                    resolveAuthMethod()
                )
                securitySessionRegistered = true
            }
        }
    }

    fun touchSecuritySession(context: Context) {
        viewModelScope.launch {
            runCatching { securityRepository.touchSessionActivity(context.applicationContext) }
        }
    }

    suspend fun isCurrentSessionRevoked(context: Context): Boolean =
        securityRepository.isCurrentSessionRevoked(context.applicationContext)

    /** @return true when the server revoked this device session and the user should sign out. */
    suspend fun syncSecuritySession(context: Context): Boolean {
        val revoked = securityRepository.syncSecuritySession(
            context.applicationContext,
            resolveAuthMethod(),
        )
        if (!revoked) {
            securitySessionRegistered = true
        }
        return revoked
    }

    fun requestPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                auth.sendPasswordResetEmail(email.trim()).await()
            }.map { }
            onResult(result)
        }
    }

    fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        signOutOthers: Boolean
    ) {
        val user = auth.currentUser
        val userEmail = user?.email
        if (user == null || userEmail.isNullOrBlank()) {
            _changePasswordState.value = SecurityActionState.Error("Not signed in.")
            return
        }
        if (newPassword.length < 8) {
            _changePasswordState.value = SecurityActionState.Error("Password must be at least 8 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            _changePasswordState.value = SecurityActionState.Error("New passwords do not match.")
            return
        }
        _changePasswordState.value = SecurityActionState.Loading
        val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
        user.reauthenticate(credential).addOnSuccessListener {
            user.updatePassword(newPassword).addOnSuccessListener {
                viewModelScope.launch {
                    runCatching {
                        securityRepository.logSecurityEvent(
                            "PASSWORD_CHANGED",
                            "Your Cherry AI password was changed.",
                            forceRefreshToken = true,
                        )
                    }.onFailure {
                        Log.w(
                            "CherryAISecurity",
                            "Password changed but security event log failed",
                            it
                        )
                    }
                    if (signOutOthers) {
                        runCatching {
                            val sessionId =
                                SessionStore.getOrCreateSessionId(context.applicationContext)
                            securityRepository.signOutAllDevices(
                                sessionId,
                                forceRefreshToken = true,
                            )
                            user.reauthenticate(
                                EmailAuthProvider.getCredential(userEmail, newPassword)
                            ).await()
                        }.onFailure {
                            _changePasswordState.value = SecurityActionState.Error(
                                it.message ?: "Password updated but sign-out failed."
                            )
                            return@launch
                        }
                    }
                    _changePasswordState.value = SecurityActionState.Success
                }
            }.addOnFailureListener {
                _changePasswordState.value = SecurityActionState.Error(
                    it.message ?: "Could not update password."
                )
            }
        }.addOnFailureListener {
            _changePasswordState.value = SecurityActionState.Error(
                "Current password is incorrect."
            )
        }
    }

    fun revokeSession(sessionId: String) {
        _revokeSessionState.value = SecurityActionState.Loading
        viewModelScope.launch {
            runCatching { securityRepository.revokeSession(sessionId) }
                .onSuccess {
                    _revokeSessionState.value = SecurityActionState.Success
                    refreshSecurityFromBackend()
                }
                .onFailure {
                    _revokeSessionState.value = SecurityActionState.Error(
                        it.message ?: "Could not revoke session."
                    )
                }
        }
    }

    fun signOutAllDevices(context: Context, keepCurrentDevice: Boolean) {
        _signOutAllState.value = SecurityActionState.Loading
        viewModelScope.launch {
            runCatching {
                val keepId = if (keepCurrentDevice) {
                    SessionStore.getOrCreateSessionId(context.applicationContext)
                } else {
                    null
                }
                securityRepository.signOutAllDevices(keepId)
            }.onSuccess {
                _signOutAllState.value = SecurityActionState.Success
            }.onFailure {
                _signOutAllState.value = SecurityActionState.Error(
                    it.message ?: "Could not sign out all devices."
                )
            }
        }
    }

    private fun clearSecurityState() {
        securitySessionRegistered = false
        _deviceSessions.value = emptyList()
        _loginEvents.value = emptyList()
        _securityEvents.value = emptyList()
    }

    val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val _authstate = MutableLiveData<Authstate>()
    val authstate : MutableLiveData<Authstate> = _authstate
    private var trackedAuthUid: String? = null

    init {
        _authstate.value = Authstate.Loading
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val uid = user?.uid
            if (uid == trackedAuthUid) return@addAuthStateListener
            trackedAuthUid = uid
            applyAuthUser(user)
        }
    }

    fun checkauthstate() {
        trackedAuthUid = null
        applyAuthUser(auth.currentUser)
        trackedAuthUid = auth.currentUser?.uid
    }

    private fun requiresEmailVerification(user: FirebaseUser): Boolean {
        if (user.isEmailVerified) return false
        val hasGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        val hasPassword = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        return hasPassword && !hasGoogle
    }

    private fun applyAuthUser(user: FirebaseUser?) {
        when {
            user == null -> {
                _userProfile.value = null
                _authstate.value = Authstate.Unauthenticated
            }
            requiresEmailVerification(user) -> _authstate.value = Authstate.VerificationSent
            else -> {
                _authstate.value = Authstate.Loading
                email = user.email ?: ""
                loadOrCreateUserProfile(user.uid, email)
            }
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authstate.value = Authstate.Loading
            try {
                val result = securityRepository.loginWithPassword(email, password)
                auth.signInWithCustomToken(result.customToken).await()
                val user = auth.currentUser
                val verified = user?.isEmailVerified == true || result.emailVerified
                if (verified) {
                    this@cherryAiViewModel.email = user?.email ?: result.email
                    beginAuthenticatedSession()
                    loadOrCreateUserProfile(user!!.uid, this@cherryAiViewModel.email)
                } else {
                    auth.signOut()
                    _authstate.value = Authstate.EmailNotVerified
                }
            } catch (e: com.example.cherryai.security.PasswordLoginException) {
                _authstate.value = Authstate.Error(
                    e.message ?: "Sign-in failed. Use Forgot password if you are locked out."
                )
            } catch (e: Exception) {
                _authstate.value = Authstate.Error(e.message ?: "Something went wrong")
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

    fun handleNotificationIntent(intent: Intent?) {
        val route = intent?.getStringExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE) ?: return
        pendingNotificationRoute = route
        pendingNotificationSessionId = intent.getStringExtra(MainActivity.EXTRA_SESSION_ID)
    }

    fun consumePendingNotificationRoute(): Pair<String?, String?> {
        val route = pendingNotificationRoute
        val session = pendingNotificationSessionId
        pendingNotificationRoute = null
        pendingNotificationSessionId = null
        return route to session
    }

    fun registerPushIfNeeded(context: Context) {
        viewModelScope.launch {
            runCatching {
                val prefs = NotificationPreferencesRepository().fetchPreferences()
                NotificationPrefsCache.update(prefs)
                PushTokenRegistrar.registerCurrentToken(context.applicationContext)
            }
        }
    }

    fun signout() {
        val ctx = historyAppContext
        viewModelScope.launch {
            ctx?.let {
                runCatching { PushTokenRegistrar.unregisterStoredToken(it.applicationContext) }
                SessionStore.clearSessionId(it.applicationContext)
            }
            if (ctx != null && messagelist.isNotEmpty()) {
                finalizeAndPersistSession(ctx)
            }
            auth.signOut()
            trackedAuthUid = null
            clearChatHistoryState()
            clearSecurityState()
            _userProfile.value = null
            _authstate.value = Authstate.Unauthenticated
        }
    }

    /** Soft-deletes the account while retaining its chat history in Firestore. */
    fun deleteAccount(onComplete: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onComplete(Result.failure(IllegalStateException("No signed-in account")))
            return
        }
        viewModelScope.launch {
            runCatching {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update(
                        mapOf(
                            "isDeleted" to true,
                            "deletedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                auth.signOut()
                clearChatHistoryState()
                clearSecurityState()
                val ctx = historyAppContext ?: profileStorageContext
                ctx?.let { SessionStore.clearSessionId(it.applicationContext) }
                _userProfile.value = null
                _authstate.value = Authstate.Unauthenticated
            }.onSuccess { onComplete(Result.success(Unit)) }
                .onFailure { onComplete(Result.failure(it)) }
        }
    }

    fun restoreDeletedAccount(onComplete: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(Result.failure(IllegalStateException("No signed-in account")))
            return
        }
        viewModelScope.launch {
            runCatching {
                val token = user.getIdToken(false).await()?.token
                    ?: error("Could not authenticate restore request")
                val connection = (java.net.URL("$BACKEND_BASE/v1/account/restore").openConnection()
                    as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }
                val status = connection.responseCode
                connection.disconnect()
                if (status !in 200..299) error("Restore request failed ($status)")
                loadOrCreateUserProfile(user.uid, user.email.orEmpty())
            }.onSuccess { onComplete(Result.success(Unit)) }
                .onFailure { onComplete(Result.failure(it)) }
        }
    }
    fun resetState() {
        _authstate.value = Authstate.Unauthenticated
    }

    fun setError(message: String) {
        _authstate.value = Authstate.Error(message)
    }

    fun handleGoogleIdToken(idToken: String, mode: GoogleSignInMode) {
        when (mode) {
            GoogleSignInMode.SignIn -> signInWithGoogle(idToken)
            GoogleSignInMode.LinkToCurrentAccount -> linkGoogleToCurrentAccount(idToken)
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authstate.value = Authstate.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onGoogleSignInSuccess()
                return@addOnCompleteListener
            }
            val collisionEmail = resolveGoogleCollisionEmail(task.exception)
            if (collisionEmail != null) {
                pendingGoogleLinkCredential = credential
                email = collisionEmail
                _authstate.value = Authstate.LinkGoogleRequiresPassword(collisionEmail)
                return@addOnCompleteListener
            }
            _authstate.value = Authstate.Error(
                task.exception?.message ?: "Google sign-in failed"
            )
        }
    }

    fun completeGoogleLinkAtSignIn(password: String) {
        val linkCredential = pendingGoogleLinkCredential
        val linkEmail = (_authstate.value as? Authstate.LinkGoogleRequiresPassword)?.email
            ?: email.takeIf { it.isNotBlank() }
        if (linkCredential == null || linkEmail.isNullOrBlank()) {
            _authstate.value = Authstate.Error("Link session expired. Try Google sign-in again.")
            return
        }
        if (password.isBlank()) {
            _authstate.value = Authstate.Error("Enter your Cherry AI password to link Google.")
            return
        }
        _authstate.value = Authstate.Loading
        auth.signInWithEmailAndPassword(linkEmail, password).addOnCompleteListener { signInTask ->
            if (!signInTask.isSuccessful) {
                _authstate.value = Authstate.LinkGoogleRequiresPassword(
                    linkEmail,
                    "Incorrect password. Use the password for your email account."
                )
                return@addOnCompleteListener
            }
            val user = auth.currentUser
            if (user == null) {
                _authstate.value = Authstate.Error("Sign-in failed. Try again.")
                return@addOnCompleteListener
            }
            user.linkWithCredential(linkCredential).addOnCompleteListener { linkTask ->
                if (!linkTask.isSuccessful) {
                    _authstate.value = Authstate.Error(
                        linkTask.exception?.message ?: "Could not link Google to this account."
                    )
                    return@addOnCompleteListener
                }
                pendingGoogleLinkCredential = null
                logGoogleLinkedBestEffort()
                onGoogleSignInSuccess()
            }
        }
    }

    fun linkGoogleToCurrentAccount(idToken: String) {
        val user = auth.currentUser
        if (user == null) {
            _linkGoogleState.value = SecurityActionState.Error("Not signed in.")
            return
        }
        if (securityRepository.hasGoogleProvider()) {
            _linkGoogleState.value = SecurityActionState.Error("Google is already linked.")
            return
        }
        _linkGoogleState.value = SecurityActionState.Loading
        val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
        user.linkWithCredential(googleCredential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                refreshLinkedProviders()
                logGoogleLinkedBestEffort()
                _linkGoogleState.value = SecurityActionState.Success
                return@addOnCompleteListener
            }
            val message = when ((task.exception as? FirebaseAuthException)?.errorCode) {
                "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                    "This Google account is linked to another Cherry AI user. Sign in with that account or use a different Google account."
                "ERROR_EMAIL_ALREADY_IN_USE" ->
                    "This Google email is already used by another Cherry AI account."
                "ERROR_REQUIRES_RECENT_LOGIN" ->
                    "For security, change your password first, then try linking Google again."
                else -> task.exception?.message ?: "Could not link Google account."
            }
            _linkGoogleState.value = SecurityActionState.Error(message)
        }
    }

    private fun onGoogleSignInSuccess() {
        val user = auth.currentUser ?: return
        email = user.email ?: ""
        val nameParts = user.displayName?.trim()?.split(" ") ?: emptyList()
        if (nameParts.isNotEmpty() && firstname.isBlank()) firstname = nameParts.first()
        if (nameParts.size > 1 && lastname.isBlank()) lastname = nameParts.drop(1).joinToString(" ")
        beginAuthenticatedSession()
        loadOrCreateUserProfile(user.uid, email)
    }

    private fun beginAuthenticatedSession() {
        securitySessionRegistered = false
        val ctx = historyAppContext ?: profileStorageContext ?: return
        SessionStore.rotateSessionId(ctx.applicationContext)
    }

    private fun resolveGoogleCollisionEmail(error: Exception?): String? {
        if (error is FirebaseAuthUserCollisionException) {
            return error.email?.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun logGoogleLinkedBestEffort() {
        viewModelScope.launch {
            runCatching {
                securityRepository.logSecurityEvent(
                    "GOOGLE_LINKED",
                    "A Google account was linked to your Cherry AI account."
                )
            }.onFailure {
                Log.w("CherryAISecurity", "GOOGLE_LINKED event log failed", it)
            }
        }
    }

    private fun loadOrCreateUserProfile(uid: String, email: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        readCachedProfile(uid, email)?.let { cached ->
            _userProfile.value = cached
            firstname = cached.firstname
            lastname = cached.lastname
        }

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                if (document.getBoolean("isDeleted") == true) {
                    val deletedAt = document.getTimestamp("deletedAt")?.toDate()?.time
                    val gracePeriodActive = deletedAt != null &&
                        System.currentTimeMillis() - deletedAt <= 30L * 24 * 60 * 60 * 1000
                    if (gracePeriodActive) {
                        _authstate.value = Authstate.DeletedPendingRestore
                    } else {
                        auth.signOut()
                        _userProfile.value = null
                        _authstate.value = Authstate.Unauthenticated
                    }
                    return@addOnSuccessListener
                }
                // Email is owned by Firebase Authentication, not the editable
                // profile document. This also repairs any stale legacy value.
                val profile = document.toObject(UserProfile::class.java)
                    ?.copy(email = auth.currentUser?.email ?: email)
                _userProfile.value = profile
                profile?.let { loaded ->
                    firstname = loaded.firstname
                    lastname = loaded.lastname
                    cacheProfile(loaded, uid)
                }
                profileSummary = profile?.profileSummary.orEmpty()
                summarizedChatCount = profile?.summarizedChatCount ?: 0
                fetchWallet()
                _authstate.value = if (profile?.onboardcomplete == true)
                    Authstate.Authenticated else Authstate.ProfileIncomplete
                refreshLinkedProviders()
                startSecurityListeners()
                historyAppContext?.let { registerSecuritySessionIfNeeded(it) }
                historyAppContext?.let { registerPushIfNeeded(it) }
            } else {
                val cached = readCachedProfile(uid, email)
                if (cached != null) {
                    _userProfile.value = cached
                    firstname = cached.firstname
                    lastname = cached.lastname
                    _authstate.value = if (cached.onboardcomplete) Authstate.Authenticated else Authstate.ProfileIncomplete
                    return@addOnSuccessListener
                }
                val newProfile = UserProfile(email = email, firstname = firstname, lastname = lastname)
                userRef.set(newProfile)
                    .addOnSuccessListener {
                        _userProfile.value = newProfile
                        cacheProfile(newProfile, uid)
                        profileSummary = ""
                        summarizedChatCount = 0
                        _authstate.value = Authstate.ProfileIncomplete
                        refreshLinkedProviders()
                        startSecurityListeners()
                        historyAppContext?.let { registerSecuritySessionIfNeeded(it) }
                        historyAppContext?.let { registerPushIfNeeded(it) }
                    }
                    .addOnFailureListener {
                        if (_userProfile.value == null) {
                            _authstate.value = Authstate.Error("Failed to load profile")
                        }
                    }
            }
        }.addOnFailureListener {
            if (_userProfile.value == null) {
                _authstate.value = Authstate.Error("Failed to load profile")
            }
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
                _userProfile.value?.let { cacheProfile(it, uid) }
                _authstate.value = Authstate.Authenticated
                onDone()
            }
    }

    fun updateProfile(
        fullName: String,
        phoneNumber: String,
        gender: String,
        age: Int,
        onDone: () -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: return
        val parts = fullName.trim().split(Regex("\\s+"), limit = 2)
        val first = parts.firstOrNull().orEmpty()
        val last = parts.getOrNull(1).orEmpty()
        val values = mapOf(
            "firstname" to first,
            "lastname" to last,
            "phoneNumber" to phoneNumber,
            "gender" to gender,
            "age" to age
        )
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(uid).update(values)
            .addOnSuccessListener {
                firstname = first
                lastname = last
                _userProfile.value = _userProfile.value?.copy(
                    firstname = first, lastname = last,
                    phoneNumber = phoneNumber, gender = gender, age = age
                )
                _userProfile.value?.let { cacheProfile(it, uid) }
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
        refreshOfflineModelFiles(context)
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

    fun refreshOfflineModelFiles(context: Context) {
        offlineModelFiles = context.filesDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("task", ignoreCase = true) && it.length() > 0L }
            ?.sortedBy { it.name.lowercase() }
            ?.map { OfflineModelFile(it.name, it.length(), it.absolutePath) }
            .orEmpty()
    }

    fun deleteOfflineModel(context: Context, model: OfflineModelFile): Boolean {
        if (model.name == "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task") {
            runCatching { llmInference?.close() }
            llmInference = null
            isModelDownloaded = false
        }
        val deleted = File(model.path).delete()
        refreshOfflineModelFiles(context)
        return deleted
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
                        LocalNotificationHelper.showOfflineDownload(context, success = true)
                    }
                    loadLocalModel(context)

                } catch (e: Exception) {
                    if (file.exists()) file.delete()
                    Log.w(TAG, "Offline model download failed", e)
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        downloadProgress = 0f
                        isModelDownloaded = false
                        showOfflineDownloadFailedDialog()
                        LocalNotificationHelper.showOfflineDownload(context, success = false)
                    }
                }
            }
        }
    }

    private var llmInference: LlmInference? = null
    private var offlineEngineContext: Context? = null
    private val offlineLlmMutex = Mutex()

    private class OfflineContextTooLongException : Exception()

    private fun createOfflineEngineLocked(appContext: Context) {
        val modelFile = File(appContext.filesDir, "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task")
        if (!modelFile.exists()) error("Offline model file missing")
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .build()
        llmInference = LlmInference.createFromOptions(appContext, options)
    }

    private suspend fun runOfflineInference(prompt: String): String {
        if (estimateOfflineTokenCount(prompt) >= OFFLINE_MAX_PROMPT_TOKENS) {
            throw OfflineContextTooLongException()
        }
        return offlineLlmMutex.withLock {
            val appContext = offlineEngineContext
            val model = llmInference ?: error("Offline model not loaded")
            try {
                withContext(Dispatchers.IO) {
                    model.generateResponse(prompt)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Offline inference failed; recreating on-device session", t)
                runCatching { llmInference?.close() }
                llmInference = null
                if (appContext != null) {
                    runCatching { createOfflineEngineLocked(appContext) }
                }
                throw t
            }
        }
    }

    fun loadLocalModel(context: Context) {
        val appContext = context.applicationContext
        offlineEngineContext = appContext
        viewModelScope.launch {
            offlineLlmMutex.withLock {
                if (llmInference != null) return@launch
                val modelFile = File(appContext.filesDir, "Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task")
                if (!modelFile.exists()) return@launch
                try {
                    createOfflineEngineLocked(appContext)
                } catch (t: Throwable) {
                    runCatching { llmInference?.close() }
                    llmInference = null
                    Log.w(TAG, "Failed to load offline model", t)
                    withContext(Dispatchers.Main) {
                        showChatDialog(
                            message = "The offline model could not be loaded on this device.",
                            title = "Offline model unavailable"
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        runCatching { llmInference?.close() }
        llmInference = null
        super.onCleared()
    }

    fun sendMessageOffline(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return
        if (loading) {
            queueMessage(trimmed)
            return
        }

        chatJob = viewModelScope.launch {
            val generationId = ++chatGenerationId
            loading = true
            try {
                performOfflineChat(trimmed, generationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineContextTooLongException) {
                if (generationId == chatGenerationId) {
                    if (messagelist.isNotEmpty() && messagelist.last().Role == "User") {
                        messagelist.removeAt(messagelist.lastIndex)
                    }
                    showChatDialog(
                        message = OFFLINE_CONTEXT_TOO_LONG,
                        title = "Message too long for offline model"
                    )
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Offline inference failed", e)
                if (generationId == chatGenerationId) {
                    if (messagelist.isNotEmpty() && messagelist.last().Role == "User") {
                        messagelist.removeAt(messagelist.lastIndex)
                    }
                    showChatDialog(GENERIC_CHAT_ERROR)
                }
            } finally {
                if (generationId == chatGenerationId) {
                    loading = false
                }
                chatJob = null
            }
        }
    }

    private suspend fun performOfflineChat(question: String, generationId: Int) {
        messagelist.add(Response(question, "User"))

        val model = llmInference
        if (model == null) {
            if (messagelist.isNotEmpty()) messagelist.removeAt(messagelist.lastIndex)
            showChatDialog(
                message = "The offline model is still loading or is not installed. Download it to chat offline.",
                title = "Offline model not ready"
            )
            return
        }

        val priorMessages = messagelist.dropLast(1)
        val prompt = buildOfflineChatPrompt(chatMode, priorMessages, question)

        val reply = runOfflineInference(prompt)

        if (coroutineContext[Job]?.isActive != true || generationId != chatGenerationId) return

        messagelist.add(Response(reply, "Model"))
        historyAppContext?.let { persistCurrentSession(it) }
    }
}
enum class GoogleSignInMode {
    SignIn,
    LinkToCurrentAccount,
}

sealed class Authstate{
    object Authenticated : Authstate()
    object Unauthenticated : Authstate()
    object Loading : Authstate()
    data class Error(val message : String) : Authstate()
    object VerificationSent : Authstate()
    object EmailNotVerified : Authstate()
    object ProfileIncomplete : Authstate()
    object DeletedPendingRestore : Authstate()
    data class LinkGoogleRequiresPassword(
        val email: String,
        val inlineError: String? = null,
    ) : Authstate()
}

data class UserProfile(
    val email: String = "",
    val firstname: String = "",
    val lastname : String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val age: Int = 0,
    val plan: String = "",
    val planExpiresAt: Timestamp? = null,
    val onboardcomplete : Boolean = false,
    val selectedplan : String = "",
    val profileSummary: String = "",
    val summarizedChatCount: Int = 0,
    val isDeleted: Boolean = false
)
