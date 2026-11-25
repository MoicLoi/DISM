package me.moiclathy.dism.attention.recovery

import me.moiclathy.dism.attention.persistence.AttentionEventLog

/** Application-facing facade for reconstructing the full Attention projection. */
class AttentionRuntimeStateReconstructor(
    private val reducer: AttentionStateReducer = AttentionStateReducer(),
) {
    fun recover(attentionKey: String, log: AttentionEventLog): AttentionReplayState? =
        reducer.reduce(log.eventsFor(attentionKey))

    fun recoverAll(log: AttentionEventLog): Map<String, AttentionReplayState> =
        log.all()
            .distinctBy { it.id }
            .groupBy { it.attentionKey }
            .mapNotNull { (key, events) -> reducer.reduce(events)?.let { key to it } }
            .toMap()
}
