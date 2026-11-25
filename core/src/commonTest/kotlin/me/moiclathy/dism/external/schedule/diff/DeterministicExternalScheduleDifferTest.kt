package me.moiclathy.dism.external.schedule.diff

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeterministicExternalScheduleDifferTest {

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
        title: String = "实验动物学",
        startAt: Instant = time,
        endAt: Instant? =
            Instant.parse(
                "2026-09-10T02:00:00Z"
            ),
        active: Boolean = true,
        fingerprint: String? = "fp-$key",
    ): ExternalSchedulePersistenceRecord {

        return ExternalSchedulePersistenceRecord(

            externalScheduleId =
                scheduleId,

            externalKey =
                key,

            eventId =
                "event-$key",

            title =
                title,

            startAt =
                startAt,

            endAt =
                endAt,

            locationEntityId =
                "location-001",

            instructorEntityId =
                "teacher-001",

            courseEntityId =
                "course-001",

            lastSeenAt =
                time,

            active =
                active,

            fingerprint =
                fingerprint,
        )
    }

    private val differ =
        DeterministicExternalScheduleDiffer()

    @Test
    fun `new record is detected as added`() {

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-001")
                    ),
            )

        assertTrue(
            result.successful
        )

        val diff =
            result.diff!!

        assertEquals(
            1,
            diff.addedCount
        )

        assertEquals(
            0,
            diff.changedCount
        )

        assertEquals(
            0,
            diff.removedCount
        )
    }

    @Test
    fun `missing record is detected as removed`() {

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(
                        record("entry-001")
                    ),

                current = emptyList(),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .removedCount
        )
    }

    @Test
    fun `same record is unchanged`() {

        val old =
            record("entry-001")

        val new =
            old.copy()

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),
            )

        assertTrue(
            result.successful
        )

        val diff =
            result.diff!!

        assertEquals(
            1,
            diff.unchangedCount
        )

        assertEquals(
            0,
            diff.changedCount
        )
    }

    @Test
    fun `title change is detected`() {

        val old =
            record(
                key = "entry-001",
                title = "实验动物学",
            )

        val new =
            record(
                key = "entry-001",
                title = "实验动物学实验",
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .changedCount
        )
    }

    @Test
    fun `time change is detected`() {

        val old =
            record(
                key = "entry-001",
                startAt =
                    Instant.parse(
                        "2026-09-10T01:00:00Z"
                    ),
            )

        val new =
            record(
                key = "entry-001",
                startAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                endAt =
                    Instant.parse(
                        "2026-09-10T04:00:00Z"
                    ),
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .changedCount
        )
    }

    @Test
    fun `fingerprint change is detected`() {

        val old =
            record(
                key = "entry-001",
                fingerprint = "old",
            )

        val new =
            record(
                key = "entry-001",
                fingerprint = "new",
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .changedCount
        )
    }

    @Test
    fun `fingerprint can be ignored by policy`() {

        val old =
            record(
                key = "entry-001",
                fingerprint = "old",
            )

        val new =
            record(
                key = "entry-001",
                fingerprint = "new",
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),

                policy =
                    ExternalScheduleDiffPolicy(
                        compareFingerprint = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .unchangedCount
        )
    }

    @Test
    fun `active change is detected`() {

        val old =
            record(
                key = "entry-001",
                active = true,
            )

        val new =
            record(
                key = "entry-001",
                active = false,
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .changedCount
        )
    }

    @Test
    fun `active change can be ignored`() {

        val old =
            record(
                key = "entry-001",
                active = true,
            )

        val new =
            record(
                key = "entry-001",
                active = false,
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(old),

                current =
                    listOf(new),

                policy =
                    ExternalScheduleDiffPolicy(
                        compareActive = false
                    ),
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            1,
            result.diff!!
                .unchangedCount
        )
    }

    @Test
    fun `multiple changes are classified independently`() {

        val previous =
            listOf(
                record("unchanged"),

                record(
                    key = "changed",
                    title = "旧课程",
                ),

                record("removed"),
            )

        val current =
            listOf(
                record("unchanged"),

                record(
                    key = "changed",
                    title = "新课程",
                ),

                record("added"),
            )

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    previous,

                current =
                    current,
            )

        assertTrue(
            result.successful
        )

        val diff =
            result.diff!!

        assertEquals(
            1,
            diff.addedCount
        )

        assertEquals(
            1,
            diff.changedCount
        )

        assertEquals(
            1,
            diff.unchangedCount
        )

        assertEquals(
            1,
            diff.removedCount
        )
    }

    @Test
    fun `duplicate previous key is rejected`() {

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous =
                    listOf(
                        record("entry-001"),
                        record("entry-001"),
                    ),

                current = emptyList(),
            )

        assertFalse(
            result.successful
        )

        assertNotNull(
            result.errors
        )

        assertTrue(
            result.errors.isNotEmpty()
        )
    }

    @Test
    fun `duplicate current key is rejected`() {

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-001"),
                        record("entry-001"),
                    ),
            )

        assertFalse(
            result.successful
        )

        assertTrue(
            result.errors.isNotEmpty()
        )
    }

    @Test
    fun `result is deterministic`() {

        val previous =
            listOf(
                record("entry-003"),
                record("entry-001"),
                record("entry-002"),
            )

        val current =
            listOf(
                record(
                    "entry-002",
                    title = "修改课程",
                ),

                record("entry-004"),

                record("entry-001"),
            )

        val first =
            differ.diff(
                scheduleId,
                previous,
                current,
            )

        val second =
            differ.diff(
                scheduleId,
                previous,
                current,
            )

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun `diff records are sorted by external key`() {

        val result =
            differ.diff(
                scheduleId = scheduleId,

                previous = emptyList(),

                current =
                    listOf(
                        record("entry-003"),
                        record("entry-001"),
                        record("entry-002"),
                    ),
            )

        val keys =
            result.diff!!
                .records
                .map {
                    it.externalKey
                }

        assertEquals(
            listOf(
                "entry-001",
                "entry-002",
                "entry-003",
            ),
            keys,
        )
    }
}
