package com.example.cherryai.voice

object VoiceConfig {
    const val sampleRate = 16_000
    const val frameMs = 20
    const val preRollMs = 200
    const val speechStartMs = 150
    const val silenceEndMs = 700
    const val minimumSegmentMs = 400
    const val maxSegmentMs = 25_000
    const val noSpeechTimeoutMs = 5_000L
    const val sessionLimitMs = 120_000L
    const val energyThreshold = 0.025f
}
