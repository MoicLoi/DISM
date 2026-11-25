package me.moiclathy.dism.external.schedule.parser

import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * 静态 Parser。
 *
 * 用于：
 *
 *     单元测试
 *     E2E
 *     Runtime Fixture
 *
 * 不真正解析 rawContent。
 */
class StaticExternalScheduleParser(
    private val schedule: ExternalSchedule,
) : ExternalScheduleParser {

    override val origin: ExternalScheduleOrigin
        get() =
            schedule.source

    override fun parse(
        input: ExternalScheduleParseInput,
    ): ExternalScheduleParseResult {

        if (input.origin != origin) {

            return ExternalScheduleParseResult.failure(
                ExternalScheduleParseError(
                    code =
                        ExternalScheduleParseErrorCode.UNSUPPORTED_FORMAT,

                    message =
                        "Parser origin does not match input origin.",
                )
            )
        }

        return ExternalScheduleParseResult.success(
            schedule =
                schedule,
        )
    }
}
