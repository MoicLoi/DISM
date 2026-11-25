package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

import me.moiclathy.dism.schedule.snapshot.DeterministicDynamicScheduleDecisionSnapshotBuilder
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceAssembler
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceAssembler
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceInput
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

class DynamicScheduleDecisionRuntimeService(
    private val runtime:
        DynamicScheduleIntelligenceRuntime,

    private val persistence:
        DynamicScheduleDecisionRuntimePersistence,

    private val snapshotBuilder:
        DeterministicDynamicScheduleDecisionSnapshotBuilder =
        DeterministicDynamicScheduleDecisionSnapshotBuilder(),

    private val traceBuilder:
        DeterministicDynamicScheduleDecisionTraceBuilder =
        DeterministicDynamicScheduleDecisionTraceBuilder(),

    /**
     * 12.22：Trace 不再在 Runtime 中手写阶段序列，
     * 统一交给 TraceAssembler 组装。
     */
    private val traceAssembler:
        DynamicScheduleDecisionTraceAssembler =
        DeterministicDynamicScheduleDecisionTraceAssembler(),

    private val engineVersion:
        String = "12.20",
) {

    suspend fun evaluateAndPersist(
        input:
            DynamicScheduleIntelligenceInput,

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        now: Instant,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ): DynamicScheduleDecisionRuntimeResult {

        val intelligence =
            runtime.evaluate(
                input = input,
                previous = previous,
                now = now,
                policy = policy,
            )

        val snapshots =
            mutableListOf<DynamicScheduleDecisionSnapshot>()

        val traces =
            mutableListOf<DynamicScheduleDecisionTraceRecord>()

        val items =
            intelligence
                .contextResult
                .projectedModel
                .items

        items.forEach { item ->

            val snapshot =
                buildSnapshot(
                    item = item,
                    intelligence = intelligence,
                    input = input,
                    policy = policy,
                    now = now,
                )

            val trace =
                buildTrace(
                    item = item,
                    intelligence = intelligence,
                    now = now,
                )

            val finalSnapshot =
                snapshot.copy(
                    traceId = trace.id,
                )

            val finalTrace =
                DynamicScheduleDecisionTraceRecord(
                    trace = trace,
                    createdAt = now,
                    scheduleRevision =
                        intelligence
                            .contextResult
                            .projectedModel
                            .revision,
                    engineVersion = engineVersion,
                )

            persistence.persistSnapshot(
                finalSnapshot,
            )

            persistence.persistTrace(
                finalTrace,
            )

            snapshots += finalSnapshot
            traces += finalTrace
        }

        return DynamicScheduleDecisionRuntimeResult(
            intelligence = intelligence,
            snapshots = snapshots,
            traces = traces,
            diagnostics = intelligence.diagnostics,
        )
    }

    private fun buildSnapshot(
        item:
            me.moiclathy.dism.schedule.model.DynamicScheduleItem,

        intelligence:
            DynamicScheduleIntelligenceResult,

        input:
            DynamicScheduleIntelligenceInput,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy,

        now: Instant,
    ): DynamicScheduleDecisionSnapshot {

        val candidate =
            intelligence
                .attentionProjection
                .candidates
                .firstOrNull {
                    it.itemId == item.id
                }

        /*
         * Snapshot 记录的是：
         *
         * “真正交给 Attention Runtime 的 priority”
         *
         * 而不是最原始的 schedule priority。
         *
         * 这样 12.18 replay 至少可以准确重建
         * 当时 Attention 层看到的 priority。
         */
        val decisionPriority =
            candidate
                ?.candidate
                ?.recommendation
                ?.priority
                ?: item.priority

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
                    it.dependency.toItemId == item.id &&
                        it.dependency.effective
                }

        val requiredDependencies =
            dependencyMatches
                .filter {
                    it.blocking
                }

        val missingDependencies =
            requiredDependencies
                .filterNot {
                    it.satisfied
                }
                .map {
                    it.dependency.id.value
                }

        val requiredSatisfied =
            missingDependencies.isEmpty()

        val dependencySnapshot =
            DynamicScheduleDependencySnapshot(
                requiredSatisfied =
                    requiredSatisfied,

                missingDependencyIds =
                    missingDependencies,

                requiredDependencyCount =
                    requiredDependencies.size,
            )

        val contextSnapshot =
            DynamicScheduleContextSnapshot(
                activeContextKeys =
                    input.activeContextKeys,

                requiredSatisfied =
                    contextMatch
                        ?.requiredSatisfied
                        ?: true,

                excludedSatisfied =
                    /*
                     * 12.23 语义修正：
                     *
                     * DynamicScheduleContextMatch.excludedSatisfied
                     * 表示“至少一个 EXCLUDED context 当前 active”，
                     * 而 Snapshot 字段表示“Excluded 条件是否满足”，
                     * 因此必须取反，否则 replay 会出现反向语义。
                     */
                    contextMatch
                        ?.let { !it.excludedSatisfied }
                        ?: true,
            )

        /*
         * 当前 Runtime 已经把 adaptation/context factor
         * 实际应用到 temporary model 中。
         *
         * 因此这里把最终 decision priority 作为 snapshot
         * 的 priority，并保持 memory factor = 1。
         *
         * 这样不会在 replay 时把同一个 factor 重复乘一次。
         */
        val attentionMemory =
            DynamicScheduleAttentionMemorySnapshot(
                globalFactor = 1.0,
                contextFactor = 1.0,
                adaptationFactor = 1.0,
            )

        val policySnapshot =
            DynamicSchedulePolicySnapshot(
                minimumAttentionPriority =
                    policy
                        .attentionPolicy
                        .minimumPriority,

                minimumAttentionConfidence =
                    policy
                        .attentionProjectionPolicy
                        .minimumConfidence,

                /*
                 * 12.12 尚未正式接入当前 Runtime。
                 *
                 * 这里保存当前 Conflict Policy 的默认值，
                 * 等 12.21 真正接入 Conflict Resolver
                 * 后再改成 Runtime policy。
                 */
                minimumHardConstraintPriority =
                    0.75,

                explicitRequestPriority =
                    1.0,

                maximumLearnedSuppression =
                    0.5,
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
                attentionMemory,

            dependency =
                dependencySnapshot,

            policy =
                policySnapshot,

            capturedAt =
                now,

            scheduleRevision =
                intelligence
                    .contextResult
                    .projectedModel
                    .revision,

            engineVersion =
                engineVersion,

            traceId = null,
        )
    }

    private fun buildTrace(
        item:
            me.moiclathy.dism.schedule.model.DynamicScheduleItem,

        intelligence:
            DynamicScheduleIntelligenceResult,

        now: Instant,
    ): DynamicScheduleDecisionTrace {

        val evidence =
            DynamicScheduleDecisionTraceEvidenceFactory
                .from(
                    item = item,
                    intelligence = intelligence,
                    now = now,
                )

        val conflict =
            evidence.conflict

        val explanation =
            evidence.explanation

        return traceAssembler.assemble(
            DynamicScheduleDecisionTraceInput(
                evidence = evidence,

                conflictMessage =
                    conflict
                        ?.reasons
                        ?.joinToString("; "),

                conflictDecisive =
                    conflict?.conflict != null,

                explanationMessage =
                    explanation?.summary,
            ),
        )
    }
}
