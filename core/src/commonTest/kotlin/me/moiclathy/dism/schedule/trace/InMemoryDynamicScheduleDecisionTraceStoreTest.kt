package me.moiclathy.dism.schedule.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class InMemoryDynamicScheduleDecisionTraceStoreTest {

    private val store =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val otherItemId =
        DynamicScheduleItemId("item-2")

    private fun record(
        itemId: DynamicScheduleItemId,
        createdAt: String,
        decisive: Boolean = false,
    ): DynamicScheduleDecisionTraceRecord {

        val trace =
            DynamicScheduleDecisionTrace(
                itemId = itemId,
            ).withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage.INPUT,

                message =
                    "Schedule item received",

                decisive = decisive,
            )

        return DynamicScheduleDecisionTraceRecord(
            trace = trace,
            createdAt = Instant.parse(createdAt),
            scheduleRevision = 1,
        )
    }

    @Test
    fun saveAndGetWorks() = runTest {

        val record =
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
            )

        store.save(record)

        val loaded =
            store.get(
                record.traceId,
            )

        assertNotNull(loaded)

        assertEquals(
            record.traceId,
            loaded.traceId,
        )

        assertEquals(
            itemId,
            loaded.itemId,
        )
    }

    @Test
    fun unknownTraceReturnsNull() = runTest {

        val result =
            store.get(
                DynamicScheduleDecisionTraceId(),
            )

        assertNull(result)
    }

    @Test
    fun recordsAreRetrievableByTraceId() = runTest {

        /*
         * 13.3：Trace Store 不再提供 "按 item 找最近"。
         *
         * 启动恢复的关系是：
         *
         * ```text
         * Snapshot ──traceId──► traceStore.get(traceId)
         * ```
         *
         * 所以这里验证的是"给定 traceId 一定拿得回记录"，
         * 而不是"按 item 猜测最近一条"。
         */
        val older =
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
            )

        val newer =
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T09:00:00Z",
            )

        store.save(older)
        store.save(newer)

        assertEquals(
            newer,
            store.get(newer.traceId),
        )

        assertEquals(
            older,
            store.get(older.traceId),
        )
    }

    @Test
    fun deleteRemovesOnlyThatTrace() = runTest {

        val kept =
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
            )

        val removed =
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T09:00:00Z",
            )

        store.save(kept)
        store.save(removed)

        store.delete(removed.traceId)

        assertNull(store.get(removed.traceId))

        assertNotNull(store.get(kept.traceId))

        assertEquals(
            1,
            store
                .query(
                    DynamicScheduleDecisionTraceQuery(
                        itemId = itemId,
                    ),
                )
                .totalMatched,
        )
    }

    @Test
    fun clearRemovesAllTraces() = runTest {

        store.save(
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
            ),
        )

        store.clear()

        assertEquals(
            0,
            store
                .query(
                    DynamicScheduleDecisionTraceQuery(),
                )
                .totalMatched,
        )
    }

    @Test
    fun queryCanFilterByItem() = runTest {

        store.save(
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
            ),
        )

        store.save(
            record(
                itemId = otherItemId,
                createdAt =
                    "2026-09-22T09:00:00Z",
            ),
        )

        val result =
            store.query(
                DynamicScheduleDecisionTraceQuery(
                    itemId = itemId,
                ),
            )

        assertEquals(
            1,
            result.totalMatched,
        )

        assertEquals(
            itemId,
            result.records.single().itemId,
        )
    }

    @Test
    fun queryCanFilterDecisiveTraces() = runTest {

        store.save(
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T08:00:00Z",
                decisive = false,
            ),
        )

        store.save(
            record(
                itemId = itemId,
                createdAt =
                    "2026-09-22T09:00:00Z",
                decisive = true,
            ),
        )

        val result =
            store.query(
                DynamicScheduleDecisionTraceQuery(
                    itemId = itemId,
                    decisiveOnly = true,
                ),
            )

        assertEquals(
            1,
            result.totalMatched,
        )

        assertTrue(
            result.records
                .single()
                .trace
                .decisiveEntries
                .isNotEmpty(),
        )
    }

    @Test
    fun queryCanFilterByStage() = runTest {

        val trace =
            DynamicScheduleDecisionTrace(
                itemId = itemId,
            )
                .withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage.INPUT,
                    message =
                        "Input",
                )
                .withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION,
                    message =
                        "Conflict resolved",
                    decisive = true,
                )

        store.save(
            DynamicScheduleDecisionTraceRecord(
                trace = trace,
                createdAt =
                    Instant.parse(
                        "2026-09-22T10:00:00Z",
                    ),
                scheduleRevision = 2,
            ),
        )

        val result =
            store.query(
                DynamicScheduleDecisionTraceQuery(
                    itemId = itemId,
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION,
                ),
            )

        assertEquals(
            1,
            result.totalMatched,
        )
    }
}
