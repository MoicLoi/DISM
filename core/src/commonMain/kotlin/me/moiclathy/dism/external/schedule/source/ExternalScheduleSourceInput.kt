package me.moiclathy.dism.external.schedule.source

import kotlin.time.Instant

/**
 * 请求一个外部日程来源获取数据时使用的输入。
 */
data class ExternalScheduleSourceInput(
    val requestedAt: Instant,
    val config: ExternalScheduleSourceConfig =
        ExternalScheduleSourceConfig.EMPTY,
    val metadata: Map<String, String> = emptyMap(),
) {

    init {

        require(metadata.keys.all { it.isNotBlank() }) {
            "Input metadata keys must not be blank."
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
