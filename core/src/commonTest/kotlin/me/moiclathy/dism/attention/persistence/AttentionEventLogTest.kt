package me.moiclathy.dism.attention.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Instant

class AttentionEventLogTest {
    private val first = Instant.parse("2026-01-01T10:00:00Z")
    private val second = Instant.parse("2026-01-01T10:01:00Z")

    @Test
    fun appendPreservesEventsAndCanFilterByAttentionKey() {
        val log = InMemoryAttentionEventLog()
        val a = AttentionEvent("1", "a", AttentionEventType.DECISION, first)
        val b = AttentionEvent("2", "b", AttentionEventType.DECISION, first)
        log.append(a)
        log.append(b)

        assertEquals(listOf(a), log.eventsFor("a"))
        assertEquals(listOf(a, b), log.all())
    }

    @Test
    fun appendAllKeepsInsertionOrder() {
        val log = InMemoryAttentionEventLog()
        val a = AttentionEvent("1", "a", AttentionEventType.DECISION, first)
        val b = AttentionEvent("2", "a", AttentionEventType.FEEDBACK, second)
        log.appendAll(listOf(a, b))

        assertEquals(listOf(a, b), log.all())
    }

    @Test
    fun replayBuildsChronologicalTimelineAndLatestEvent() {
        val log = InMemoryAttentionEventLog()
        val later = AttentionEvent("2", "a", AttentionEventType.FEEDBACK, second)
        val earlier = AttentionEvent("1", "a", AttentionEventType.DECISION, first)
        log.append(later)
        log.append(earlier)

        val replay = AttentionEventReplay()
        assertEquals(listOf(earlier, later), replay.timelineFor("a", log))
        assertSame(later, replay.latestFor("a", log))
    }
}
