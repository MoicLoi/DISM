package me.moiclathy.dism.schedule.engine

/**
 * 13.11.1
 *
 * 当前 Dynamic Schedule Engine 的描述。
 *
 * 版本号不再散落在 Assembly / Runtime / Replay / Verification /
 * Persistence 里，而是只有一个来源：
 *
 * ```kotlin
 * engineRegistry.current.version
 * ```
 *
 * 以后升级只需要改
 * [DefaultDynamicScheduleEngineRegistry]，
 * 而不是全项目 grep `"13.11"`。
 */
data class DynamicScheduleEngineDescriptor(
    val version: String,

    val displayName: String,

    val description: String,
) {

    init {
        require(version.isNotBlank()) {
            "Engine version must not be blank"
        }

        require(displayName.isNotBlank()) {
            "Engine display name must not be blank"
        }

        require(description.isNotBlank()) {
            "Engine description must not be blank"
        }
    }

    override fun toString(): String =
        "$displayName $version"
}
