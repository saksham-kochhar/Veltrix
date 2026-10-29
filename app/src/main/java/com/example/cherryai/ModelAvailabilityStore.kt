package com.example.cherryai

import android.content.Context

/**
 * Short-lived client-side suppression when OpenRouter returns model_unavailable.
 * Merged with server `available` flags from /v1/models.
 */
object ModelAvailabilityStore {
    private const val PREFS = "model_availability_v1"
    private const val BLOCK_MS = 30 * 60 * 1000L

    fun markUnavailable(context: Context, modelId: String) {
        if (modelId.isBlank() || AiModels.isAutoSelection(modelId)) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(modelId, System.currentTimeMillis())
            .apply()
    }

    fun clear(context: Context, modelId: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(modelId)
            .apply()
    }

    fun isClientBlocked(context: Context?, modelId: String): Boolean {
        if (context == null || modelId.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val blockedAt = prefs.getLong(modelId, 0L)
        if (blockedAt <= 0L) return false
        val age = System.currentTimeMillis() - blockedAt
        if (age >= BLOCK_MS) {
            prefs.edit().remove(modelId).apply()
            return false
        }
        return true
    }

    fun mergeWithServer(
        context: Context?,
        serverAvailability: Map<String, Boolean>
    ): Map<String, Boolean> {
        if (context == null) return serverAvailability
        return serverAvailability.mapValues { (id, serverLive) ->
            serverLive && !isClientBlocked(context, id)
        }
    }

    fun pruneExpired(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        var changed = false
        for (key in prefs.all.keys) {
            val blockedAt = prefs.getLong(key, 0L)
            if (blockedAt > 0L && now - blockedAt >= BLOCK_MS) {
                editor.remove(key)
                changed = true
            }
        }
        if (changed) editor.apply()
    }
}

fun AiModels.readOpenRouterAvailability(model: org.json.JSONObject): Boolean {
    when {
        model.has("available") -> return model.optBoolean("available", true)
        model.has("openrouter_available") -> return model.optBoolean("openrouter_available", true)
        model.has("live") -> return model.optBoolean("live", true)
        model.has("is_available") -> return model.optBoolean("is_available", true)
    }
    return true
}
