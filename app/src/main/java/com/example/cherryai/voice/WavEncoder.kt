package com.example.cherryai.voice

import java.io.ByteArrayOutputStream

object WavEncoder {
    fun encode(pcm: ShortArray): ByteArray {
        val out = ByteArrayOutputStream(44 + pcm.size * 2)
        fun text(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun le(v: Int) { out.write(v and 255); out.write(v shr 8 and 255); out.write(v shr 16 and 255); out.write(v shr 24 and 255) }
        fun le16(v: Int) { out.write(v and 255); out.write(v shr 8 and 255) }
        text("RIFF"); le(36 + pcm.size * 2); text("WAVEfmt "); le(16); le16(1); le16(1); le(VoiceConfig.sampleRate); le(VoiceConfig.sampleRate * 2); le16(2); le16(16); text("data"); le(pcm.size * 2)
        pcm.forEach { le16(it.toInt()) }; return out.toByteArray()
    }
}
