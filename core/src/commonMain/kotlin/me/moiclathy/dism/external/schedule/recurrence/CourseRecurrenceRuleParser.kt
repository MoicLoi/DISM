package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant

/**
 * RRULE 文本解析器。
 *
 * 当前支持：
 *
 * FREQ
 * INTERVAL
 * COUNT
 * UNTIL
 * BYDAY
 *
 * 其他 RFC 5545 参数暂时拒绝，
 * 避免出现“看起来支持、实际语义错误”的情况。
 */
class CourseRecurrenceRuleParser {

    fun parse(
        value: String,
    ): Result<CourseRecurrenceRule> {

        if (value.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "RRULE value must not be blank."
                )
            )
        }

        val parts =
            value
                .removePrefix("RRULE:")
                .split(';')

        val values =
            mutableMapOf<String, String>()

        parts.forEach { part ->

            val separator =
                part.indexOf('=')

            if (separator <= 0) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid RRULE component: $part"
                    )
                )
            }

            val key =
                part
                    .substring(
                        0,
                        separator,
                    )
                    .uppercase()

            val componentValue =
                part.substring(
                    separator + 1
                )

            if (key in values) {
                return Result.failure(
                    IllegalArgumentException(
                        "Duplicate RRULE component: $key"
                    )
                )
            }

            values[key] =
                componentValue
        }

        return try {

            val frequency =
                parseFrequency(
                    values["FREQ"]
                        ?: throw IllegalArgumentException(
                            "RRULE FREQ is required."
                        )
                )

            val interval =
                values["INTERVAL"]
                    ?.toIntOrNull()
                    ?: 1

            val count =
                values["COUNT"]
                    ?.toIntOrNull()

            val until =
                values["UNTIL"]
                    ?.let {
                        parseInstant(it)
                    }

            val byDay =
                values["BYDAY"]
                    ?.split(',')
                    ?.filter {
                        it.isNotBlank()
                    }
                    ?.map {
                        parseDay(it)
                    }
                    ?: emptyList()

            Result.success(
                CourseRecurrenceRule(
                    frequency =
                        frequency,

                    interval =
                        interval,

                    count =
                        count,

                    until =
                        until,

                    byDay =
                        byDay,
                )
            )

        } catch (
            exception: IllegalArgumentException
        ) {

            Result.failure(
                exception
            )
        }
    }

    private fun parseFrequency(
        value: String,
    ): RecurrenceFrequency =
        try {
            RecurrenceFrequency.valueOf(
                value.uppercase()
            )
        } catch (
            exception: IllegalArgumentException
        ) {
            throw IllegalArgumentException(
                "Unsupported RRULE frequency: $value"
            )
        }

    private fun parseDay(
        value: String,
    ): DayOfWeek =
        when (
            value.uppercase()
        ) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY

            else ->
                throw IllegalArgumentException(
                    "Unsupported RRULE BYDAY value: $value"
                )
        }

    private fun parseInstant(
        value: String,
    ): Instant {

        /**
         * RRULE UNTIL 在当前 DISM 版本
         * 只接受 UTC 格式：
         *
         * 20260916T080000Z
         */
        if (
            value.length != 16 ||
            !value.endsWith('Z')
        ) {
            throw IllegalArgumentException(
                "RRULE UNTIL must use UTC datetime format: $value"
            )
        }

        val iso =
            buildString {

                append(
                    value.substring(0, 4)
                )
                append('-')

                append(
                    value.substring(4, 6)
                )
                append('-')

                append(
                    value.substring(6, 8)
                )

                append('T')

                append(
                    value.substring(9, 11)
                )
                append(':')

                append(
                    value.substring(11, 13)
                )
                append(':')

                append(
                    value.substring(13, 15)
                )

                append('Z')
            }

        return Instant.parse(
            iso
        )
    }
}
