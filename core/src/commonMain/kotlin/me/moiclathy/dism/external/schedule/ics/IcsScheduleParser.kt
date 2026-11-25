package me.moiclathy.dism.external.schedule.ics

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseError
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseErrorCode
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseInput
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseResult
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParser

/**
 * ICS → ExternalSchedule Parser。
 */
class IcsScheduleParser(
    private val parser:
        IcsParser =
        IcsParser(),

    private val eventBuilder:
        IcsEventBuilder =
        IcsEventBuilder(),
) : ExternalScheduleParser {

    override val origin:
        ExternalScheduleOrigin
        get() =
            ExternalScheduleOrigin.ICS

    override fun parse(
        input: ExternalScheduleParseInput,
    ): ExternalScheduleParseResult {

        if (
            input.origin !=
            ExternalScheduleOrigin.ICS
        ) {

            return ExternalScheduleParseResult.failure(
                ExternalScheduleParseError(
                    code =
                        ExternalScheduleParseErrorCode.UNSUPPORTED_FORMAT,

                    message =
                        "IcsScheduleParser requires ICS input.",
                )
            )
        }

        val parsed =
            try {
                parser
                    .parse(
                        input.rawContent
                    )
                    .getOrThrow()
            } catch (
                exception: IllegalArgumentException
            ) {

                return ExternalScheduleParseResult.failure(
                    ExternalScheduleParseError(
                        code =
                            ExternalScheduleParseErrorCode.MALFORMED_DATA,

                        message =
                            exception.message
                                ?: "Malformed ICS document.",
                    )
                )
            }

        val entries =
            mutableListOf<
                me.moiclathy.dism.external.schedule.ExternalScheduleEntry
            >()

        val errors =
            mutableListOf<
                ExternalScheduleParseError
            >()

        parsed.events.forEachIndexed { index, properties ->

            val fallbackId =
                buildFallbackId(
                    input,
                    index,
                )

            val result =
                eventBuilder.build(
                    properties =
                        properties,

                    fallbackId =
                        fallbackId,
                )

            result
                .onSuccess {
                    entries += it
                }
                .onFailure { exception ->

                    errors +=
                        ExternalScheduleParseError(
                            code =
                                ExternalScheduleParseErrorCode.INVALID_ENTRY,

                            message =
                                exception.message
                                    ?: "Invalid VEVENT.",

                            field =
                                "VEVENT[$index]",
                        )
                }
        }

        if (errors.isNotEmpty()) {

            return ExternalScheduleParseResult.failure(
                errors =
                    errors,
            )
        }

        val scheduleId =
            buildScheduleId(
                input,
                parsed,
            )

        val scheduleName =
            parsed.calendarName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "ICS Schedule"

        val importedAt =
            input["importedAt"]
                ?.let {
                    runCatching {
                        Instant.parse(it)
                    }.getOrNull()
                }
                ?: Instant.DISTANT_PAST

        val schedule =
            try {

                ExternalSchedule(
                    id =
                        ExternalScheduleId(
                            scheduleId
                        ),

                    source =
                        ExternalScheduleOrigin.ICS,

                    name =
                        scheduleName,

                    importedAt =
                        importedAt,

                    entries =
                        entries,
                )

            } catch (
                exception: IllegalArgumentException
            ) {

                return ExternalScheduleParseResult.failure(
                    ExternalScheduleParseError(
                        code =
                            ExternalScheduleParseErrorCode.INVALID_ENTRY,

                        message =
                            exception.message
                                ?: "Invalid ICS schedule.",
                    )
                )
            }

        return ExternalScheduleParseResult.success(
            schedule =
                schedule,

            warnings =
                parsed.warnings,
        )
    }

    private fun buildFallbackId(
        input: ExternalScheduleParseInput,
        index: Int,
    ): String =
        "${input.sourceId.value}:event:$index"

    private fun buildScheduleId(
        input: ExternalScheduleParseInput,
        parsed: IcsParseResult,
    ): String {

        val calendarName =
            parsed.calendarName
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (calendarName != null) {
            "${input.sourceId.value}:$calendarName"
        } else {
            "${input.sourceId.value}:calendar"
        }
    }
}
