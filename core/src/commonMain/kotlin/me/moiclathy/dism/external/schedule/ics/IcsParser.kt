package me.moiclathy.dism.external.schedule.ics

import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseError
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseErrorCode

/**
 * 基础 iCalendar 文档 Parser。
 */
class IcsParser(
    private val policy: IcsParserPolicy =
        IcsParserPolicy(),
) {

    fun parse(
        rawContent: String,
    ): Result<IcsParseResult> {

        if (rawContent.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "ICS content must not be blank."
                )
            )
        }

        val lines =
            unfoldLines(
                rawContent
            )

        if (
            lines.firstOrNull() !=
            "BEGIN:VCALENDAR"
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "ICS document must begin with BEGIN:VCALENDAR."
                )
            )
        }

        if (
            lines.lastOrNull() !=
            "END:VCALENDAR"
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "ICS document must end with END:VCALENDAR."
                )
            )
        }

        val calendarProperties =
            mutableListOf<IcsProperty>()

        val events =
            mutableListOf<List<IcsProperty>>()

        var currentEvent =
            mutableListOf<IcsProperty>()

        var insideCalendar =
            false

        var insideEvent =
            false

        lines.forEachIndexed { index, line ->

            when (line) {

                "BEGIN:VCALENDAR" -> {
                    insideCalendar = true
                }

                "END:VCALENDAR" -> {
                    insideCalendar = false
                }

                "BEGIN:VEVENT" -> {

                    if (!insideCalendar) {
                        throw IllegalArgumentException(
                            "VEVENT appears outside VCALENDAR at line ${index + 1}."
                        )
                    }

                    if (insideEvent) {
                        throw IllegalArgumentException(
                            "Nested VEVENT at line ${index + 1}."
                        )
                    }

                    insideEvent = true
                    currentEvent = mutableListOf()
                }

                "END:VEVENT" -> {

                    if (!insideEvent) {
                        throw IllegalArgumentException(
                            "END:VEVENT without BEGIN:VEVENT."
                        )
                    }

                    events +=
                        currentEvent.toList()

                    currentEvent =
                        mutableListOf()

                    insideEvent = false
                }

                else -> {

                    if (line.startsWith("BEGIN:") ||
                        line.startsWith("END:")
                    ) {
                        return@forEachIndexed
                    }

                    val property =
                        IcsProperty.parse(
                            line
                        )

                    if (insideEvent) {
                        currentEvent +=
                            property
                    } else {
                        calendarProperties +=
                            property
                    }
                }
            }
        }

        if (insideEvent) {
            return Result.failure(
                IllegalArgumentException(
                    "Unclosed VEVENT."
                )
            )
        }

        val version =
            calendarProperties
                .firstOrNull {
                    it.name == "VERSION"
                }
                ?.value

        if (
            policy.requireVersion &&
            version == null
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "ICS VERSION property is required."
                )
            )
        }

        val calendarName =
            calendarProperties
                .firstOrNull {
                    it.name == "X-WR-CALNAME"
                }
                ?.value

        return Result.success(
            IcsParseResult(
                events =
                    events,

                calendarName =
                    calendarName,

                version =
                    version,
            )
        )
    }

    private fun unfoldLines(
        rawContent: String,
    ): List<String> {

        val physicalLines =
            rawContent
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .split('\n')

        val unfolded =
            mutableListOf<String>()

        physicalLines.forEach { line ->

            if (
                line.startsWith(" ") ||
                line.startsWith("\t")
            ) {

                require(
                    unfolded.isNotEmpty()
                ) {
                    "ICS continuation line has no previous line."
                }

                unfolded[
                    unfolded.lastIndex
                ] += line.substring(1)

            } else if (
                line.isNotEmpty()
            ) {

                unfolded +=
                    line
            }
        }

        return unfolded
    }
}
