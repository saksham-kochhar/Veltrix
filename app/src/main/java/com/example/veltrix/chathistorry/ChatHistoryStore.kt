package com.example.veltrix.chathistorry

import android.content.Context
import com.example.veltrix.Response
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatHistoryStore {

    private const val MAX_LOCAL_SESSIONS = 5

    fun chatsCollection(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("chats")

    /** @return null on success, or a short user-facing error message. */
    suspend fun syncSessionToFirestore(uid: String, session: ChatSession): String? {
        return try {
            chatsCollection(uid)
                .document(session.id)
                .set(sessionToMap(session))
                .await()
            null
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Chat couldn't sync — update Firestore rules for users/{uid}/chats"
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Chat saved offline — will sync when you're back online"
                else -> "Chat sync failed: ${e.code}"
            }
        } catch (e: Exception) {
            "Chat sync failed: ${e.message ?: "unknown error"}"
        }
    }

    private fun fileFor(context: Context, uid: String): File =
        File(context.filesDir, "chat_history_$uid.json")

    fun loadLocal(context: Context, uid: String): List<ChatSession> {
        val file = fileFor(context, uid)
        if (!file.exists()) return emptyList()
        return try {
            parseSessions(JSONArray(file.readText()))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocal(context: Context, uid: String, sessions: List<ChatSession>) {
        val top = sessions
            .sortedByDescending { it.updatedAt }
            .take(MAX_LOCAL_SESSIONS)
        val array = JSONArray()
        top.forEach { array.put(sessionToJson(it)) }
        fileFor(context, uid).writeText(array.toString())
    }

    fun upsertLocal(context: Context, uid: String, session: ChatSession): List<ChatSession> {
        val existing = loadLocal(context, uid).toMutableList()
        val index = existing.indexOfFirst { it.id == session.id }
        if (index >= 0) existing[index] = session else existing.add(session)
        val trimmed = existing.sortedByDescending { it.updatedAt }.take(MAX_LOCAL_SESSIONS)
        saveLocal(context, uid, trimmed)
        return trimmed
    }

    fun findLocal(context: Context, uid: String, sessionId: String): ChatSession? =
        loadLocal(context, uid).find { it.id == sessionId }

    fun sessionToMap(session: ChatSession): Map<String, Any> = mapOf(
        "id" to session.id,
        "title" to session.title,
        "summary" to session.summary,
        "lastMessage" to session.lastMessage,
        "updatedAt" to session.updatedAt,
        "createdAt" to session.createdAt,
        "titleRefined" to session.titleRefined,
        "compactedSummary" to session.compactedSummary,
        "summarizedUntilMessage" to session.summarizedUntilMessage,
        "messagesSinceSummary" to session.messagesSinceSummary,
        "messages" to session.messages.map { mapOf("message" to it.message, "Role" to it.Role) }
    )

    fun sessionFromMap(data: Map<String, Any?>): ChatSession {
        val messagesRaw = data["messages"] as? List<*> ?: emptyList<Any>()
        val messages = messagesRaw.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            Response(
                message = map["message"]?.toString() ?: "",
                Role = map["Role"]?.toString() ?: "Model"
            )
        }
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        return ChatSession(
            id = data["id"]?.toString() ?: "",
            title = data["title"]?.toString() ?: "New chat",
            summary = data["summary"]?.toString() ?: "",
            lastMessage = data["lastMessage"]?.toString() ?: "",
            updatedAt = updatedAt,
            titleRefined = data["titleRefined"] as? Boolean ?: false,
            compactedSummary = data["compactedSummary"]?.toString() ?: "",
            summarizedUntilMessage = (data["summarizedUntilMessage"] as? Number)?.toInt() ?: 0,
            messagesSinceSummary = (data["messagesSinceSummary"] as? Number)?.toInt() ?: 0,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: updatedAt,
            messages = messages
        )
    }

    fun summaryFromMap(data: Map<String, Any?>, cachedLocally: Boolean): ChatSessionSummary {
        val compacted = data["compactedSummary"]?.toString().orEmpty()
        val summary = data["summary"]?.toString().orEmpty()
        return ChatSessionSummary(
            id = data["id"]?.toString() ?: "",
            title = data["title"]?.toString() ?: "New chat",
            summary = summary.ifBlank { compacted },
            lastMessage = data["lastMessage"]?.toString() ?: "",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            cachedLocally = cachedLocally,
            compactedSummary = compacted
        )
    }

    private fun sessionToJson(session: ChatSession): JSONObject {
        val messages = JSONArray()
        session.messages.forEach { msg ->
            messages.put(
                JSONObject()
                    .put("message", msg.message)
                    .put("Role", msg.Role)
            )
        }
        return JSONObject()
            .put("id", session.id)
            .put("title", session.title)
            .put("summary", session.summary)
            .put("lastMessage", session.lastMessage)
            .put("updatedAt", session.updatedAt)
            .put("createdAt", session.createdAt)
            .put("titleRefined", session.titleRefined)
            .put("compactedSummary", session.compactedSummary)
            .put("summarizedUntilMessage", session.summarizedUntilMessage)
            .put("messagesSinceSummary", session.messagesSinceSummary)
            .put("messages", messages)
    }

    private fun parseSessions(array: JSONArray): List<ChatSession> {
        val result = mutableListOf<ChatSession>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val messagesArr = obj.optJSONArray("messages") ?: JSONArray()
            val messages = mutableListOf<Response>()
            for (j in 0 until messagesArr.length()) {
                val m = messagesArr.getJSONObject(j)
                messages.add(
                    Response(
                        message = m.optString("message"),
                        Role = m.optString("Role", "Model")
                    )
                )
            }
            val updatedAt = obj.optLong("updatedAt")
            result.add(
                ChatSession(
                    id = obj.optString("id"),
                    title = obj.optString("title", "New chat"),
                    summary = obj.optString("summary"),
                    lastMessage = obj.optString("lastMessage"),
                    updatedAt = updatedAt,
                    titleRefined = obj.optBoolean("titleRefined", false),
                    compactedSummary = obj.optString("compactedSummary"),
                    summarizedUntilMessage = obj.optInt("summarizedUntilMessage", 0),
                    messagesSinceSummary = obj.optInt("messagesSinceSummary", 0),
                    createdAt = obj.optLong("createdAt", updatedAt),
                    messages = messages
                )
            )
        }
        return result
    }
}
