package me.moiclathy.dism.external.schedule.sync.transaction

/**
 * Run-level Recovery 结果。
 */
data class ExternalScheduleSyncRunRecoveryResult(

    val successful:
        Boolean,

    val plan:
        ExternalScheduleSyncRunRecoveryPlan? = null,

    val warnings:
        List<String> = emptyList(),

    val errors:
        List<String> = emptyList(),
) {

    companion object {

        fun success(
            plan:
                ExternalScheduleSyncRunRecoveryPlan,

            warnings:
                List<String> = emptyList(),
        ):
            ExternalScheduleSyncRunRecoveryResult =
            ExternalScheduleSyncRunRecoveryResult(

                successful = true,

                plan = plan,

                warnings = warnings,
            )

        fun failure(
            errors:
                List<String>,

            warnings:
                List<String> = emptyList(),
        ):
            ExternalScheduleSyncRunRecoveryResult =
            ExternalScheduleSyncRunRecoveryResult(

                successful = false,

                warnings = warnings,

                errors = errors,
            )
    }
}
