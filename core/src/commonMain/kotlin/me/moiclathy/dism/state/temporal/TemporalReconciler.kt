package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.persistence.repository.EvidenceRepository
import me.moiclathy.dism.semantic.confidence.RelationConfidenceAggregator
import me.moiclathy.dism.state.constraint.RelationConstraintPolicy
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport

/**
 * 对同一语义槽的局部时间线做重建。
 *
 * 新的 retroactive assertion 不应该直接截断"当前 active state"，
 * 而应该重建该 subject + slot 的历史区间：
 *
 * 已有：
 *   22:00 Classroom
 *   23:30 Backpack
 * 补充："其实 22:47 就在宿舍了"
 * ↓
 *   22:00–22:47 Classroom
 *   22:47–23:30 Dorm
 *   23:30–∞ Backpack
 *
 * 第一版边界：
 * 只处理 同 subject + 同 exclusive RelationSlot。
 * 不处理：跨实体传播、事件时间修改、依赖链重算、路线重新规划。
 */
class TemporalReconciler(
    private val constraintPolicy: RelationConstraintPolicy =
        RelationConstraintPolicy(),

    private val confidenceAggregator: RelationConfidenceAggregator =
        RelationConfidenceAggregator(),

    private val diffBuilder: TimelineDiffBuilder =
        TimelineDiffBuilder(),
) {

    fun reconcileAssertion(
        states: List<RelationState>,
        incoming: RelationState,
        evidenceRegistry: EvidenceRepository,
    ): TemporalReconciliationResult {

        val slot =
            constraintPolicy.slotOf(
                incoming.key.predicate
            )
                ?: return TemporalReconciliationResult(
                    previousStates = states,
                    currentStates = states + incoming,
                    affectedStates = listOf(incoming),
                    changeSet = diffBuilder.build(
                        before = states,
                        after = states + incoming,
                    ),
                )

        val relatedTimeline =
            states
                .filter { state ->
                    state.key.subjectId ==
                        incoming.key.subjectId &&
                        constraintPolicy.slotOf(
                            state.key.predicate
                        ) == slot
                }
                .sortedBy {
                    it.validFrom
                }

        val unrelatedStates =
            states.filterNot {
                it in relatedTimeline
            }

        val rebuilt =
            rebuildTimeline(
                timeline = relatedTimeline,
                incoming = incoming,
                evidenceRegistry = evidenceRegistry,
            )

        val currentStates =
            unrelatedStates +
                rebuilt

        return TemporalReconciliationResult(
            previousStates = states,
            currentStates = currentStates,
            affectedStates =
                relatedTimeline +
                    incoming,
            changeSet = diffBuilder.build(
                before = states,
                after = currentStates,
            ),
        )
    }

    private fun rebuildTimeline(
        timeline: List<RelationState>,
        incoming: RelationState,
        evidenceRegistry: EvidenceRepository,
    ): List<RelationState> {

        val before =
            timeline.filter { state ->
                state.validFrom <
                    incoming.validFrom
            }

        // 同一 effectiveAt 的状态单独处理。
        // 规则：后进入的 incoming 在同一时间点获得最终解释权。
        // 同 key → 合并 evidence；不同 key → 旧状态在该时间点结束。
        // 这样不会产生“validFrom == validUntil”的伪 ACTIVE 状态。
        val atSameTime =
            timeline.filter { state ->
                state.validFrom ==
                    incoming.validFrom
            }

        val after =
            timeline.filter { state ->
                state.validFrom >
                    incoming.validFrom
            }

        // 同 key 且是时间线最后一段（其后没有其他状态）：
        // incoming 并入既有状态，不新建节点。
        // 例如"充电宝在宿舍"被第二次确认——关系继续，置信度更新。
        val beforeLast = before.lastOrNull()

        if (
            beforeLast != null &&
            beforeLast.key == incoming.key &&
            atSameTime.isEmpty()
        ) {
            return mergeIntoBeforeLast(
                before = before,
                beforeLast = beforeLast,
                after = after,
                incoming = incoming,
                evidenceRegistry = evidenceRegistry,
            )
        }

        val rebuiltBefore =
            before.map { state ->

                val overlapsIncoming =
                    state.validUntil == null ||
                        state.validUntil >
                        incoming.validFrom

                if (
                    overlapsIncoming &&
                    state.key != incoming.key
                ) {
                    state.copy(
                        validUntil =
                            incoming.validFrom,
                        status =
                            RelationStatus.SUPERSEDED,
                    )
                } else {
                    state
                }
            }

        // 同时刻旧状态：incoming 获得最终解释权。
        // 不同 key 的旧状态保留为零长度历史段，便于审计；
        // 同 key 的状态则并入 incoming，避免重复节点。
        val sameRelationAtTime =
            atSameTime.firstOrNull {
                it.key == incoming.key
            }

        val conflictingAtSameTime =
            atSameTime
                .filter { it.key != incoming.key }
                .map { state ->
                    state.copy(
                        validUntil = incoming.validFrom,
                        status = RelationStatus.SUPERSEDED,
                    )
                }

        val sameRelationAfter =
            after.firstOrNull {
                it.key == incoming.key
            }

        val incomingUntil =
            after
                .firstOrNull {
                    it.key != incoming.key
                }
                ?.validFrom

        val mergedSupports =
            listOfNotNull(
                sameRelationAtTime,
                sameRelationAfter,
            )
                .fold(incoming.supports) { supports, state ->
                    mergeSupports(supports, state.supports)
                }

        val mergedConfidence =
            confidenceAggregator.aggregate(
                supports = mergedSupports,
                evidenceRegistry =
                    evidenceRegistry,
            )

        val normalizedIncoming =
            incoming.copy(
                confidence =
                    mergedConfidence,
                supports =
                    mergedSupports,
                validUntil =
                    incomingUntil,
                status =
                    if (incomingUntil == null) {
                        RelationStatus.ACTIVE
                    } else {
                        RelationStatus.SUPERSEDED
                    },
            )

        val remainingAfter =
            after.filterNot {
                it === sameRelationAfter
            }

        val rebuilt =
            (
                rebuiltBefore +
                    conflictingAtSameTime +
                    normalizedIncoming +
                    remainingAfter
                )
                .sortedBy {
                    it.validFrom
                }

        validateTimeline(rebuilt)

        return rebuilt
    }

    private fun mergeIntoBeforeLast(
        before: List<RelationState>,
        beforeLast: RelationState,
        after: List<RelationState>,
        incoming: RelationState,
        evidenceRegistry: EvidenceRepository,
    ): List<RelationState> {

        val mergedSupports =
            mergeSupports(
                beforeLast.supports,
                incoming.supports,
            )

        val mergedConfidence =
            confidenceAggregator.aggregate(
                supports = mergedSupports,
                evidenceRegistry =
                    evidenceRegistry,
            )

        val incomingUntil =
            after
                .firstOrNull {
                    it.key != incoming.key
                }
                ?.validFrom

        val mergedState =
            if (incomingUntil == null) {
                beforeLast.copy(
                    confidence =
                        mergedConfidence,
                    supports =
                        mergedSupports,
                )
            } else {
                beforeLast.copy(
                    confidence =
                        mergedConfidence,
                    supports =
                        mergedSupports,
                    validUntil =
                        incomingUntil,
                    status =
                        RelationStatus.SUPERSEDED,
                )
            }

        val rebuilt =
            (
                before.dropLast(1) +
                    mergedState +
                    after
                )
                .sortedBy {
                    it.validFrom
                }

        validateTimeline(rebuilt)

        return rebuilt
    }

    /**
     * 合并 supports：
     * 同一个 evidenceId 只保留一份 support，取最高 semanticConfidence。
     */
    private fun mergeSupports(
        first: Set<RelationSupport>,
        second: Set<RelationSupport>,
    ): Set<RelationSupport> {
        return (first + second)
            .groupBy { it.evidenceId }
            .map { (_, supports) ->
                supports.maxBy { it.semanticConfidence }
            }
            .toSet()
    }

    /**
     * 时间线重建之后的不变量：
     * 同一 subject + slot 的任意两个状态的有效区间不能重叠。
     */
    private fun validateTimeline(
        timeline: List<RelationState>,
    ) {
        val sorted =
            timeline.sortedBy {
                it.validFrom
            }

        sorted.zipWithNext()
            .forEach { (first, second) ->

                val firstUntil =
                    first.validUntil

                require(
                    firstUntil == null ||
                        firstUntil <=
                        second.validFrom
                ) {
                    "Temporal relation intervals cannot overlap"
                }
            }
    }
}
