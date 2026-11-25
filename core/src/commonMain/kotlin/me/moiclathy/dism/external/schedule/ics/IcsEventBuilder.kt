package me.moiclathy.dism.external.schedule.ics

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleMetadata
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseError
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseErrorCode

/**
 * 将一个 VEVENT 转换成 ExternalScheduleEntry。
 */
class IcsEventBuilder(
    private val policy: IcsParserPolicy =
        IcsParserPolicy(),
) {

    fun build(
        properties: List<IcsProperty>,
        fallbackId: String,
    ): Result<ExternalScheduleEntry> {

        val grouped =
            properties.groupBy {
                it.name
            }

        val uid =
            grouped["UID"]
                ?.firstOrNull()
                ?.value
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: fallbackId

        val summary =
            grouped["SUMMARY"]
                ?.firstOrNull()
                ?.value

        if (
            policy.requireSummary &&
            summary.isNullOrBlank()
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "VEVENT SUMMARY is required."
                )
            )
        }

        val title =
            summary
                ?.takeIf { it.isNotBlank() }
                ?: "Untitled Event"

        val startProperty =
            grouped["DTSTART"]
                ?.firstOrNull()

        val endProperty =
            grouped["DTEND"]
                ?.firstOrNull()

        val start =
            parseDateTime(
                startProperty
            )

        val end =
            parseDateTime(
                endProperty
            )

        val location =
            grouped["LOCATION"]
                ?.firstOrNull()
                ?.value

        val description =
            grouped["DESCRIPTION"]
                ?.firstOrNull()
                ?.value

        val organizer =
            grouped["ORGANIZER"]
                ?.firstOrNull()
                ?.value

        val courseCode =
            grouped["X-COURSE-CODE"]
                ?.firstOrNull()
                ?.value

        val metadata =
            buildMetadata(
                properties
            )

        return try {

            Result.success(
                ExternalScheduleEntry(
                    id =
                        ExternalScheduleId(
                            uid
                        ),

                    title =
                        unescapeText(
                            title
                        ),

                    source =
                        ExternalScheduleOrigin.ICS,

                    startAt =
                        start,

                    endAt =
                        end,

                    location =
                        location
                            ?.let(::unescapeText),

                    instructor =
                        organizer
                            ?.let(::unescapeText),

                    courseCode =
                        courseCode
                            ?.let(::unescapeText),

                    description =
                        description
                            ?.let(::unescapeText),

                    metadata =
                        metadata,
                )
            )

        } catch (exception: IllegalArgumentException) {

            Result.failure(
                exception
            )
        }
    }

    private fun parseDateTime(
        property: IcsProperty?,
    ): Instant? {

        if (property == null) {
            return null
        }

        val value =
            property.value

        /**
         * UTC datetime:
         *
         * 20260916T080000Z
         */
        if (
            value.length == 16 &&
            value.endsWith("Z")
        ) {

            val iso =
                buildString {
                    append(
                        value.substring(
                            0,
                            4
                        )
                    )
                    append('-')
                    append(
                        value.substring(
                            4,
                            6
                        )
                    )
                    append('-')
                    append(
                        value.substring(
                            6,
                            8
                        )
                    )
                    append('T')
                    append(
                        value.substring(
                            9,
                            11
                        )
                    )
                    append(':')
                    append(
                        value.substring(
                            11,
                            13
                        )
                    )
                    append(':')
                    append(
                        value.substring(
                            13,
                            15
                        )
                    )
                    append('Z')
                }

            return Instant.parse(
                iso
            )
        }

        /**
         * DATE-only：
         *
         * 20260916
         *
         * 11.4 暂不把 DATE-only
         * 强行转换为具体时刻。
         */
        if (
            value.length == 8 &&
            property["VALUE"] == "DATE"
        ) {

            if (!policy.allowDateOnly) {
                throw IllegalArgumentException(
                    "DATE-only DTSTART is not allowed."
                )
            }

            return null
        }

        throw IllegalArgumentException(
            "Unsupported ICS datetime format: $value"
        )
    }

    private fun buildMetadata(
        properties: List<IcsProperty>,
    ): ExternalScheduleMetadata {

        val values =
            buildMap {

                properties
                    .filter {
                        it.name.startsWith("X-")
                    }
                    .forEach { property ->

                        put(
                            property.name,
                            property.value,
                        )
                    }
            }

        return ExternalScheduleMetadata(
            values
        )
    }

    private fun unescapeText(
        value: String,
    ): String =
        value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
}
