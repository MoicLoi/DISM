package me.moiclathy.dism.schedule.adaptation.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * 适配记录"存下去再读回来"必须逐字段相等。
 *
 * ```text
 * restored == original
 * ```
 *
 * 只比较 `effectivePriority` 是不够的——
 * 那样 override / reasons 丢了也测不出来。
 */
class DynamicScheduleAdaptationPersistenceServiceTest {

    private val itemId =
        DynamicScheduleItemId("course-001")

    private val persistedAt =
        Instant.parse("2026-09-24T07:20:00Z")

    private fun adaptation(
        effectivePriority: Double = 0.65,
    ) =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.5,
            adaptationFactor = 1.3,
            effectivePriority = effectivePriority,
            reasons = listOf("Context matched after_class"),
        )

    private fun service(
        store:
            DynamicScheduleAdaptationPersistenceStore =
            InMemoryDynamicScheduleAdaptationPersistenceStore(),
    ) =
        DeterministicDynamicScheduleAdaptationPersistenceService(
            store = store,
            clock =
                object : Clock {
                    override fun now() = persistedAt
                },
        )

    @Test
    fun persistedAdaptationRoundTripsExactly() = runTest {

        val service = service()

        val original = adaptation()

        service.persist(original)

        val restored =
            assertNotNull(service.restore(itemId))

        assertEquals(original, restored.adaptation)
        assertEquals(itemId, restored.itemId)
        assertEquals(persistedAt, restored.persistedAt)
        assertEquals(1L, restored.schemaVersion)
        assertNull(restored.humanOverride)

        /*
         * 关键断言：整体相等。
         */
        assertEquals(original, restored.adaptation)
    }

    @Test
    fun humanOverrideRoundTripsAndStaysDistinguishable() = runTest {

        val service = service()

        val override =
            DynamicScheduleHumanOverride(
                itemId = itemId,
                priorityOverride = 0.9,
                createdAt = persistedAt,
                reason = "User explicitly marked important",
            )

        service.persist(
            adaptation = adaptation(),
            humanOverride = override,
        )

        val restored =
            assertNotNull(service.restore(itemId))

        /*
         * 重启之后仍然能区分：
         *
         * "自动算出来的 0.65"
         * "用户手动设的 0.9"
         *
         * 如果只存最终 priority，这个信息就永久丢了。
         */
        assertEquals(
            0.65,
            restored.adaptation.effectivePriority,
            absoluteTolerance = 0.000001,
        )

        assertEquals(override, restored.humanOverride)
    }

    @Test
    fun missingRecordReturnsNull() = runTest {

        val service = service()

        assertNull(
            service.restore(
                DynamicScheduleItemId("nobody"),
            ),
        )
    }

    @Test
    fun savingAgainReplacesTheRecord() = runTest {

        val service = service()

        service.persist(adaptation(effectivePriority = 0.65))
        service.persist(adaptation(effectivePriority = 0.42))

        val restored =
            assertNotNull(service.restore(itemId))

        assertEquals(
            0.42,
            restored.adaptation.effectivePriority,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun mismatchedOverrideIsRejected() = runTest {

        val service = service()

        val other =
            DynamicScheduleHumanOverride(
                itemId = DynamicScheduleItemId("other"),
                priorityOverride = 0.9,
                createdAt = persistedAt,
            )

        val failure =
            kotlin.runCatching {
                service.persist(
                    adaptation = adaptation(),
                    humanOverride = other,
                )
            }

        kotlin.test.assertTrue(
            failure.isFailure,
            "an override for a different item must not be stored",
        )
    }
}
