package me.moiclathy.dism.external.schedule.sync.recovery

/**
 * Recovery Plan 生成结果。
 */
data class ExternalScheduleSyncRecoveryResult(

    val successful:
        Boolean,

    val plan:
        ExternalScheduleSyncRecoveryPlan? = null,

    val warnings:
        List<String> = emptyList(),

    val errors:
        List<String> = emptyList(),
) {

    companion object {

        fun success(
            plan:
                ExternalScheduleSyncRecoveryPlan,

            warnings:
                List<String> = emptyList(),
        ): ExternalScheduleSyncRecoveryResult =
            ExternalScheduleSyncRecoveryResult(
                successful = true,
                plan = plan,
                warnings = warnings,
            )

        fun failure(
            errors:
                List<String>,

            warnings:
                List<String> = emptyList(),
        ): ExternalScheduleSyncRecoveryResult =
            ExternalScheduleSyncRecoveryResult(
                successful = false,
                warnings = warnings,
                errors = errors,
            )
    }
}
