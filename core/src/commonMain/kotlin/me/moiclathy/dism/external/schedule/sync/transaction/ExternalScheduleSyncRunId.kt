package me.moiclathy.dism.external.schedule.sync.transaction

/**
 * 一次完整 External Schedule Sync 的稳定 Run ID。
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class ExternalScheduleSyncRunId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "syncRunId must not be blank"
        }
    }

    override fun toString(): String =
        value
}
