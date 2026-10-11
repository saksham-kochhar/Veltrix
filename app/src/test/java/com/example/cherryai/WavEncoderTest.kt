package com.example.cherryai

import com.example.cherryai.voice.WavEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavEncoderTest {
    @Test fun writesPcmWaveHeader() {
        val wav = WavEncoder.encode(shortArrayOf(1, -1))
        assertEquals(46, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals("fmt ", String(wav, 12, 4))
        assertEquals("data", String(wav, 36, 4))
        assertTrue(wav[40].toInt() == 4)
    }
}
