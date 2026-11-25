package me.moiclathy.dism.external.schedule.sync.reconciliation

/**
 * 一次 Reconciliation 结果。
 */
data class ExternalScheduleSyncReconciliationResult(

    val successful:
        Boolean,

    val records:
        List<ExternalScheduleSyncReconciliationRecord> =
            emptyList(),

    val warnings:
        List<String> =
            emptyList(),

    val errors:
        List<String> =
            emptyList(),
) {

    val actionRequiredCount: Int
        get() =
            records.count {
                it.requiresAction
            }

    val consistentCount: Int
        get() =
            records.count {
                it.status ==
                    ExternalScheduleSyncReconciliationStatus
                        .CONSISTENT
            }

    val missingEventCount: Int
        get() =
            records.count {
                it.status ==
                    ExternalScheduleSyncReconciliationStatus
                        .MISSING_EVENT
            }

    val incompleteCount: Int
        get() =
            records.count {
                it.status ==
                    ExternalScheduleSyncReconciliationStatus
                        .EVENT_EXISTS_BUT_SYNC_INCOMPLETE ||
                it.status ==
                    ExternalScheduleSyncReconciliationStatus
                        .SYNC_INCOMPLETE_EVENT_MISSING
            }

    companion object {

        fun success(
            records:
                List<ExternalScheduleSyncReconciliationRecord>,

            warnings:
                List<String> =
                emptyList(),
        ):
            ExternalScheduleSyncReconciliationResult =
            ExternalScheduleSyncReconciliationResult(

                successful =
                    true,

                records =
                    records,

                warnings =
                    warnings,
            )

        fun failure(
            errors:
                List<String>,

            warnings:
                List<String> =
                emptyList(),
        ):
            ExternalScheduleSyncReconciliationResult =
            ExternalScheduleSyncReconciliationResult(

                successful =
                    false,

                warnings =
                    warnings,

                errors =
                    errors,
            )
    }
}
