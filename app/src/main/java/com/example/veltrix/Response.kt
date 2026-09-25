package com.example.veltrix

data class Response(
    val message : String,
    val Role : String
)

/** True when the model bubble is a rate/credit limit from the server (show offline fallback). */
fun Response.isLimitExhaustedMessage(): Boolean {
    if (Role != "Model") return false
    val lower = message.lowercase()
    return lower.contains("daily free request limit") ||
        lower.contains("daily credit limit") ||
        lower.contains("12-hour credit limit")
}
