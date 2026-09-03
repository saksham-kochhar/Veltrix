package com.example.veltrix.chathistorry

import android.content.Context
import com.example.veltrix.Response
import com.google.firebase.firestore.FirebaseFirestore
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
        "titleRefined" to session.titleRefined,
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
        return ChatSession(
            id = data["id"]?.toString() ?: "",
            title = data["title"]?.toString() ?: "New chat",
            summary = data["summary"]?.toString() ?: "",
            lastMessage = data["lastMessage"]?.toString() ?: "",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            titleRefined = data["titleRefined"] as? Boolean ?: false,
            messages = messages
        )
    }

    fun summaryFromMap(data: Map<String, Any?>, cachedLocally: Boolean): ChatSessionSummary =
        ChatSessionSummary(
            id = data["id"]?.toString() ?: "",
            title = data["title"]?.toString() ?: "New chat",
            summary = data["summary"]?.toString() ?: "",
            lastMessage = data["lastMessage"]?.toString() ?: "",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            cachedLocally = cachedLocally
        )

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
            .put("titleRefined", session.titleRefined)
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
            result.add(
                ChatSession(
                    id = obj.optString("id"),
                    title = obj.optString("title", "New chat"),
                    summary = obj.optString("summary"),
                    lastMessage = obj.optString("lastMessage"),
                    updatedAt = obj.optLong("updatedAt"),
                    titleRefined = obj.optBoolean("titleRefined", false),
                    messages = messages
                )
            )
        }
        return result
    }
}
