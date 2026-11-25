package me.moiclathy.dism.context.runtime

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Runtime 状态恢复器。
 *
 * 通过 Event Log 重建当前 Projection。
 */
class ContextRuntimeRecovery {

    fun recover(
        events: Iterable<ContextRuntimeEvent>,
    ): ContextRuntimeState {

        /*
         * Event ID 去重。
         *
         * 保证重复持久化不会导致重复计算。
         */
        val ordered =
            events
                .distinctBy {
                    it.id
                }
                .sortedWith(
                    compareBy<ContextRuntimeEvent> {
                        it.occurredAt
                    }.thenBy {
                        it.id
                    }
                )

        val records =
            linkedMapOf<
                ContextMode,
                ContextRuntimeRecord
            >()

        var activeMode: ContextMode? =
            null

        var activeConfidence:
            Double? = null

        val latestEventAt =
            ordered.lastOrNull()?.occurredAt

        for (event in ordered) {

            val eventType =
                event.type

            if (
                eventType ==
                ContextRuntimeEventType.OBSERVED
            ) {

                val mode =
                    event.mode
                        ?: continue

                val previous =
                    records[mode]

                records[mode] =
                    updateObservation(
                        previous = previous,
                        event = event,
                    )
            }

            if (
                eventType ==
                ContextRuntimeEventType.RESOLVED
            ) {

                val mode =
                    event.mode
                        ?: continue

                activeMode =
                    mode

                activeConfidence =
                    event.confidence
                        ?: activeConfidence

                val previous =
                    records[mode]

                records[mode] =
                    updateObservation(
                        previous = previous,
                        event = event,
                    )
            }

            if (
                eventType ==
                ContextRuntimeEventType.TRANSITION
            ) {

                val mode =
                    event.mode

                val previousMode =
                    event.previousMode

                if (previousMode != null) {

                    val previous =
                        records[previousMode]

                    if (previous != null) {
                        records[previousMode] =
                            previous.copy(
                                exitedAt =
                                    event.occurredAt,
                                transitionCount =
                                    previous.transitionCount + 1,
                            )
                    }
                }

                if (mode != null) {

                    val previous =
                        records[mode]

                    records[mode] =
                        (
                            previous
                                ?: ContextRuntimeRecord(
                                    mode = mode,
                                )
                            ).copy(
                                enteredAt =
                                    event.occurredAt,
                                lastObservedAt =
                                    event.occurredAt,
                                transitionCount =
                                    (
                                        previous
                                            ?.transitionCount
                                            ?: 0
                                        ) + 1,
                                lastConfidence =
                                    event.confidence
                                        ?: previous
                                            ?.lastConfidence
                                        ?: 0.0,
                            )

                    activeMode =
                        mode

                    activeConfidence =
                        event.confidence
                            ?: activeConfidence
                }
            }

            if (
                eventType ==
                ContextRuntimeEventType.CONFIDENCE_CHANGED
            ) {

                val mode =
                    event.mode

                if (
                    mode != null &&
                    mode == activeMode
                ) {
                    activeConfidence =
                        event.confidence
                }

                if (mode != null) {

                    val previous =
                        records[mode]

                    if (previous != null) {
                        records[mode] =
                            previous.copy(
                                lastConfidence =
                                    event.confidence
                                        ?: previous
                                            .lastConfidence,
                            )
                    }
                }
            }

            if (
                eventType ==
                ContextRuntimeEventType.EXPIRED
            ) {

                val mode =
                    event.mode

                if (
                    mode != null &&
                    mode == activeMode
                ) {
                    activeMode = null
                    activeConfidence = null
                }

                if (mode != null) {

                    val previous =
                        records[mode]

                    if (previous != null) {
                        records[mode] =
                            previous.copy(
                                exitedAt =
                                    event.occurredAt,
                            )
                    }
                }
            }
        }

        val confidence =
            if (
                activeMode != null &&
                activeConfidence != null
            ) {
                me.moiclathy.dism.context.confidence
                    .ContextConfidence(
                        mode = activeMode,
                        value =
                            activeConfidence
                                .coerceIn(
                                    0.0,
                                    1.0,
                                ),
                        lastUpdatedAt =
                            latestEventAt,
                    )
            } else {
                null
            }

        return ContextRuntimeState(
            activeMode = activeMode,
            confidence = confidence,
            records = records,
            restoredAt = latestEventAt,
            eventCount = ordered.size,
        )
    }

    private fun updateObservation(
        previous: ContextRuntimeRecord?,
        event: ContextRuntimeEvent,
    ): ContextRuntimeRecord {

        val mode =
            event.mode
                ?: return previous
                    ?: error(
                        "Observation event requires a mode."
                    )

        return (
            previous
                ?: ContextRuntimeRecord(
                    mode = mode,
                    firstObservedAt =
                        event.occurredAt,
                )
            ).copy(
                lastObservedAt =
                    event.occurredAt,
                observationCount =
                    (
                        previous
                            ?.observationCount
                            ?: 0
                        ) + 1,
                lastConfidence =
                    event.confidence
                        ?: previous
                            ?.lastConfidence
                        ?: 0.0,
            )
    }
}
