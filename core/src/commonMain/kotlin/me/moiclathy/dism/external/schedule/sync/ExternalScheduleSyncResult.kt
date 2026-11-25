package me.moiclathy.dism.external.schedule.sync

data class ExternalScheduleSyncResult(
    val plan: ExternalScheduleSyncPlan?,

    val executedChanges:
        List<ExternalScheduleSyncChange> = emptyList(),

    val warnings: List<String> = emptyList(),

    val errors: List<String> = emptyList(),
) {

    val successful: Boolean
        get() =
            plan != null &&
                errors.isEmpty()

    val executedCount: Int
        get() =
            executedChanges.size

    companion object {

        fun success(
            plan: ExternalScheduleSyncPlan,
            executedChanges:
                List<ExternalScheduleSyncChange> =
                emptyList(),
            warnings:
                List<String> =
                emptyList(),
        ): ExternalScheduleSyncResult =
            ExternalScheduleSyncResult(
                plan = plan,
                executedChanges =
                    executedChanges,
                warnings =
                    warnings,
            )

        fun failure(
            errors: List<String>,
            plan: ExternalScheduleSyncPlan? = null,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleSyncResult =
            ExternalScheduleSyncResult(
                plan = plan,
                warnings = warnings,
                errors = errors,
            )
    }
}
