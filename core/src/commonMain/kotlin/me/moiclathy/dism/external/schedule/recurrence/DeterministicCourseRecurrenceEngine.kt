package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.time.Duration.Companion.days

/**
 * 确定性课程重复展开器。
 *
 * 当前重点：
 *
 * DAILY
 * WEEKLY
 *
 * MONTHLY / YEARLY 暂不进行日历语义猜测。
 */
class DeterministicCourseRecurrenceEngine(
    private val policy:
        CourseRecurrencePolicy =
        CourseRecurrencePolicy(),
) : CourseRecurrenceEngine {

    override fun expand(
        sourceEntryId: ExternalScheduleId,
        startAt: Instant,
        rule: CourseRecurrenceRule,
        exceptions: List<CourseRecurrenceException>,
        expansionStart: Instant,
        expansionEnd: Instant?,
    ): CourseRecurrenceResult {

        return try {

            validateFrequency(
                rule.frequency
            )

            val excluded =
                exceptions
                    .filterIsInstance<
                        CourseRecurrenceException.Excluded
                    >()
                    .map {
                        it.occurrenceAt
                    }
                    .toSet()

            val added =
                exceptions
                    .filterIsInstance<
                        CourseRecurrenceException.Added
                    >()
                    .map {
                        it.occurrenceAt
                    }
                    .toSet()

            val generated =
                when (rule.frequency) {

                    RecurrenceFrequency.DAILY ->
                        expandDaily(
                            sourceEntryId,
                            startAt,
                            rule,
                            excluded,
                            expansionStart,
                            expansionEnd,
                        )

                    RecurrenceFrequency.WEEKLY ->
                        expandWeekly(
                            sourceEntryId,
                            startAt,
                            rule,
                            excluded,
                            expansionStart,
                            expansionEnd,
                        )

                    RecurrenceFrequency.MONTHLY,
                    RecurrenceFrequency.YEARLY ->
                        throw IllegalArgumentException(
                            "MONTHLY/YEARLY recurrence expansion is deferred."
                        )

                    RecurrenceFrequency.SECONDLY,
                    RecurrenceFrequency.MINUTELY,
                    RecurrenceFrequency.HOURLY ->
                        throw IllegalArgumentException(
                            "High-frequency recurrence is not supported for course schedules."
                        )
                }

            val merged =
                (
                    generated.map {
                        it.occurrenceAt
                    } + added
                )
                    .distinct()
                    .filter {
                        it >= expansionStart
                    }
                    .filter {
                        expansionEnd == null ||
                            it <= expansionEnd
                    }
                    .sorted()

            val instances =
                merged
                    .take(
                        policy.maxOccurrences
                    )
                    .mapIndexed { index, occurrenceAt ->

                        CourseRecurrenceInstance(
                            instanceId =
                                buildInstanceId(
                                    sourceEntryId,
                                    occurrenceAt,
                                ),

                            sourceEntryId =
                                sourceEntryId,

                            occurrenceAt =
                                occurrenceAt,

                            occurrenceIndex =
                                index,
                        )
                    }

            val warnings =
                if (
                    merged.size >
                    policy.maxOccurrences
                ) {
                    listOf(
                        "Recurrence expansion reached maxOccurrences=${policy.maxOccurrences}."
                    )
                } else {
                    emptyList()
                }

            CourseRecurrenceResult.success(
                expansion =
                    CourseRecurrenceExpansion(
                        sourceEntryId =
                            sourceEntryId,

                        instances =
                            instances,
                    ),

                warnings =
                    warnings,
            )

        } catch (
            exception: IllegalArgumentException
        ) {

            CourseRecurrenceResult.failure(
                exception.message
                    ?: "Recurrence expansion failed."
            )
        }
    }

    private fun expandDaily(
        sourceEntryId: ExternalScheduleId,
        startAt: Instant,
        rule: CourseRecurrenceRule,
        excluded: Set<Instant>,
        expansionStart: Instant,
        expansionEnd: Instant?,
    ): List<CourseRecurrenceInstance> {

        val result =
            mutableListOf<CourseRecurrenceInstance>()

        var occurrence =
            startAt

        var occurrenceNumber =
            0

        while (
            result.size <
            policy.maxOccurrences
        ) {

            if (
                rule.count != null &&
                occurrenceNumber >=
                rule.count
            ) {
                break
            }

            if (
                rule.until != null &&
                occurrence >
                rule.until
            ) {
                break
            }

            if (
                expansionEnd != null &&
                occurrence >
                expansionEnd
            ) {
                break
            }

            if (
                occurrence >=
                expansionStart &&
                occurrence !in excluded
            ) {

                result +=
                    CourseRecurrenceInstance(
                        instanceId =
                            buildInstanceId(
                                sourceEntryId,
                                occurrence,
                            ),

                        sourceEntryId =
                            sourceEntryId,

                        occurrenceAt =
                            occurrence,

                        occurrenceIndex =
                            occurrenceNumber,
                    )
            }

            occurrenceNumber++

            occurrence =
                occurrence +
                    rule.interval.days

            /**
             * 防止没有任何终止条件的规则无限运行。
             */
            if (
                rule.count == null &&
                rule.until == null &&
                expansionEnd == null &&
                occurrenceNumber >=
                policy.maxOccurrences
            ) {
                break
            }
        }

        return result
    }

    private fun expandWeekly(
        sourceEntryId: ExternalScheduleId,
        startAt: Instant,
        rule: CourseRecurrenceRule,
        excluded: Set<Instant>,
        expansionStart: Instant,
        expansionEnd: Instant?,
    ): List<CourseRecurrenceInstance> {

        val result =
            mutableListOf<CourseRecurrenceInstance>()

        val days =
            if (rule.byDay.isEmpty()) {
                listOf(
                    dayOfWeekApproximation(
                        startAt
                    )
                )
            } else {
                rule.byDay
            }

        val orderedDays =
            days.sortedBy {
                dayOrder(it)
            }

        var currentWeekStart =
            startAt

        var occurrenceNumber =
            0

        while (
            result.size <
            policy.maxOccurrences
        ) {

            if (
                rule.count != null &&
                occurrenceNumber >=
                rule.count
            ) {
                break
            }

            val currentDay =
                dayOfWeekApproximation(
                    currentWeekStart
                )

            val weekOffset =
                dayOffset(
                    currentDay,
                    orderedDays.first(),
                )

            var generatedThisWeek =
                false

            orderedDays.forEach { day ->

                if (
                    result.size >=
                    policy.maxOccurrences
                ) {
                    return@forEach
                }

                val offset =
                    dayOffset(
                        currentDay,
                        day,
                    )

                val occurrence =
                    currentWeekStart +
                        (offset.toLong()).days

                if (
                    occurrence < startAt
                ) {
                    return@forEach
                }

                if (
                    rule.until != null &&
                    occurrence >
                    rule.until
                ) {
                    return@forEach
                }

                if (
                    expansionEnd != null &&
                    occurrence >
                    expansionEnd
                ) {
                    return@forEach
                }

                if (
                    occurrence >=
                    expansionStart
                ) {

                    /*
                     * COUNT 的语义按 RFC 5545：
                     *
                     * 先按规则生成 occurrence，
                     * 再由 EXDATE 排除。
                     *
                     * 因此被排除的 occurrence
                     * 同样占用 COUNT 名额。
                     */
                    val occurrenceSequence =
                        occurrenceNumber

                    occurrenceNumber++

                    generatedThisWeek = true

                    if (
                        occurrence !in excluded
                    ) {

                        result +=
                            CourseRecurrenceInstance(
                                instanceId =
                                    buildInstanceId(
                                        sourceEntryId,
                                        occurrence,
                                    ),

                                sourceEntryId =
                                    sourceEntryId,

                                occurrenceAt =
                                    occurrence,

                                occurrenceIndex =
                                    occurrenceSequence,
                            )
                    }
                }
            }

            if (
                rule.count != null &&
                occurrenceNumber >=
                rule.count
            ) {
                break
            }

            /**
             * 当前实现按完整 7 天推进。
             *
             * interval=1：
             * 每周
             *
             * interval=2：
             * 每两周
             */
            currentWeekStart =
                currentWeekStart +
                    (7L * rule.interval).days

            if (
                !generatedThisWeek &&
                expansionEnd != null &&
                currentWeekStart >
                expansionEnd
            ) {
                break
            }

            if (
                !generatedThisWeek &&
                rule.until == null &&
                expansionEnd == null &&
                rule.count == null &&
                result.size >=
                policy.maxOccurrences
            ) {
                break
            }
        }

        return result
    }

    private fun validateFrequency(
        frequency: RecurrenceFrequency,
    ) {

        when (frequency) {

            RecurrenceFrequency.DAILY -> {
                require(policy.allowDaily) {
                    "DAILY recurrence is disabled."
                }
            }

            RecurrenceFrequency.WEEKLY -> {
                require(policy.allowWeekly) {
                    "WEEKLY recurrence is disabled."
                }
            }

            RecurrenceFrequency.MONTHLY -> {
                require(policy.allowMonthly) {
                    "MONTHLY recurrence is disabled."
                }
            }

            RecurrenceFrequency.YEARLY -> {
                require(policy.allowYearly) {
                    "YEARLY recurrence is disabled."
                }
            }

            else -> Unit
        }
    }

    private fun buildInstanceId(
        sourceEntryId: ExternalScheduleId,
        occurrenceAt: Instant,
    ): String =
        "${sourceEntryId.value}@${occurrenceAt}"

    /**
     * 11.6 当前不引入时区转换。
     *
     * 因此这里使用 Instant 的 epoch day
     * 做稳定的星期计算。
     */
    private fun dayOfWeekApproximation(
        instant: Instant,
    ): DayOfWeek {

        val days =
            instant.epochSeconds /
                86_400L

        val index =
            ((days + 4) % 7 + 7) % 7

        return when (index.toInt()) {
            0 -> DayOfWeek.THURSDAY
            1 -> DayOfWeek.FRIDAY
            2 -> DayOfWeek.SATURDAY
            3 -> DayOfWeek.SUNDAY
            4 -> DayOfWeek.MONDAY
            5 -> DayOfWeek.TUESDAY
            else -> DayOfWeek.WEDNESDAY
        }
    }

    private fun dayOffset(
        from: DayOfWeek,
        to: DayOfWeek,
    ): Int =
        (
            dayOrder(to) -
                dayOrder(from) +
                7
            ) % 7

    private fun dayOrder(
        day: DayOfWeek,
    ): Int =
        when (day) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
}
