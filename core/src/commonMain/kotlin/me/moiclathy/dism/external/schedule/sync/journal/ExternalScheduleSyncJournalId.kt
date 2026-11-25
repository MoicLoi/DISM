package me.moiclathy.dism.external.schedule.sync.journal

/**
 * 一条 Journal Event 的唯一 ID。
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class ExternalScheduleSyncJournalId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "journalId must not be blank"
        }
    }

    override fun toString(): String =
        value
}
