package me.moiclathy.dism.context.source

/**
 * 一个具体 Context Source 的稳定标识。
 *
 * 例如：
 * "schedule"
 * "user_input"
 * "local_observation"
 * "derived"
 */
data class ContextSourceId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "ContextSourceId must not be blank."
        }
    }

    override fun toString(): String = value
}
