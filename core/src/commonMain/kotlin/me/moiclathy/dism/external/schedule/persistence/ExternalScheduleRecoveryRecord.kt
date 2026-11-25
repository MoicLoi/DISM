package me.moiclathy.dism.external.schedule.persistence

/**
 * Recovery 后暴露给 Runtime 的记录。
 *
 * 它不直接修改 PersistenceRecord。
 */
data class ExternalScheduleRecoveryRecord(
    val record: ExternalSchedulePersistenceRecord,
    val status: ExternalScheduleRecoveryStatus,
    val recoverable: Boolean,
    val reason: String? = null,
) {

    init {
        if (reason != null) {
            require(reason.isNotBlank()) {
                "reason must not be blank"
            }
        }

        if (status == ExternalScheduleRecoveryStatus.INVALID) {
            require(!recoverable) {
                "INVALID record cannot be recoverable"
            }
        }
    }
}
