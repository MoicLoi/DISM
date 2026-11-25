package me.moiclathy.dism.schedule.model

data class DynamicScheduleIntelligencePolicy(
    val minimumPriority: Double = 0.0,
    val maximumPriority: Double = 1.0,
    val minimumConfidence: Double = 0.0,
    val maximumConfidence: Double = 1.0,
    val defaultPriority: Double = 0.5,
    val defaultConfidence: Double = 0.5,
) {
    init {
        require(minimumPriority in 0.0..1.0)
        require(maximumPriority in 0.0..1.0)
        require(minimumPriority <= maximumPriority)
        require(minimumConfidence in 0.0..1.0)
        require(maximumConfidence in 0.0..1.0)
        require(minimumConfidence <= maximumConfidence)
        require(defaultPriority in minimumPriority..maximumPriority)
        require(defaultConfidence in minimumConfidence..maximumConfidence)
    }

    fun clampPriority(value: Double): Double = value.coerceIn(minimumPriority, maximumPriority)

    fun clampConfidence(value: Double): Double = value.coerceIn(minimumConfidence, maximumConfidence)
}
