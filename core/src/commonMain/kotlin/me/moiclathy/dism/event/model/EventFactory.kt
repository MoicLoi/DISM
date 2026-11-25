package me.moiclathy.dism.event.model

/**
 * Event 创建工具。
 *
 * 不负责：
 *
 * - Entity Resolution
 * - Temporal Resolution
 * - State Transition
 * - Recommendation
 * - Attention
 */
object EventFactory {

    fun create(
        id: EventId,
        title: String,
        type: EventType =
            EventType.ONE_TIME,
        temporal: EventTemporal =
            EventTemporal.Unscheduled,
        source: EventSource =
            EventSource.USER_INPUT,
        externalId: String? = null,
        metadata: EventMetadata =
            EventMetadata(),
    ): Event =
        Event(
            id = id,
            title = title,
            type = type,
            temporal = temporal,
            source = source,
            externalId = externalId,
            metadata = metadata,
        )
}
