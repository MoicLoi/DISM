package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryEventTimeRuntimePersistenceTest {

    private val persistence =
        InMemoryEventTimeRuntimePersistence()

    private val t1 =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    private val t2 =
        Instant.parse(
            "2026-09-16T10:01:00Z"
        )

    private fun event(
        id: String,
        time: Instant,
    ) =
        EventTimeRuntimePersistenceEvent(
            id =
                id,

            runtimeState =
                EventTimeRuntimeState.RUNNING,

            evaluatedAt =
                time,

            processedEventCount =
                1,

            generatedAlarmCount =
                0,
        )

    @Test
    fun emptyPersistenceShouldHaveNoLatestEvent() {

        assertNull(
            persistence.latest()
        )
    }

    @Test
    fun appendShouldStoreEvent() {

        val first =
            event(
                id = "runtime-1",
                time = t1,
            )

        persistence.append(first)

        assertEquals(
            listOf(first),
            persistence.all(),
        )

        assertEquals(
            first,
            persistence.latest(),
        )
    }

    @Test
    fun appendShouldPreserveOrder() {

        val first =
            event(
                id = "runtime-1",
                time = t1,
            )

        val second =
            event(
                id = "runtime-2",
                time = t2,
            )

        persistence.append(first)
        persistence.append(second)

        assertEquals(
            listOf(
                first,
                second,
            ),
            persistence.all(),
        )

        assertEquals(
            second,
            persistence.latest(),
        )
    }

    @Test
    fun allShouldReturnSnapshot() {

        val first =
            event(
                id = "runtime-1",
                time = t1,
            )

        persistence.append(first)

        val snapshot =
            persistence.all()

        persistence.clear()

        assertEquals(
            listOf(first),
            snapshot,
        )
    }

    @Test
    fun clearShouldRemoveAllEvents() {

        persistence.append(
            event(
                id = "runtime-1",
                time = t1,
            )
        )

        persistence.clear()

        assertEquals(
            emptyList(),
            persistence.all(),
        )

        assertNull(
            persistence.latest()
        )
    }

    @Test
    fun persistenceShouldBeDeterministic() {

        val first =
            event(
                id = "runtime-1",
                time = t1,
            )

        val second =
            event(
                id = "runtime-2",
                time = t2,
            )

        persistence.append(first)
        persistence.append(second)

        assertEquals(
            persistence.all(),
            listOf(
                first,
                second,
            )
        )
    }
}
