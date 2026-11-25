package me.moiclathy.dism.external.schedule

/**
 * 外部日程附加元数据。
 *
 * 采用 String → String，
 * 避免 11.1 就把外部格式绑定到 JSON / XML / 某个 API。
 */
data class ExternalScheduleMetadata(
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

    fun isEmpty(): Boolean =
        values.isEmpty()

    fun isNotEmpty(): Boolean =
        values.isNotEmpty()

    fun with(
        key: String,
        value: String,
    ): ExternalScheduleMetadata {

        require(key.isNotBlank()) {
            "Metadata key must not be blank."
        }

        return copy(
            values =
                values +
                    (key to value),
        )
    }

    companion object {

        val EMPTY =
            ExternalScheduleMetadata()
    }
}
