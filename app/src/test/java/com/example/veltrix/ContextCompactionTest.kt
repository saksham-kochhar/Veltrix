package com.example.veltrix

import com.example.veltrix.chathistorry.COMPACTION_WINDOW
import com.example.veltrix.chathistorry.compactionChunk
import com.example.veltrix.chathistorry.shouldCompact
import com.example.veltrix.chathistorry.shouldRefreshProfile
import com.example.veltrix.chathistorry.unsummarizedTail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextCompactionTest {

    private fun msgs(n: Int): List<Response> =
        (1..n).map { i ->
            Response(
                message = "m$i",
                Role = if (i % 2 == 1) "User" else "Model"
            )
        }

    @Test
    fun unsummarizedTail_after45of50_returnsLast5() {
        val all = msgs(50)
        val tail = unsummarizedTail(all, 45)
        assertEquals(5, tail.size)
        assertEquals("m46", tail.first().message)
        assertEquals("m50", tail.last().message)
    }

    @Test
    fun compactionChunk_takesNext15FromCursor() {
        val all = msgs(50)
        val chunk = compactionChunk(all, summarizedUntilMessage = 30)
        assertEquals(COMPACTION_WINDOW, chunk.size)
        assertEquals("m31", chunk.first().message)
        assertEquals("m45", chunk.last().message)
    }

    @Test
    fun shouldCompact_atWindow() {
        assertFalse(shouldCompact(14))
        assertTrue(shouldCompact(15))
    }

    @Test
    fun shouldRefreshProfile_every15Chats() {
        assertFalse(shouldRefreshProfile(0))
        assertFalse(shouldRefreshProfile(14))
        assertTrue(shouldRefreshProfile(15))
        assertTrue(shouldRefreshProfile(30))
    }
}
