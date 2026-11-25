package me.moiclathy.dism.nl.temporal

data class TemporalMention(
    val surface: String,
    val dayOffset: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val meridiem: Meridiem? = null,
    val durationMinutes: Int? = null,
    val granularity: TemporalGranularity = TemporalGranularity.UNSPECIFIED,
    val confidence: Double = 1.0,
) {
    init {
        require(surface.isNotBlank())
        require(confidence in 0.0..1.0)
        require(hour == null || hour in 0..23)
        require(minute == null || minute in 0..59)
        require(durationMinutes == null || durationMinutes > 0)
    }
}

enum class Meridiem { MORNING, AFTERNOON, EVENING, NIGHT }
