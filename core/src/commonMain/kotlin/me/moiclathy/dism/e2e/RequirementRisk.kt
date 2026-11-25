package me.moiclathy.dism.e2e

enum class RequirementRiskStatus { ACTIVE, RESOLVED, EXPIRED }

data class RequirementRisk(
    val key: String,
    val eventKey: String,
    val requirement: String,
    val status: RequirementRiskStatus = RequirementRiskStatus.ACTIVE,
    val confidence: Double = 1.0,
)
