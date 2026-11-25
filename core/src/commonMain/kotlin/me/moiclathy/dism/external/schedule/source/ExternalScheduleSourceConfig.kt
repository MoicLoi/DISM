package me.moiclathy.dism.external.schedule.source

/**
 * 外部日程来源配置。
 *
 * 11.2 不关心配置具体含义。
 * 具体 Adapter 可以从 metadata 中读取自己的配置。
 *
 * 例如：
 *
 * ICS:
 *     url
 *
 * Xiqueer:
 *     endpoint
 *     account
 *     campus
 *
 * File:
 *     path
 */
data class ExternalScheduleSourceConfig(
    val values: Map<String, String> = emptyMap(),
) {

    operator fun get(
        key: String,
    ): String? =
        values[key]

    operator fun contains(
        key: String,
    ): Boolean =
        key in values

    val isEmpty: Boolean
        get() = values.isEmpty()

    val isNotEmpty: Boolean
        get() = values.isNotEmpty()

    fun with(
        key: String,
        value: String,
    ): ExternalScheduleSourceConfig {

        require(key.isNotBlank()) {
            "Configuration key must not be blank."
        }

        return copy(
            values =
                values +
                    (key to value),
        )
    }

    companion object {

        val EMPTY =
            ExternalScheduleSourceConfig()
    }
}
