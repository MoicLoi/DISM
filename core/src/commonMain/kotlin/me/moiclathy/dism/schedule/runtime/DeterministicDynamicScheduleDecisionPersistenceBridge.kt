package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.DeterministicDynamicScheduleDecisionSnapshotBuilder
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceInput
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceAssembler
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleDecisionPersistenceBridge(
    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val tracePersistence:
        DynamicScheduleDecisionTracePersistenceService,

    /**
     * 12.26A：
     *
     * 保存完整历史 Runtime Input。
     */
    private val replayInputPersistence:
        DynamicScheduleDecisionReplayInputSnapshotPersistenceService,

    private val snapshotBuilder:
        DeterministicDynamicScheduleDecisionSnapshotBuilder =
        DeterministicDynamicScheduleDecisionSnapshotBuilder(),

    private val traceAssembler:
        DynamicScheduleDecisionTraceAssembler,

    /**
     * 13.6
     *
     * Trace + Snapshot + ReplayInput 的跨表原子写入。
     */
    private val atomicPersistence:
        DynamicScheduleDecisionAtomicPersistence,
) : DynamicScheduleDecisionPersistenceBridge {

    override suspend fun persist(
        input:
            DynamicScheduleDecisionPersistenceInput,
    ): DynamicScheduleDecisionPersistenceResult {

        val snapshots =
            mutableListOf<DynamicScheduleDecisionSnapshot>()

        val traces =
            mutableListOf<DynamicScheduleDecisionTraceRecord>()

        val replayInputs =
            mutableListOf<DynamicScheduleDecisionReplayInputSnapshot>()

        input.intelligence.inputModel.items.forEach { item ->

            val snapshot =
                buildSnapshot(
                    item = item,
                    intelligence = input.intelligence,
                    now = input.now,
                    engineVersion = input.engineVersion,
                )

            /*
             * -----------------------------------------------------
             * Trace
             * -----------------------------------------------------
             */

            /*
             * -----------------------------------------------------
             * 13.2
             *
             * Runtime 先把 Result 投影成 Decision Evidence，
             * Trace 只消费 Evidence，不再反向依赖 Runtime。
             * -----------------------------------------------------
             */

            val evidence =
                DynamicScheduleDecisionTraceEvidenceFactory
                    .from(
                        item = item,
                        intelligence = input.intelligence,
                        now = input.now,
                    )

            val trace =
                traceAssembler.assemble(
                    DynamicScheduleDecisionTraceInput(
                        evidence = evidence,

                        /*
                         * 12.21 的 Conflict / Explanation 已经接入 Runtime，
                         * 但 Trace 只有在显式收到 message 时才会写入
                         * CONFLICT_RESOLUTION / EXPLANATION 阶段。
                         *
                         * 这里把 Runtime 的结论回填进 Trace，
                         * 否则 Replay Verification 会因为
                         * "历史 Trace 没有 EXPLANATION 阶段"
                         * 判定 explanation parity 不一致。
                         */
                        conflictMessage =
                            evidence.conflict
                                ?.let { result ->
                                    buildString {
                                        append("Attention conflict on ")
                                        append(item.id)
                                        append(": original=")
                                        append(
                                            result.conflict
                                                ?.originalPriority
                                                ?: result.resolvedPriority,
                                        )
                                        append(", resolved=")
                                        append(result.resolvedPriority)

                                        val reason =
                                            result.conflict
                                                ?.reasons
                                                ?.firstOrNull()
                                                ?: result.reasons
                                                    .firstOrNull()

                                        if (reason != null) {
                                            append(" (")
                                            append(reason)
                                            append(")")
                                        }
                                    }
                                },

                        conflictDecisive =
                            evidence.conflict
                                ?.hardConstraintPresent
                                ?: false,

                        explanationMessage =
                            evidence.explanation?.summary,
                    ),
                )

            val traceRecord =
                DynamicScheduleDecisionTraceRecord(
                    trace = trace,

                    createdAt =
                        input.now,

                    scheduleRevision =
                        input.intelligence
                            .inputModel
                            .revision,

                    engineVersion =
                        input.engineVersion,
                )

            /*
             * -----------------------------------------------------
             * Snapshot ← Trace
             * -----------------------------------------------------
             */

            val linkedSnapshot =
                snapshot.copy(
                    traceId =
                        trace.id,
                )

            /*
             * -----------------------------------------------------
             * 12.26A
             *
             * 在生成最终 Snapshot 后，
             * 立刻建立与它一一对应的完整 Replay Input。
             *
             * 注意：
             *
             * 这里保存的是 Runtime 真正收到的 input，
             * 而不是从 intelligence result 反推出来的 input。
             * -----------------------------------------------------
             */

            val replayInput =
                DynamicScheduleDecisionReplayInputSnapshot(
                    decisionSnapshot =
                        linkedSnapshot,

                    /*
                     * 12.26A 修正：
                     *
                     * 直接保存 Runtime 真正收到的 Input，
                     * 而不是从 intelligence result 反推。
                     */
                    input =
                        input.input,

                    previousState =
                        input.previous,

                    policy =
                        input.policy,

                    evaluatedAt =
                        input.now,

                    scheduleRevision =
                        input
                            .input
                            .model
                            .revision,

                    engineVersion =
                        input.engineVersion,
                )

            /*
             * -----------------------------------------------------
             * 13.6 Atomic persistence
             *
             * 一个 Decision 的三份投影在**一个事务**里落地：
             *
             * 1. Trace             "为什么这么判断"
             * 2. Decision Snapshot "当时系统怎么看"
             * 3. Replay Input      "能不能重跑当时的决策"
             *
             * 中途失败 → 三份一起回滚，
             * 不会留下"有 Snapshot 但没有 ReplayInput"的半截历史。
             * -----------------------------------------------------
             */

            atomicPersistence.persist(
                trace = traceRecord,
                snapshot = linkedSnapshot,
                replayInput = replayInput,
            )

            traces += traceRecord
            snapshots += linkedSnapshot
            replayInputs += replayInput
        }

        return DynamicScheduleDecisionPersistenceResult(
            snapshots =
                snapshots,

            traces =
                traces,

            replayInputs =
                replayInputs,
        )
    }

    private fun buildSnapshot(
        item:
            DynamicScheduleItem,

        intelligence:
            DynamicScheduleIntelligenceResult,

        now:
            kotlin.time.Instant,

        engineVersion:
            String,
    ): DynamicScheduleDecisionSnapshot {

        val contextMatch =
            intelligence
                .contextResult
                .matches
                .firstOrNull {
                    it.itemId == item.id
                }

        val dependencyMatches =
            intelligence
                .dependencyResult
                .matches
                .filter {
                    it.dependency.toItemId == item.id
                }

        val adaptation =
            intelligence
                .priorityAdaptation
                .adaptations
                .firstOrNull {
                    it.itemId == item.id
                }

        /*
         * 缝合修正：
         *
         * 12.21 Conflict Resolution 接入 Runtime 之后，
         * 最终 decision priority 是 conflict-resolved priority
         * （见 Runtime Phase 8：Recommendation.priority 被替换）。
         *
         * Snapshot 记录的是"这一次决策最终的结论"，
         * 所以这里必须以 conflict result 为准，
         * 否则 Replay Verification 会拿
         * pre-conflict priority 去比对 post-conflict priority，
         * 永远得到 BEHAVIOR_CHANGED。
         */
        val decisionPriority =
            intelligence
                .attentionConflicts[item.id]
                ?.resolvedPriority
                ?: adaptation
                    ?.effectivePriority
                ?: item.priority

        val dependencySnapshot =
            buildDependencySnapshot(
                dependencyMatches,
            )

        val contextSnapshot =
            buildContextSnapshot(
                contextMatch,
                inputContextKeys =
                    inferActiveContextKeys(
                        contextMatch,
                    ),
            )

        /*
         * 当前 Snapshot 模型仍然只保存最终决策摘要。
         *
         * 完整历史 Memory 在 12.26A 中由
         * ReplayInputSnapshot 保存。
         *
         * 因此这里继续保持 1.0，
         * 防止未来 Replay 对最终 priority 重复乘 Memory factor。
         */
        val memorySnapshot =
            DynamicScheduleAttentionMemorySnapshot(
                globalFactor = 1.0,
                contextFactor = 1.0,
                adaptationFactor = 1.0,
            )

        /*
         * 12.26A 不再把 Policy 当作 Replay 的唯一来源。
         *
         * Snapshot 中保留摘要，
         * ReplayInputSnapshot 中保存完整 Runtime Policy。
         */
        val policySnapshot =
            DynamicSchedulePolicySnapshot(
                minimumAttentionPriority = 0.0,

                minimumAttentionConfidence = 0.0,

                minimumHardConstraintPriority = 0.75,

                explicitRequestPriority = 1.0,

                maximumLearnedSuppression = 0.5,
            )

        return snapshotBuilder.build(
            item =
                DynamicScheduleItemSnapshot(
                    itemId =
                        item.id,

                    title =
                        item.title,

                    state =
                        item.state,

                    priority =
                        decisionPriority,

                    confidence =
                        item.confidence,
                ),

            context =
                contextSnapshot,

            attentionMemory =
                memorySnapshot,

            dependency =
                dependencySnapshot,

            policy =
                policySnapshot,

            capturedAt =
                now,

            scheduleRevision =
                intelligence
                    .inputModel
                    .revision,

            engineVersion =
                engineVersion,

            traceId =
                null,
        )
    }

    private fun buildDependencySnapshot(
        matches:
            List<
                me.moiclathy.dism.schedule.dependency
                    .DynamicScheduleDependencyMatch,
                >,
    ): DynamicScheduleDependencySnapshot {

        val blocking =
            matches.filter {
                it.blocking
            }

        val missing =
            blocking
                .filterNot {
                    it.satisfied
                }
                .map {
                    it.dependency.id.value
                }

        return DynamicScheduleDependencySnapshot(
            requiredSatisfied =
                missing.isEmpty(),

            missingDependencyIds =
                missing,

            requiredDependencyCount =
                blocking.size,
        )
    }

    private fun buildContextSnapshot(
        match:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextMatch?,

        inputContextKeys:
            Set<String>,
    ): DynamicScheduleContextSnapshot {

        return DynamicScheduleContextSnapshot(
            activeContextKeys =
                inputContextKeys,

            requiredSatisfied =
                match
                    ?.requiredSatisfied
                    ?: true,

            excludedSatisfied =
                match
                    ?.let {
                        !it.excludedSatisfied
                    }
                    ?: true,
        )
    }

    private fun inferActiveContextKeys(
        match:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextMatch?,
    ): Set<String> {

        /*
         * Decision Snapshot 只保留 Context 摘要。
         *
         * 真正的 activeContextKeys 来自
         * DynamicScheduleIntelligenceInput，
         * 并由 ReplayInputSnapshot 保存。
         *
         * 因此这里不能从 Match 反推。
         */
        return emptySet()
    }
}
