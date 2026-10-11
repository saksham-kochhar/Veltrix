package com.example.cherryai.voice

import kotlin.math.sqrt

/** Energy VAD with pre-roll, hangover, minimum duration, and hard segment split. */
class VadSegmenter {
    private val preRoll = ArrayDeque<ShortArray>()
    private val current = ArrayList<ShortArray>()
    private var active = false
    private var speechMs = 0
    private var silenceMs = 0
    private var totalMs = 0
    fun accept(frame: ShortArray): List<ShortArray> {
        val frameMs = VoiceConfig.frameMs
        totalMs += frameMs
        preRoll.addLast(frame)
        while (preRoll.sumOf { it.size } > VoiceConfig.sampleRate * VoiceConfig.preRollMs / 1000) preRoll.removeFirst()
        val rms = sqrt(frame.map { it.toFloat() / Short.MAX_VALUE }.map { it * it }.average()).toFloat()
        val loud = rms >= VoiceConfig.energyThreshold
        if (!active) { speechMs = if (loud) speechMs + frameMs else 0; if (speechMs >= VoiceConfig.speechStartMs) { active = true; current.addAll(preRoll); preRoll.clear() } }
        if (active) { current.add(frame); silenceMs = if (loud) 0 else silenceMs + frameMs }
        if (active && (silenceMs >= VoiceConfig.silenceEndMs || current.sumOf { it.size } >= VoiceConfig.sampleRate * VoiceConfig.maxSegmentMs / 1000)) return finish()
        return emptyList()
    }
    fun flush(): List<ShortArray> = if (active) finish() else emptyList()
    private fun finish(): List<ShortArray> { val result = if (current.sumOf { it.size } >= VoiceConfig.sampleRate * VoiceConfig.minimumSegmentMs / 1000) listOf(current.flatMap { it.asIterable() }.toShortArray()) else emptyList(); current.clear(); preRoll.clear(); active = false; speechMs = 0; silenceMs = 0; return result }
}
