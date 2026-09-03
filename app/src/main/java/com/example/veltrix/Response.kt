package com.example.veltrix

const val LIMIT_EXHAUSTED_MESSAGE =
    "You have reached your free limit. You can still use offline model"

data class Response(
    val message : String,
    val Role : String
)

fun Response.isLimitExhaustedMessage(): Boolean =
    Role == "Model" && message == LIMIT_EXHAUSTED_MESSAGE
