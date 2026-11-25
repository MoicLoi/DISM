package me.moiclathy.dism.external.schedule.sync

/**
 * 单条 Sync Action 的实际执行结果。
 */
data class ExternalScheduleSyncExecutionRecord(
    val externalKey: String,
    val action: ExternalScheduleSyncAction,
    val status: ExternalScheduleSyncExecutionStatus,
    val eventId: String? = null,
    val message: String? = null,
) {

    init {
        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        if (
            status == ExternalScheduleSyncExecutionStatus.FAILED
        ) {
            require(
                !message.isNullOrBlank()
            ) {
                "failed execution must contain a message"
            }
        }
    }

    val successful: Boolean
        get() =
            status !=
                ExternalScheduleSyncExecutionStatus.FAILED
}
