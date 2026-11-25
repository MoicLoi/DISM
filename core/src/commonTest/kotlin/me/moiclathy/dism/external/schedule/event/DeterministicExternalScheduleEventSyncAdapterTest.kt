package me.moiclathy.dism.external.schedule.event

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffRecord
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffType
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicExternalScheduleEventSyncAdapterTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val time =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private fun record(
        key: String,
        eventId: String? = "event-$key",
        title: String = "实验动物学",
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                eventId,

            title =
                title,

            startAt =
                time,

            endAt =
                Instant.parse(
                    "2026-09-10T02:00:00Z"
                ),

            locationEntityId =
                "location-001",

            instructorEntityId =
                "teacher-001",

            courseEntityId =
                "course-001",

            lastSeenAt =
                time,

            active =
                true,

            fingerprint =
                "fp-$key",
        )
    }

    private class FakeHandler :
        ExternalScheduleEventSyncAdapter {

        val creates =
            mutableListOf<String>()

        val updates =
            mutableListOf<String>()

        val removals =
            mutableListOf<String>()

        var updateEventId:
            String? = null

        override fun create(
            change:
                ExternalScheduleSyncChange,
        ): String? {

            creates +=
                change.externalKey

            return "created-${change.externalKey}"
        }

        override fun update(
            change:
                ExternalScheduleSyncChange,
        ): String? {

            updates +=
                change.externalKey

            return updateEventId
                ?: change.diff
                    .previous
                    ?.eventId
        }

        override fun handleRemoved(
            change:
                ExternalScheduleSyncChange,
        ): String? {

            removals +=
                change.externalKey

            return change.diff
                .previous
                ?.eventId
        }
    }

    private fun addedChange(
        key: String = "entry-001",
    ): ExternalScheduleSyncChange {

        return ExternalScheduleSyncChange(

            externalKey =
                key,

            action =
                ExternalScheduleSyncAction
                    .CREATE_EVENT,

            diff =
                ExternalScheduleDiffRecord(

                    externalKey =
                        key,

                    type =
                        ExternalScheduleDiffType
                            .ADDED,

                    previous =
                        null,

                    current =
                        record(key),
                ),
        )
    }

    private fun changedChange(
        key: String = "entry-001",
        previousEventId: String? =
            "event-entry-001",
    ): ExternalScheduleSyncChange {

        return ExternalScheduleSyncChange(

            externalKey =
                key,

            action =
                ExternalScheduleSyncAction
                    .UPDATE_EVENT,

            diff =
                ExternalScheduleDiffRecord(

                    externalKey =
                        key,

                    type =
                        ExternalScheduleDiffType
                            .CHANGED,

                    previous =
                        record(
                            key = key,
                            eventId =
                                previousEventId,
                            title = "旧课程",
                        ),

                    current =
                        record(
                            key = key,
                            eventId =
                                previousEventId,
                            title = "新课程",
                        ),
                ),
        )
    }

    private fun removedChange(
        key: String = "entry-001",
    ): ExternalScheduleSyncChange {

        return ExternalScheduleSyncChange(

            externalKey =
                key,

            action =
                ExternalScheduleSyncAction
                    .HANDLE_REMOVED_EVENT,

            diff =
                ExternalScheduleDiffRecord(

                    externalKey =
                        key,

                    type =
                        ExternalScheduleDiffType
                            .REMOVED,

                    previous =
                        record(key),

                    current =
                        null,
                ),
        )
    }

    @Test
    fun `create delegates to event handler`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,
            )

        val result =
            adapter.execute(
                addedChange()
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            listOf("entry-001"),
            handler.creates
        )

        assertEquals(
            "created-entry-001",
            result.eventId
        )
    }

    @Test
    fun `update delegates to event handler`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,
            )

        val result =
            adapter.execute(
                changedChange()
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            listOf("entry-001"),
            handler.updates
        )

        assertEquals(
            "event-entry-001",
            result.eventId
        )
    }

    @Test
    fun `update preserves event id`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,
            )

        val result =
            adapter.execute(
                changedChange(
                    previousEventId =
                        "event-original",
                )
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            "event-original",
            result.eventId
        )
    }

    @Test
    fun `changed record without mapping can recreate`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,

                policy =
                    ExternalScheduleEventSyncPolicy(
                        recreateMissingEventMapping =
                            true
                    ),
            )

        val result =
            adapter.execute(
                changedChange(
                    previousEventId =
                        null,
                )
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            listOf("entry-001"),
            handler.creates
        )

        assertEquals(
            "created-entry-001",
            result.eventId
        )
    }

    @Test
    fun `changed record without mapping can fail when recreation disabled`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,

                policy =
                    ExternalScheduleEventSyncPolicy(
                        recreateMissingEventMapping =
                            false
                    ),
            )

        val result =
            adapter.execute(
                changedChange(
                    previousEventId =
                        null,
                )
            )

        assertFalse(
            result.successful
        )

        assertEquals(
            ExternalScheduleEventSyncError
                .MISSING_EVENT_MAPPING,

            result.error
        )
    }

    @Test
    fun `removed record delegates to handler`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,
            )

        val result =
            adapter.execute(
                removedChange()
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            listOf("entry-001"),
            handler.removals
        )

        assertEquals(
            "event-entry-001",
            result.eventId
        )
    }

    @Test
    fun `removed event handling can be disabled`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,

                policy =
                    ExternalScheduleEventSyncPolicy(
                        handleRemovedEvents =
                            false
                    ),
            )

        val result =
            adapter.execute(
                removedChange()
            )

        assertTrue(
            result.successful
        )

        assertTrue(
            handler.removals.isEmpty()
        )

        assertEquals(
            "event-entry-001",
            result.eventId
        )
    }

    @Test
    fun `no op does not call handler`() {

        val handler =
            FakeHandler()

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,
            )

        val change =
            ExternalScheduleSyncChange(

                externalKey =
                    "entry-001",

                action =
                    ExternalScheduleSyncAction
                        .NO_OP,

                diff =
                    ExternalScheduleDiffRecord(

                        externalKey =
                            "entry-001",

                        type =
                            ExternalScheduleDiffType
                                .UNCHANGED,

                        previous =
                            record("entry-001"),

                        current =
                            record("entry-001"),
                    ),
            )

        val result =
            adapter.execute(
                change
            )

        assertTrue(
            result.successful
        )

        assertTrue(
            handler.creates.isEmpty()
        )

        assertTrue(
            handler.updates.isEmpty()
        )

        assertTrue(
            handler.removals.isEmpty()
        )
    }

    @Test
    fun `blank external key is rejected by the record model`() {

        /*
         * ExternalSchedulePersistenceRecord
         * 自身已经拒绝空白 externalKey。
         *
         * Adapter 的 INVALID_EXTERNAL_KEY 检查
         * 是防御性代码，只有在记录模型不变量
         * 被放开时才会真正触发。
         */
        assertFailsWith<IllegalArgumentException> {
            record(
                key = " "
            )
        }
    }

    @Test
    fun `update cannot silently replace event id`() {

        val handler =
            FakeHandler()

        handler.updateEventId =
            "event-new"

        val adapter =
            DeterministicExternalScheduleEventSyncAdapter(
                eventHandler =
                    handler,

                policy =
                    ExternalScheduleEventSyncPolicy(
                        preserveEventIdOnUpdate =
                            true
                    ),
            )

        val result =
            adapter.execute(
                changedChange(
                    previousEventId =
                        "event-old",
                )
            )

        assertFalse(
            result.successful
        )

        assertEquals(
            ExternalScheduleEventSyncError
                .EVENT_ID_NOT_PRESERVED,

            result.error
        )
    }
}
