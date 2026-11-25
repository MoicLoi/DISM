package me.moiclathy.dism.schedule.snapshot

data class DynamicSchedulePolicySnapshot(
    val minimumAttentionPriority: Double,

    val minimumAttentionConfidence: Double,

    val minimumHardConstraintPriority: Double,

    val explicitRequestPriority: Double,

    val maximumLearnedSuppression: Double,
) {
    init {
        require(minimumAttentionPriority in 0.0..1.0)
        require(minimumAttentionConfidence in 0.0..1.0)
        require(minimumHardConstraintPriority in 0.0..1.0)
        require(explicitRequestPriority in 0.0..1.0)
        require(maximumLearnedSuppression in 0.0..1.0)
    }
}
