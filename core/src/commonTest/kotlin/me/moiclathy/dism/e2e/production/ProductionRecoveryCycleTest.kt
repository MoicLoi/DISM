package me.moiclathy.dism.e2e.production

import kotlin.test.Test
import kotlin.test.assertEquals

class ProductionRecoveryCycleTest {
    @Test
    fun fullCyclePersistsFeedbackAndRecovers() {
        val cycle = ProductionRecoveryCycle<Int, String, String, String, List<String>>(
            evaluate = { "attention:$it" },
            persist = { "event:$it" },
            applyFeedback = { result, feedback -> "$result+$feedback" },
            recover = { events -> events },
            snapshot = { result -> result.length },
        )

        val result = cycle.run(1, "completed")

        assertEquals(listOf("event:attention:1", "event:attention:1+completed"), result.persistedEvents)
        assertEquals(result.persistedEvents, result.recoveredState)
        assertEquals("attention:1+completed".length, result.nextSnapshot)
    }

    @Test
    fun emptyPersistenceCanBeRepresentedByRecoveryAdapter() {
        val recovered = emptyList<String>()
        ProductionRecoveryAssertions.assertPersistedEventCount(recovered.size, 0)
    }

    @Test
    fun deterministicAssertionAcceptsEqualResults() {
        ProductionRecoveryAssertions.assertDeterministic("same", "same")
    }
}
