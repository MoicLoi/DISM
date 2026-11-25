package me.moiclathy.dism.event.model

/**
 * Event 的非核心扩展信息。
 *
 * 用 Map 保持 10.1 对上层领域的低耦合。
 */
data class EventMetadata(
    val values: Map<String, String> = emptyMap(),
) {

    operator fun get(
        key: String,
    ): String? =
        values[key]

    fun contains(
        key: String,
    ): Boolean =
        key in values
}
