package me.moiclathy.dism.schedule.version

/**
 * 13.10
 *
 * 历史引擎版本。
 *
 * 之所以包一层而不是到处传 `String`：
 * 一旦版本值在历史模块里裸奔，
 * 就很容易出现"这里比较的是格式版本、那里比较的是语义版本"
 * 这类只有出事故时才会被发现的问题。
 */
data class DynamicScheduleHistoryEngineVersion(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "Engine version must not be blank"
        }
    }

    override fun toString(): String =
        value
}
