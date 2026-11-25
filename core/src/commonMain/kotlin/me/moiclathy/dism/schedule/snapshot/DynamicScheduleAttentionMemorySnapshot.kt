package me.moiclathy.dism.schedule.snapshot

data class DynamicScheduleAttentionMemorySnapshot(
    val globalFactor: Double = 1.0,

    val contextFactor: Double = 1.0,

    val adaptationFactor: Double = 1.0,
) {
    init {
        require(globalFactor >= 0.0)
        require(contextFactor >= 0.0)
        require(adaptationFactor >= 0.0)
    }

    val effectiveFactor: Double
        get() =
            globalFactor *
                contextFactor *
                adaptationFactor
}
