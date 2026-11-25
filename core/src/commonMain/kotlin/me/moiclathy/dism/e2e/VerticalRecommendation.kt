package me.moiclathy.dism.e2e

enum class VerticalRecommendationStatus { ACTIVE, RESOLVED, EXPIRED }

data class VerticalRecommendation(
    val key: String,
    val riskKey: String,
    val eventKey: String,
    val message: String,
    val priority: Double,
    val status: VerticalRecommendationStatus = VerticalRecommendationStatus.ACTIVE,
) {
    init { require(priority in 0.0..1.0) }
}
