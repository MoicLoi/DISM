package me.moiclathy.dism.semantic.propagation

data class RequirementRiskLifecycleChange(
    val key: RequirementRiskKey,
    val lifecycle: RequirementRiskLifecycle,
    val previous: RequirementRisk? = null,
    val current: RequirementRisk? = null,
)
