package me.moiclathy.dism.external.schedule.parser

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * External Schedule Parser 注册表。
 */
class ExternalScheduleParserRegistry {

    private val parsers =
        linkedMapOf<
            ExternalScheduleOrigin,
            ExternalScheduleParser,
        >()

    /**
     * 注册 Parser。
     */
    fun register(
        parser: ExternalScheduleParser,
    ) {

        require(
            !parsers.containsKey(parser.origin)
        ) {
            "External schedule parser already registered: ${parser.origin}"
        }

        parsers[parser.origin] =
            parser
    }

    /**
     * 注册多个 Parser。
     */
    fun registerAll(
        parsers: Iterable<ExternalScheduleParser>,
    ) {
        parsers.forEach(::register)
    }

    /**
     * 查找 Parser。
     */
    fun find(
        origin: ExternalScheduleOrigin,
    ): ExternalScheduleParser? =
        parsers[origin]

    /**
     * 是否存在。
     */
    fun contains(
        origin: ExternalScheduleOrigin,
    ): Boolean =
        origin in parsers

    /**
     * 当前所有 Parser。
     */
    fun all():
        List<ExternalScheduleParser> =
        parsers.values.toList()

    /**
     * 移除 Parser。
     */
    fun remove(
        origin: ExternalScheduleOrigin,
    ): Boolean =
        parsers.remove(origin) != null

    /**
     * Parser 数量。
     */
    val size: Int
        get() =
            parsers.size
}
