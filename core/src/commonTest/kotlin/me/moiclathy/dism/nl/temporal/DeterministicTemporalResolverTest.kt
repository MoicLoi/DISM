package me.moiclathy.dism.nl.temporal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicTemporalResolverTest {
    private val resolver = DeterministicTemporalResolver()

    @Test
    fun resolvesTomorrowAfternoonThree() {
        val result = resolver.resolve("明天下午3点实验课")
        val mention = result.mentions.single()
        assertEquals(1, mention.dayOffset)
        assertEquals(15, mention.hour)
        assertEquals(0, mention.minute)
        assertEquals(Meridiem.AFTERNOON, mention.meridiem)
        assertEquals(TemporalGranularity.POINT, mention.granularity)
    }

    @Test
    fun resolvesExplicitClock() {
        val mention = resolver.resolve("15:30开始").mentions.single()
        assertEquals(15, mention.hour)
        assertEquals(30, mention.minute)
    }

    @Test
    fun resolvesRelativeDayWithoutInventingClock() {
        val mention = resolver.resolve("后天").mentions.single()
        assertEquals(2, mention.dayOffset)
        assertEquals(null, mention.hour)
        assertEquals(TemporalGranularity.DAY, mention.granularity)
    }

    @Test
    fun unknownTextStaysUnresolved() {
        val result = resolver.resolve("找一下实验服")
        assertTrue(result.mentions.isEmpty())
        assertEquals("找一下实验服", result.unresolvedText)
    }
}
