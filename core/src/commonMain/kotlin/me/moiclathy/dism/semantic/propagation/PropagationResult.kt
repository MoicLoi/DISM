package me.moiclathy.dism.semantic.propagation

/**
 * 一次传播的语义后果。
 */
data class PropagationResult(
    val affectedEventIds: Set<kotlin.uuid.Uuid> = emptySet(),
    val requirementRisks: List<RequirementRisk> = emptyList(),
    val requirementRiskLifecycleChanges: List<RequirementRiskLifecycleChange> = emptyList(),
)
