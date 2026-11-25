package me.moiclathy.dism.external.schedule.event

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalScheduleEventFactoryAdapterTest {

    private val start =
        Instant.parse(
            "2026-09-14T01:00:00Z"
        )

    private val end =
        Instant.parse(
            "2026-09-14T02:30:00Z"
        )

    private fun draft(
        externalKey: String = "ics:course-001",
        startAt: Instant? = start,
        endAt: Instant? = end,
    ): ExternalScheduleEventDraft {

        return ExternalScheduleEventDraft(
            sourceEntryId =
                ExternalScheduleId(
                    "course-001"
                ),

            externalKey =
                externalKey,

            title =
                "实验动物学",

            startAt =
                startAt,

            endAt =
                endAt,

            location =
                "实验楼 201",

            instructor =
                "张老师",

            courseCode =
                "LAB101",

            description =
                "实验动物学课程",

            metadata =
                mapOf(
                    "source" to "ics"
                ),
        )
    }

    @Test
    fun factoryShouldCreateEvent() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> { input ->

                "EVENT:${input.externalKey}"
            }

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft()
                )
            )

        assertTrue(
            result.successful
        )

        assertEquals(
            "EVENT:ics:course-001",
            result.event
        )

        assertEquals(
            "ics:course-001",
            result.externalKey
        )
    }

    @Test
    fun factoryShouldReceiveOriginalDraft() {

        var received:
            ExternalScheduleEventFactoryInput? =
            null

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                received = it

                "EVENT"
            }

        val original =
            draft()

        adapter.create(
            ExternalScheduleEventFactoryInput(
                original
            )
        )

        assertEquals(
            original,
            received?.draft
        )
    }

    @Test
    fun nullFactoryResultShouldFail() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                null
            }

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft()
                )
            )

        assertFalse(
            result.successful
        )

        assertNull(
            result.event
        )

        assertTrue(
            result.errors.any {
                it.code ==
                    ExternalScheduleEventFactoryError.Code
                        .FACTORY_REJECTED
            }
        )
    }

    @Test
    fun factoryExceptionShouldBecomeStructuredError() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                throw IllegalArgumentException(
                    "invalid event"
                )
            }

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft()
                )
            )

        assertFalse(
            result.successful
        )

        assertTrue(
            result.errors.any {
                it.code ==
                    ExternalScheduleEventFactoryError.Code
                        .INVALID_DRAFT
            }
        )
    }

    @Test
    fun blankExternalKeyShouldBeRejectedByTheDraftModel() {

        /*
         * ExternalScheduleEventDraft 自身
         * 已经拒绝空白 externalKey。
         *
         * Adapter 的 INVALID_EXTERNAL_KEY 检查
         * 是防御性代码，只有在 Draft 不变量
         * 被放开时才会真正触发。
         */
        assertFailsWith<IllegalArgumentException> {
            draft(
                externalKey = ""
            )
        }
    }

    @Test
    fun unscheduledEventShouldBeAllowedByDefault() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                "EVENT"
            }

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft(
                        startAt = null,
                        endAt = null,
                    )
                )
            )

        assertTrue(
            result.successful
        )

        assertTrue(
            result.hasWarnings
        )
    }

    @Test
    fun unscheduledEventCanBeRejected() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String>(
                factory = {
                    "EVENT"
                },
                policy =
                    ExternalScheduleEventFactoryPolicy(
                        allowUnscheduled =
                            false
                    ),
            )

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft(
                        startAt = null,
                        endAt = null,
                    )
                )
            )

        assertFalse(
            result.successful
        )

        assertTrue(
            result.errors.any {
                it.code ==
                    ExternalScheduleEventFactoryError.Code
                        .POLICY_REJECTED
            }
        )
    }

    @Test
    fun missingEndTimeCanBeRejected() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String>(
                factory = {
                    "EVENT"
                },
                policy =
                    ExternalScheduleEventFactoryPolicy(
                        allowMissingEndTime =
                            false
                    ),
            )

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft(
                        startAt = start,
                        endAt = null,
                    )
                )
            )

        assertFalse(
            result.successful
        )
    }

    @Test
    fun missingEndTimeProducesWarningByDefault() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                "EVENT"
            }

        val result =
            adapter.create(
                ExternalScheduleEventFactoryInput(
                    draft(
                        startAt = start,
                        endAt = null,
                    )
                )
            )

        assertTrue(
            result.successful
        )

        assertTrue(
            result.warnings.any {
                it.contains("no end time")
            }
        )
    }

    @Test
    fun sameInputShouldProduceSameExternalKey() {

        val adapter =
            ExternalScheduleEventFactoryAdapter<String> {
                "EVENT:${it.externalKey}"
            }

        val input =
            ExternalScheduleEventFactoryInput(
                draft()
            )

        val first =
            adapter.create(input)

        val second =
            adapter.create(input)

        assertEquals(
            first.externalKey,
            second.externalKey
        )

        assertEquals(
            first.event,
            second.event
        )
    }
}
