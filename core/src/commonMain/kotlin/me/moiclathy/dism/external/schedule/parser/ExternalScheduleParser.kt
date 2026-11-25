package me.moiclathy.dism.external.schedule.parser

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * 外部日程 Parser。
 *
 * Parser 的职责只有：
 *
 *     Raw Data → ExternalSchedule
 *
 * Parser 不负责：
 *
 *     Source
 *     网络访问
 *     Event 编译
 *     Entity Binding
 *     Persistence
 *     Attention
 */
interface ExternalScheduleParser {

    /**
     * Parser 支持的来源类型。
     */
    val origin: ExternalScheduleOrigin

    /**
     * 解析原始数据。
     */
    fun parse(
        input: ExternalScheduleParseInput,
    ): ExternalScheduleParseResult
}
