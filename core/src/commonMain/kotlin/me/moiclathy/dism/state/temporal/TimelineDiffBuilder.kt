package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.state.model.RelationState

/**
 * 基于 (key, validFrom) 身份的两份状态集合差异。
 *
 * 职责是描述"数据结构具体改了什么"；
 * "是什么语义操作导致的"由 StateTransitionRecord 回答。
 */
class TimelineDiffBuilder {

    fun build(
        before: List<RelationState>,
        after: List<RelationState>,
    ): TimelineChangeSet {

        val changes =
            mutableListOf<StateChange>()

        val beforeByIdentity =
            before.associateBy {
                identityOf(it)
            }

        val afterByIdentity =
            after.associateBy {
                identityOf(it)
            }

        val identities =
            beforeByIdentity.keys +
                afterByIdentity.keys

        identities.forEach { identity ->

            val previous =
                beforeByIdentity[identity]

            val current =
                afterByIdentity[identity]

            when {
                previous == null &&
                    current != null -> {
                    changes +=
                        StateChange.Inserted(
                            current
                        )
                }

                previous != null &&
                    current == null -> {
                    changes +=
                        StateChange.Removed(
                            previous
                        )
                }

                previous != null &&
                    current != null &&
                    previous != current -> {
                    changes +=
                        StateChange.Updated(
                            before = previous,
                            after = current,
                        )
                }
            }
        }

        return TimelineChangeSet(
            changes = changes,
        )
    }

    private fun identityOf(
        state: RelationState,
    ): StateIdentity {
        return StateIdentity(
            key = state.key,
            validFrom = state.validFrom,
        )
    }
}
