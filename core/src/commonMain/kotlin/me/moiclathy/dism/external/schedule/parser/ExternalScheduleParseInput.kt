package me.moiclathy.dism.external.schedule.parser

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId

/**
 * 外部日程 Parser 的输入。
 *
 * Parser 不关心数据是从网络、文件还是 CLI 获得的。
 * 它只接收已经获取到的原始数据。
 */
data class ExternalScheduleParseInput(
    val sourceId: ExternalScheduleSourceId,
    val origin: ExternalScheduleOrigin,
    val rawContent: String,
    val metadata: Map<String, String> = emptyMap(),
) {

    init {
        require(rawContent.isNotBlank()) {
            "Parser rawContent must not be blank."
        }

        require(metadata.keys.all { it.isNotBlank() }) {
            "Parser metadata keys must not be blank."
        }
    }

    operator fun get(
        key: String,
    ): String? =
        metadata[key]

    operator fun contains(
        key: String,
    ): Boolean =
        key in metadata
}
