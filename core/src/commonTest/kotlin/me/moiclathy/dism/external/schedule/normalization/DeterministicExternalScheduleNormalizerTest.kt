package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleMetadata
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeterministicExternalScheduleNormalizerTest {

    private val normalizedAt =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private fun schedule(): ExternalSchedule {

        return ExternalSchedule(
            id =
                ExternalScheduleId(
                    "schedule-001"
                ),

            source =
                ExternalScheduleOrigin.ICS,

            name =
                "  2026 秋季课程表  ",

            importedAt =
                normalizedAt,

            entries =
                listOf(
                    ExternalScheduleEntry(
                        id =
                            ExternalScheduleId(
                                "entry-001"
                            ),

                        title =
                            "  高等数学   ",

                        source =
                            ExternalScheduleOrigin.ICS,

                        startAt =
                            Instant.parse(
                                "2026-09-16T08:00:00Z"
                            ),

                        endAt =
                            Instant.parse(
                                "2026-09-16T09:40:00Z"
                            ),

                        location =
                            " A 301 ",

                        instructor =
                            " 张老师 ",

                        courseCode =
                            " math 101 ",

                        description =
                            "  第一周课程  ",

                        metadata =
                            ExternalScheduleMetadata(
                                mapOf(
                                    "X-TEST" to
                                        "value",
                                )
                            ),
                    )
                ),
        )
    }

    @Test
    fun textShouldBeNormalized() {

        val result =
            DeterministicExternalScheduleNormalizer()
                .normalize(
                    schedule =
                        schedule(),

                    normalizedAt =
                        normalizedAt,
                )

        assertTrue(
            result.success
        )

        val normalized =
            result.schedule!!

        assertEquals(
            "2026 秋季课程表",
            normalized.name,
        )

        val entry =
            normalized.entries.first()

        assertEquals(
            "高等数学",
            entry.title,
        )

        assertEquals(
            "A301",
            entry.location,
        )

        assertEquals(
            "张老师",
            entry.instructor,
        )

        assertEquals(
            "MATH101",
            entry.courseCode,
        )

        assertEquals(
            "第一周课程",
            entry.description,
        )
    }

    @Test
    fun originalValuesShouldBePreservedWhenChanged() {

        val result =
            DeterministicExternalScheduleNormalizer()
                .normalize(
                    schedule =
                        schedule(),

                    normalizedAt =
                        normalizedAt,
                )

        val entry =
            result.schedule!!
                .entries
                .first()

        assertEquals(
            "  高等数学   ",
            entry.originalTitle,
        )

        assertEquals(
            " A 301 ",
            entry.originalLocation,
        )

        assertEquals(
            " math 101 ",
            entry.originalCourseCode,
        )
    }

    @Test
    fun metadataShouldBePreserved() {

        val result =
            DeterministicExternalScheduleNormalizer()
                .normalize(
                    schedule =
                        schedule(),

                    normalizedAt =
                        normalizedAt,
                )

        val entry =
            result.schedule!!
                .entries
                .first()

        assertEquals(
            "value",
            entry.metadata["X-TEST"],
        )
    }

    @Test
    fun normalizationShouldBeDeterministic() {

        val normalizer =
            DeterministicExternalScheduleNormalizer()

        val input =
            schedule()

        val first =
            normalizer.normalize(
                schedule =
                    input,

                normalizedAt =
                    normalizedAt,
            )

        val second =
            normalizer.normalize(
                schedule =
                    input,

                normalizedAt =
                    normalizedAt,
            )

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun alreadyNormalizedDataShouldRemainStable() {

        val normalizer =
            DeterministicExternalScheduleNormalizer()

        val first =
            normalizer.normalize(
                schedule =
                    schedule(),

                normalizedAt =
                    normalizedAt,
            )

        val normalized =
            first.schedule

        assertNotNull(
            normalized
        )

        val reconstructed =
            ExternalSchedule(
                id =
                    normalized.id,

                source =
                    normalized.source,

                name =
                    normalized.name,

                importedAt =
                    normalized.normalizedAt,

                entries =
                    normalized.entries.map {
                        ExternalScheduleEntry(
                            id =
                                it.id,

                            title =
                                it.title,

                            source =
                                normalized.source,

                            startAt =
                                it.startAt,

                            endAt =
                                it.endAt,

                            location =
                                it.location,

                            instructor =
                                it.instructor,

                            courseCode =
                                it.courseCode,

                            description =
                                it.description,

                            metadata =
                                ExternalScheduleMetadata(
                                    it.metadata
                                ),
                        )
                    },
            )

        val second =
            normalizer.normalize(
                schedule =
                    reconstructed,

                normalizedAt =
                    normalizedAt,
            )

        assertTrue(
            second.success
        )

        assertEquals(
            normalized.entries.size,
            second.schedule!!.entries.size,
        )
    }
}
