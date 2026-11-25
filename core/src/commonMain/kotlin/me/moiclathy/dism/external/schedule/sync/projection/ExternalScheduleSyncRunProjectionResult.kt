package me.moiclathy.dism.external.schedule.sync.projection

/** Run Projection 执行结果。 */
data class ExternalScheduleSyncRunProjectionResult(
    val successful: Boolean,
    val projection: ExternalScheduleSyncRunProjection? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        if (successful) require(projection != null) { "successful projection requires projection" }
        if (!successful) require(errors.isNotEmpty()) { "failed projection requires errors" }
    }

    companion object {
        fun success(projection: ExternalScheduleSyncRunProjection, warnings: List<String> = emptyList()) =
            ExternalScheduleSyncRunProjectionResult(true, projection, warnings, emptyList())

        fun failure(vararg errors: String) =
            ExternalScheduleSyncRunProjectionResult(false, null, emptyList(), errors.toList())
    }
}
