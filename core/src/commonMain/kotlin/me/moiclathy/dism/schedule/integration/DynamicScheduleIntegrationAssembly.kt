package me.moiclathy.dism.schedule.integration

import me.moiclathy.dism.api.DismInternalApi
import kotlin.time.Instant

import me.moiclathy.dism.core.DynamicScheduleCore
import me.moiclathy.dism.core.DynamicScheduleCoreFactory
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationBoundaryPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationBoundaryService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationBoundaryState
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationConfidence
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationConfidencePolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationConfidenceService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBoundedAdaptation
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenance as AdaptiveDecisionProvenance
import me.moiclathy.dism.schedule.adaptation.audit.DeterministicDynamicScheduleDecisionAuditBuilder
import me.moiclathy.dism.schedule.adaptation.audit.DynamicScheduleDecisionAudit
import me.moiclathy.dism.schedule.adaptation.audit.DynamicScheduleDecisionAuditBuilder
import me.moiclathy.dism.schedule.adaptation.provenance.DeterministicDynamicScheduleDecisionProvenanceBuilder
import me.moiclathy.dism.schedule.adaptation.provenance.DeterministicDynamicScheduleDecisionProvenanceExplainer
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenanceBuilder
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenanceExplanation
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenanceInput
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenanceQuery
import me.moiclathy.dism.schedule.adaptation.provenance.DynamicScheduleDecisionProvenanceQueryResult
import me.moiclathy.dism.schedule.adaptation.verification.DeterministicDynamicScheduleAdaptationVerificationEngine
import me.moiclathy.dism.schedule.adaptation.verification.DynamicScheduleAdaptationInputFingerprint
import me.moiclathy.dism.schedule.adaptation.verification.DynamicScheduleAdaptationVerificationEngine
import me.moiclathy.dism.schedule.adaptation.verification.DynamicScheduleAdaptationVerificationPolicy
import me.moiclathy.dism.schedule.adaptation.verification.DynamicScheduleAdaptationVerificationReport
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayInput
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayResult
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayService
import me.moiclathy.dism.schedule.adaptation.replay.InMemoryDynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.adaptation.persistence.DynamicScheduleAdaptationPersistenceRecord
import me.moiclathy.dism.schedule.adaptation.persistence.DynamicScheduleAdaptationPersistenceService
import me.moiclathy.dism.schedule.adaptation.persistence.DynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.adaptation.persistence.DeterministicDynamicScheduleAdaptationPersistenceService
import me.moiclathy.dism.schedule.adaptation.persistence.InMemoryDynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.adaptation.DeterministicDynamicScheduleHistoricalFeatureExtractor
import me.moiclathy.dism.schedule.adaptation.DeterministicDynamicScheduleHumanOverrideService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverrideInput
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverridePolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverrideResult
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverrideService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionAdaptation
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionAdaptationService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionInfluence
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralAdaptation
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralAdaptationService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralAdaptationSet
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralAdaptationSetBuilder
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralPriorityAdaptation
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralPriorityAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralPriorityAdaptationService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleBehavioralPriorityInfluence
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionFeatureSet
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretation
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHistoricalFeatureExtractor
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleWeightAdaptation
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleWeightAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleWeightAdaptationService
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleWeightAdaptationSet
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionProjector
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingInput
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBinder
import me.moiclathy.dism.schedule.context.DynamicScheduleContextPriorityInfluence
import me.moiclathy.dism.schedule.context.DynamicScheduleContextPriorityPolicy
import me.moiclathy.dism.schedule.context.DynamicScheduleContextPriorityService
import me.moiclathy.dism.schedule.context.DynamicScheduleContextSensitivePriority
import me.moiclathy.dism.schedule.context.DynamicScheduleContextSensitivePriorityService
import me.moiclathy.dism.schedule.context.DeterministicDynamicScheduleContextBinder
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencySet
import me.moiclathy.dism.schedule.engine.DefaultDynamicScheduleEngineRegistry
import me.moiclathy.dism.schedule.engine.DynamicScheduleEngineRegistry
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryCompactionBatchResult
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryIntegrityBatchReport
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryIntegrityChecker
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryIntegrityReport
import me.moiclathy.dism.schedule.history.DefaultDynamicScheduleHistoryRepairExecutor
import me.moiclathy.dism.schedule.history.DeterministicDynamicScheduleDecisionHistoryService
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryEntry
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryQuery
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryResult
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryService
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionTimeline
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionTimelineService
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairAnalyzer
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairExecutor
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairPlan
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairPlanner
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairPolicy
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairResult
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRepairSummary
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRetentionPolicy
import me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRetentionService
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DeterministicDynamicScheduleDecisionReplayReportBuilder
import me.moiclathy.dism.schedule.replay.DeterministicDynamicScheduleHistoricalRuntimeReplayEngine
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotPersistenceService
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoryReplayBatchPolicy
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoryReplayBatchResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoryReplayBatchService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.runtime.DeterministicDynamicScheduleDecisionPersistenceBridge
import me.moiclathy.dism.schedule.runtime.DeterministicDynamicScheduleIntelligenceRuntime
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionRuntimeEvaluation
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionRuntimeFacade
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.runtime.InMemoryDynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQueryResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.state.DynamicScheduleStateProjectionService
import me.moiclathy.dism.schedule.state.DynamicScheduleStateProjectionResult
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceAssembler
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQuery
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQueryResult
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionDiagnostic


/**
 * 产品化缝合：把 11.x / 12.x / 13.1 装成一条可运行的链路。
 *
 * ```text
 * ExternalSchedule (ICS / 喜鹊儿)
 *         ↓  ExternalScheduleToDynamicScheduleMapper
 * DynamicScheduleModel
 *         ↓  12.2 State Projection
 * 状态已投影的 Schedule
 *         ↓  DynamicScheduleCore.evaluate()
 * 12.10 Runtime（Dependency → Context → Memory → Priority → Attention → Conflict → Explanation）
 *         ↓  Persistence Bridge
 * Snapshot / Trace / Replay Input（可重放）
 *         ↓
 * DynamicScheduleCore.replay() / verifyReplay()
 * ```
 *
 * 这是**第一版可运行装配**：
 *
 * - 12.x 的 Snapshot / Trace / ReplayInput 仍然是 InMemory 实现
 *   （12.15 / 12.18 / 12.26A 目前只提供了 InMemory Store）；
 * - 11.x 的 Journal 已经有 SQLDelight 适配器，
 *   可以在平台层注入 `ExternalScheduleSyncJournalPersistenceRepository` 替换。
 */
@DismInternalApi
class DynamicScheduleIntegrationAssembly(
    private val mapper:
        ExternalScheduleToDynamicScheduleMapper =
        ExternalScheduleToDynamicScheduleMapper(),

    private val stateProjectionService:
        DynamicScheduleStateProjectionService =
        DynamicScheduleStateProjectionService(),

    /**
     * 12.x Snapshot / Trace / ReplayInput 存储。
     *
     * 默认 InMemory，方便单测与演示；
     * 产品化装配走 [from]，由平台层注入 SQLDelight 实现。
     */
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore =
        InMemoryDynamicScheduleDecisionSnapshotStore(),

    private val traceStore:
        DynamicScheduleDecisionTraceStore =
        InMemoryDynamicScheduleDecisionTraceStore(),

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore(),

    /**
     * 13.6：一次 Decision 的原子写入边界。
     *
     * 默认是 InMemory（带 rollback），
     * [from] 会把平台层的 SQLDelight 实现传进来。
     */
    private val atomicPersistence:
        DynamicScheduleDecisionAtomicPersistence =
        InMemoryDynamicScheduleDecisionAtomicPersistence(
            snapshotStore = snapshotStore,
            traceStore = traceStore,
            replayInputStore = replayInputStore,
        ),

    /**
     * 13.11：Engine Registry。
     *
     * 它是**当前引擎版本的唯一来源**：
     *
     * ```text
     * Decision Snapshot / Trace / ReplayInput 的 engineVersion
     * Replay 的 replayEngineVersion
     * Verification 的 replayEngineVersion
     * 版本兼容性规则
     * 迁移规则
     * ```
     *
     * 全部来自这里，Assembly 不再自己定义 `ENGINE_VERSION`。
     *
     * 默认值是"当前版本只认识自己"：
     * 未知版本一律不能直接重放。
     */
    private val engineRegistry:
        DynamicScheduleEngineRegistry =
        DefaultDynamicScheduleEngineRegistry.create(),

    /**
     * 13.12：Decision Provenance 存储。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore(),

    /**
     * 14.11：Adaptive Decision 持久化存储。
     *
     * 默认内存实现；[from] 会把它换成
     * `DismPersistence.scheduleAdaptations`。
     */
    private val adaptationStore:
        DynamicScheduleAdaptationPersistenceStore =
        InMemoryDynamicScheduleAdaptationPersistenceStore(),

    /**
     * 14.12：Adaptation Replay 的历史输入存储。
     */
    private val adaptationReplayInputStore:
        DynamicScheduleAdaptationReplayInputStore =
        InMemoryDynamicScheduleAdaptationReplayInputStore(),
) {

    val snapshotPersistence =
        DynamicScheduleDecisionSnapshotPersistenceService(
            snapshotStore,
        )

    val tracePersistence =
        DynamicScheduleDecisionTracePersistenceService(
            traceStore,
        )

    val replayInputPersistence =
        DynamicScheduleDecisionReplayInputSnapshotPersistenceService(
            replayInputStore,
        )

    /**
     * 运行期使用的 Runtime。
     *
     * attentionProjector 需要 identity resolver；
     * [build] 会按传入的 identity 映射创建实例。
     */
    private fun runtime(
        resolver:
            DeterministicScheduleItemIdentityResolver,
    ) =
        DeterministicDynamicScheduleIntelligenceRuntime(
            attentionProjector =
                DeterministicDynamicScheduleAttentionProjector(
                    identityResolver = resolver,
                ),
        )

    /**
     * 绑定 identity 之后装配 Core。
     *
     * @param identityByItem schedule item → external key
     */
    fun build(
        identityByItem:
            Map<DynamicScheduleItemId, String> = emptyMap(),
    ): DynamicScheduleSession {

        val resolver =
            DeterministicScheduleItemIdentityResolver.from(
                identityByItem,
            )

        val runtime =
            runtime(resolver)

        val bridge =
            DeterministicDynamicScheduleDecisionPersistenceBridge(
                snapshotPersistence = snapshotPersistence,
                tracePersistence = tracePersistence,
                replayInputPersistence =
                    replayInputPersistence,
                traceAssembler =
                    DeterministicDynamicScheduleDecisionTraceAssembler(),
                atomicPersistence = atomicPersistence,
            )

        val facade =
            DynamicScheduleDecisionRuntimeFacade(
                runtime = runtime,
                persistenceBridge = bridge,
            )

        /*
         * 13.11：当前引擎版本只有一个来源。
         */
        val engine = engineRegistry.current

        val replayService =
            DynamicScheduleHistoricalRuntimeReplayService(
                replayInputPersistence =
                    replayInputPersistence,

                snapshotPersistence =
                    snapshotPersistence,

                tracePersistence =
                    tracePersistence,

                replayEngine =
                    DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
                        runtime = runtime,
                    ),

                replayEngineVersion = engine.version,

                compatibilityRegistry =
                    engineRegistry.compatibilityRegistry,

                provenanceStore = provenanceStore,
            )

        val verificationService =
            DynamicScheduleReplayVerificationService(
                replayService = replayService,

                /*
                 * 12.28 的默认值是 "12.28"；这里必须跟随当前引擎版本，
                 * 否则任何一次 verify 都会得到 ENGINE_VERSION_MISMATCH。
                 *
                 * 13.11：版本来自 Engine Registry，不再由 Assembly 写死。
                 */
                replayEngineVersion = engine.version,

                provenanceStore = provenanceStore,
            )

        /*
         * 13.14：批量重放与时间线。
         *
         * 两者刻意分开：
         *
         * ```text
         * Timeline    → 历史视图（纯读取）
         * ReplayBatch → 计算操作（重新执行 Runtime）
         * ```
         */
        val replayBatchService =
            DynamicScheduleHistoryReplayBatchService(
                replayService = replayService,
                verificationService = verificationService,
            )

        /*
         * 13.13：统一历史决策查询。
         *
         * 它是 Snapshot / Trace / Provenance / Replay / Verification
         * 之上的一层产品级领域 API。
         */
        val historyService =
            DeterministicDynamicScheduleDecisionHistoryService(
                snapshotPersistence = snapshotPersistence,

                tracePersistence = tracePersistence,

                provenanceStore = provenanceStore,

                replayService = replayService,

                verificationService = verificationService,
            )

        /*
         * 13.5：历史生命周期只有一个协调者。
         *
         * App 不应该自己去操作三个 Store，
         * 否则很容易破坏 Snapshot → Trace / ReplayInput 的引用关系。
         */
        val historyRetention =
            DynamicScheduleHistoryRetentionService(
                snapshotStore = snapshotStore,
                traceStore = traceStore,
                replayInputStore = replayInputStore,
            )

        /*
         * 13.7：历史完整性体检（只读）。
         */
        val historyIntegrity =
            DynamicScheduleHistoryIntegrityChecker(
                snapshotStore = snapshotStore,
                traceStore = traceStore,
                replayInputStore = replayInputStore,

                /* 13.17：双向引用检查需要 provenance。 */
                provenanceStore = provenanceStore,
            )

        return DynamicScheduleSession(
            core =
                DynamicScheduleCoreFactory.create(
                    runtimeFacade = facade,
                    historicalReplayService = replayService,
                    replayVerificationService =
                        verificationService,
                ),

            facade = facade,

            mapper = mapper,

            stateProjectionService =
                stateProjectionService,

            snapshotPersistence =
                snapshotPersistence,

            tracePersistence =
                tracePersistence,

            replayInputPersistence =
                replayInputPersistence,

            historyRetention =
                historyRetention,

            historyIntegrity =
                historyIntegrity,

            historyRepairPlanner =
                DynamicScheduleHistoryRepairPlanner(),

            historyRepairExecutor =
                DefaultDynamicScheduleHistoryRepairExecutor(
                    replayInputStore = replayInputStore,
                ),

            historyRepairAnalyzer =
                DynamicScheduleHistoryRepairAnalyzer(
                    snapshotStore = snapshotStore,
                    traceStore = traceStore,
                    replayInputStore = replayInputStore,
                ),

            replayService = replayService,

            engineRegistry = engineRegistry,

            provenanceStore = provenanceStore,

            historyService = historyService,

            featureExtractor =
                DeterministicDynamicScheduleHistoricalFeatureExtractor(
                    history = historyService,
                ),

            interpretationService =
                DynamicScheduleDecisionInterpretationService(),

            behavioralAdaptationService =
                DynamicScheduleBehavioralAdaptationService(),

            weightAdaptationService =
                DynamicScheduleWeightAdaptationService(),

            attentionAdaptationService =
                DynamicScheduleAttentionAdaptationService(),

            behavioralPriorityAdaptationService =
                DynamicScheduleBehavioralPriorityAdaptationService(),

            contextPriorityService =
                DynamicScheduleContextPriorityService(),

            contextSensitivePriorityService =
                DynamicScheduleContextSensitivePriorityService(),

            contextBinder =
                DeterministicDynamicScheduleContextBinder(),

            adaptationConfidenceService =
                DynamicScheduleAdaptationConfidenceService(),

            adaptationBoundaryService =
                DynamicScheduleAdaptationBoundaryService(),

            adaptationPersistenceService =
                DeterministicDynamicScheduleAdaptationPersistenceService(
                    store = adaptationStore,
                ),

            adaptationReplayInputStore =
                adaptationReplayInputStore,

            adaptationStore = adaptationStore,

            adaptationReplayService =
                DynamicScheduleAdaptationReplayService(
                    adaptationStore = adaptationStore,
                    replayInputStore =
                        adaptationReplayInputStore,
                ),

            adaptationVerificationEngine =
                DeterministicDynamicScheduleAdaptationVerificationEngine(),

            provenanceBuilder =
                DeterministicDynamicScheduleDecisionProvenanceBuilder(),

            provenanceExplainer =
                DeterministicDynamicScheduleDecisionProvenanceExplainer(),

            auditBuilder =
                DeterministicDynamicScheduleDecisionAuditBuilder(),

            humanOverrideService =
                DeterministicDynamicScheduleHumanOverrideService(),

            replayBatchService = replayBatchService,

            timelineService =
                DynamicScheduleDecisionTimelineService(
                    history = historyService,
                ),
        )
    }

    companion object {

        /**
         * 产品化入口：用 [DismPersistence] 里的存储装配。
         *
         * 平台层只要把 `DismPersistenceFactory` 的 driver 指向真实数据库，
         * 这条链路产出的 Snapshot / Trace / Replay Input 就跟着落盘，
         * 重启后依然可以 replay / verify。
         */
        fun from(
            persistence: DismPersistence,

            engineRegistry:
                DynamicScheduleEngineRegistry =
                DefaultDynamicScheduleEngineRegistry.create(),
        ): DynamicScheduleIntegrationAssembly =
            DynamicScheduleIntegrationAssembly(
                snapshotStore =
                    persistence.scheduleSnapshots,

                traceStore =
                    persistence.scheduleTraces,

                replayInputStore =
                    persistence.scheduleReplayInputs,

                atomicPersistence =
                    persistence.scheduleDecisionAtomicPersistence,

                engineRegistry = engineRegistry,

                provenanceStore =
                    persistence.scheduleProvenance,

                adaptationStore =
                    persistence.scheduleAdaptations,

                adaptationReplayInputStore =
                    persistence.scheduleAdaptationReplayInputs,
            )
    }
}

/**
 * 一次装配好的会话（可重复调用 evaluate / replay / verify）。
 */
@DismInternalApi
class DynamicScheduleSession internal constructor(
    val core: DynamicScheduleCore,

    private val facade:
        DynamicScheduleDecisionRuntimeFacade,

    private val mapper:
        ExternalScheduleToDynamicScheduleMapper,

    private val stateProjectionService:
        DynamicScheduleStateProjectionService,

    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val tracePersistence:
        DynamicScheduleDecisionTracePersistenceService,

    private val replayInputPersistence:
        DynamicScheduleDecisionReplayInputSnapshotPersistenceService,

    /**
     * 13.5：历史保留 / 压缩。
     */
    private val historyRetention:
        DynamicScheduleHistoryRetentionService,

    /**
     * 13.7：历史完整性检查（只读）。
     */
    private val historyIntegrity:
        DynamicScheduleHistoryIntegrityChecker,

    /**
     * 13.8：历史恢复计划（只读，不执行）。
     */
    private val historyRepairPlanner:
        DynamicScheduleHistoryRepairPlanner,

    /**
     * 13.9：受策略约束的恢复执行器。
     */
    private val historyRepairExecutor:
        DynamicScheduleHistoryRepairExecutor,

    /**
     * 13.9.12：修复分析（完整性 + 孤儿）。
     */
    private val historyRepairAnalyzer:
        DynamicScheduleHistoryRepairAnalyzer,

    /**
     * 13.10：版本感知的 Replay。
     */
    private val replayService:
        DynamicScheduleHistoricalRuntimeReplayService,

    /**
     * 13.11：Engine Registry。
     *
     * App 通过它了解"当前引擎版本"、
     * "这条历史能不能重放"、"有没有迁移路径"，
     * 而不需要自己拼 Compatibility / Migration / Version。
     */
    val engineRegistry:
        DynamicScheduleEngineRegistry,

    /**
     * 13.12：Decision Provenance 存储。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore,

    /**
     * 13.13：统一历史决策查询。
     */
    private val historyService:
        DynamicScheduleDecisionHistoryService,

    /**
     * 14.1：历史特征提取（只读）。
     */
    private val featureExtractor:
        DynamicScheduleHistoricalFeatureExtractor,

    /**
     * 14.2：特征语义解释（只读）。
     */
    private val interpretationService:
        DynamicScheduleDecisionInterpretationService,

    /**
     * 14.3：行为适应模型（只读）。
     */
    private val behavioralAdaptationService:
        DynamicScheduleBehavioralAdaptationService,

    /**
     * 14.4：动态权重适配（只读）。
     */
    private val weightAdaptationService:
        DynamicScheduleWeightAdaptationService,

    /**
     * 14.5：Attention 适配（只读）。
     */
    private val attentionAdaptationService:
        DynamicScheduleAttentionAdaptationService,

    /**
     * 14.6：Priority 适配（只读）。
     */
    private val behavioralPriorityAdaptationService:
        DynamicScheduleBehavioralPriorityAdaptationService,

    /**
     * 14.7：Context → Priority 影响（只读）。
     */
    private val contextPriorityService:
        DynamicScheduleContextPriorityService,

    /**
     * 14.7：历史 Priority Adaptation × Context 合并（只读）。
     */
    private val contextSensitivePriorityService:
        DynamicScheduleContextSensitivePriorityService,

    /**
     * 12.4：Context 匹配器，用于把 Context 翻译成 priority 影响。
     */
    private val contextBinder:
        DynamicScheduleContextBinder,

    /**
     * 14.8：Adaptation Confidence（只读）。
     */
    private val adaptationConfidenceService:
        DynamicScheduleAdaptationConfidenceService,

    /**
     * 14.9：自适应安全边界（只读）。
     */
    private val adaptationBoundaryService:
        DynamicScheduleAdaptationBoundaryService,

    /**
     * 14.11：Adaptive Decision 持久化（会写库）。
     */
    private val adaptationPersistenceService:
        DynamicScheduleAdaptationPersistenceService,

    /**
     * 14.11：适配结果存储本体。
     *
     * [adaptationPersistenceService] 是写入路径；
     * 这里保留 Store 本身，供 14.13 的验证做"历史记录"读取。
     */
    private val adaptationStore:
        DynamicScheduleAdaptationPersistenceStore,

    /**
     * 14.12：Replay Input 存储（会写库）。
     */
    private val adaptationReplayInputStore:
        DynamicScheduleAdaptationReplayInputStore,

    /**
     * 14.12：Adaptation Replay（只读）。
     */
    private val adaptationReplayService:
        DynamicScheduleAdaptationReplayService,

    /**
     * 14.13：Adaptation Verification（纯计算）。
     */
    private val adaptationVerificationEngine:
        DynamicScheduleAdaptationVerificationEngine,

    /**
     * 14.15：Provenance 构建器。
     */
    private val provenanceBuilder:
        DynamicScheduleDecisionProvenanceBuilder =
        DeterministicDynamicScheduleDecisionProvenanceBuilder(),

    /**
     * 14.16：Provenance 解释器（纯函数）。
     */
    private val provenanceExplainer:
        DeterministicDynamicScheduleDecisionProvenanceExplainer =
        DeterministicDynamicScheduleDecisionProvenanceExplainer(),

    /**
     * 14.17：决策生命周期审计构建器（纯函数）。
     */
    private val auditBuilder:
        DynamicScheduleDecisionAuditBuilder =
        DeterministicDynamicScheduleDecisionAuditBuilder(),

    /**
     * 14.10：用户显式覆盖（只读）。
     */
    private val humanOverrideService:
        DynamicScheduleHumanOverrideService,

    /**
     * 13.14：批量重放。
     */
    private val replayBatchService:
        DynamicScheduleHistoryReplayBatchService,

    /**
     * 13.14：决策时间线（纯读取视图）。
     */
    private val timelineService:
        DynamicScheduleDecisionTimelineService,
) {

    /**
     * 13.17：全库历史完整性检查（双向引用）。
     *
     * ```kotlin
     * val report = session.checkHistoryIntegrity()
     *
     * report.checkedSnapshots / checkedReplayInputs / checkedProvenances
     * report.isHealthy
     * ```
     *
     * 与按 item 的 [checkHistoryIntegrity] 相比，它会额外检查：
     *
     * ```text
     * ReplayInput → Snapshot 还在吗？
     * Provenance  → Snapshot / ReplayInput 还在吗？
     * ```
     *
     * 只检测、只报告，**绝不自动修复**——
     * 检测算法一旦有 bug，自动修复会把历史二次破坏。
     */
    suspend fun checkHistoryIntegrity(
        checkedAt: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleHistoryIntegrityReport =
        historyIntegrity.checkAll(checkedAt)

    /**
     * 14.1：从历史中提取这个 item 的决策特征。
     *
     * ```kotlin
     * val features = session.decisionFeatures(itemId)
     *
     * features.value("completion_rate")
     * features.confidence("completion_rate")
     * ```
     *
     * **只读**：它不修改任何历史，也**不修改任何 Weight**。
     *
     * ```text
     * History → Features         ← 14.1 只到这里
     * Features → Dynamic Weight  ← 14.2 之后才研究
     * ```
     *
     * 因为 `completion_rate = 0.9` 本身推不出"应该提高 priority"——
     * 也可能只是"任务本来就简单"。测量与解释必须分开。
     */
    suspend fun decisionFeatures(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleDecisionFeatureSet =
        featureExtractor.extract(
            itemId = itemId,
            limit = limit,
            now = now,
        )

    /**
     * 14.2：把历史特征解释成**语义倾向**。
     *
     * ```kotlin
     * val interpretation = session.decisionInterpretation(itemId)
     *
     * interpretation.contains(COMPLETION_TENDENCY)
     * interpretation.strength(HIGH_DECISION_STABILITY)
     * ```
     *
     * 它**不修改** Priority / Attention / Schedule，
     * 也不提供任何"综合评分"——
     *
     * ```text
     * completion_rate  → COMPLETION_TENDENCY
     * stability        → HIGH/LOW_DECISION_STABILITY
     * ```
     *
     * 保持"一个特征对一个语义"，14.3 才能明确决定
     * 哪些倾向走向哪些适应策略。
     */
    suspend fun decisionInterpretation(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleDecisionInterpretation =
        interpretationService.interpret(
            featureExtractor.extract(
                itemId = itemId,
                limit = limit,
                now = now,
            ),
        )

    /**
     * 14.3：把这个 item 的历史行为整理成一个 Behavior Model。
     *
     * ```text
     * History → Feature → Interpretation → Behavioral Model
     * ```
     *
     * 模型只回答"过去表现出了什么稳定行为"，
     * 并**不**表示 Priority / Attention 应该如何变化。
     *
     * ```kotlin
     * val model = session.behavioralAdaptation(itemId)
     *
     * model.completionTendency
     * model.decisionStability
     * model.isReliable     // 有证据且置信度足够
     * ```
     */
    suspend fun behavioralAdaptation(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleBehavioralAdaptation =
        behavioralAdaptationService.build(
            itemId = itemId,

            interpretation =
                decisionInterpretation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                ),
        )

    /**
     * 14.3：一次为多个 item 建立 Behavior Model。
     *
     * 结果按 itemId 排序，保证相同输入得到相同顺序——
     * 这是后面做指纹 / Replay / Provenance 的前提。
     */
    suspend fun behavioralAdaptations(
        itemIds: Collection<DynamicScheduleItemId>,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleBehavioralAdaptationSet =
        DynamicScheduleBehavioralAdaptationSetBuilder()
            .build(
                interpretations =
                    itemIds
                        .distinct()
                        .associateWith { itemId ->
                            decisionInterpretation(
                                itemId = itemId,
                                limit = limit,
                                now = now,
                            )
                        },
            )

    /**
     * 14.4：把这个 item 的行为模型转换成**动态权重偏移**。
     *
     * ```text
     * History → Feature → Interpretation → Behavioral Model → Weight Delta
     * ```
     *
     * ### 它不是新 Priority
     *
     * ```kotlin
     * val adaptation = session.weightAdaptation(itemId)
     *
     * adaptation.delta(COMPLETION)   // +0.13
     * adaptation.enabled
     * ```
     *
     * `+0.13` 的含义是"完成行为这个因素应该获得更多影响力"，
     * 而**不是**"这条任务的优先级 +0.13"。
     *
     * ### enabled == false 时 deltas 为空
     *
     * 样本不足或置信度不足时不会返回"部分生效"的偏移，
     * 而是直接给出空 deltas——
     * 半个权重比没有权重更难排查。
     *
     * 真正改变用户看到什么发生在 14.5+，落盘发生在 14.11。
     */
    suspend fun weightAdaptation(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        policy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),
    ): DynamicScheduleWeightAdaptation =
        weightAdaptationService.adapt(
            behavioral =
                behavioralAdaptation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                ),
            policy = policy,
        )

    /**
     * 14.4：一次为多个 item 计算权重偏移。
     *
     * 结果按 itemId 排序，保证相同输入得到相同顺序。
     */
    suspend fun weightAdaptations(
        itemIds: Collection<DynamicScheduleItemId>,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        policy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),
    ): DynamicScheduleWeightAdaptationSet =
        weightAdaptationService.adapt(
            behavioralModels =
                behavioralAdaptations(
                    itemIds = itemIds,
                    limit = limit,
                    now = now,
                ).adaptations,
            policy = policy,
        )

    /**
     * 14.5：把权重偏移进一步翻译成 **Attention 影响量**。
     *
     * ```text
     * History → Feature → Interpretation → Behavioral Model
     *        → Weight Delta → Attention Delta
     * ```
     *
     * ```kotlin
     * val attention = session.attentionAdaptation(itemId)
     *
     * attention.attentionDelta   // 更容易 / 更不容易进入 Attention
     * attention.enabled
     * ```
     *
     * ### 它仍然不改任何东西
     *
     * 这里**没有**调用 AttentionRuntime。
     * Runtime 要拿到的只是：
     *
     * ```kotlin
     * val influence =
     *     DynamicScheduleAttentionInfluence.from(attention)
     * ```
     *
     * 真正让 Runtime 用上它是 14.6。
     *
     * ### 为什么 14.5 单独存在
     *
     * 因为"权重应该变"和"该不该更频繁提醒用户"是两件事：
     *
     * ```text
     * 经常完成  → 权重上去，但默认**不**提高 Attention
     * ```
     */
    suspend fun attentionAdaptation(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        attentionPolicy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): DynamicScheduleAttentionAdaptation =
        attentionAdaptationService.adapt(
            behavioral =
                behavioralAdaptation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                ),
            weightAdaptation =
                weightAdaptation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                    policy = weightPolicy,
                ),
            policy = attentionPolicy,
        )

    /**
     * 14.5：一次为多个 item 计算 Attention 影响。
     */
    suspend fun attentionAdaptations(
        itemIds: Collection<DynamicScheduleItemId>,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        attentionPolicy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): List<DynamicScheduleAttentionAdaptation> {

        val distinct = itemIds.distinct()

        return attentionAdaptationService.adapt(
            behavioralModels =
                behavioralAdaptations(
                    itemIds = distinct,
                    limit = limit,
                    now = now,
                ).adaptations,
            weightAdaptations =
                weightAdaptations(
                    itemIds = distinct,
                    limit = limit,
                    now = now,
                    policy = weightPolicy,
                ).adaptations,
            policy = attentionPolicy,
        )
    }

    /**
     * 14.5：Runtime 边界对象。
     *
     * ```kotlin
     * val influence = session.attentionInfluence(itemId)
     * ```
     *
     * 只包含 `delta` / `confidence` / `enabled`，
     * 不带任何历史模型细节。
     */
    suspend fun attentionInfluence(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        attentionPolicy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): DynamicScheduleAttentionInfluence =
        DynamicScheduleAttentionInfluence.from(
            attentionAdaptation(
                itemId = itemId,
                limit = limit,
                now = now,
                weightPolicy = weightPolicy,
                attentionPolicy = attentionPolicy,
            ),
        )

    /**
     * 14.6：把行为模型翻译成 **Schedule Priority 影响量**。
     *
     * ```text
     * History → Feature → Interpretation → Behavioral Model
     *        → Weight Delta → Priority Delta
     * ```
     *
     * ```kotlin
     * val priority = session.behavioralPriorityAdaptation(itemId)
     *
     * priority.priorityDelta   // 倾向提高 / 降低
     * priority.enabled
     * ```
     *
     * ### 它不是 12.x 的 Priority Adaptation
     *
     * 12.x 的 `DynamicSchedulePriorityAdaptation` 是
     * Attention 历史 → 倍率的通道，被 14.7 消费。
     * 这里产出的是 14.x 行为链的偏移量，两者并存、互不覆盖。
     *
     * ### 它不改 `DynamicScheduleItem.priority`
     *
     * 这里只给出"如果允许使用这份适配，Priority 能获得多大影响"。
     * 真正落到排序仍然是后续阶段的事。
     */
    suspend fun behavioralPriorityAdaptation(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        priorityPolicy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): DynamicScheduleBehavioralPriorityAdaptation =
        behavioralPriorityAdaptationService.adapt(
            behavioral =
                behavioralAdaptation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                ),
            weightAdaptation =
                weightAdaptation(
                    itemId = itemId,
                    limit = limit,
                    now = now,
                    policy = weightPolicy,
                ),
            policy = priorityPolicy,
        )

    /**
     * 14.6：一次为多个 item 计算 Priority 影响。
     */
    suspend fun behavioralPriorityAdaptations(
        itemIds: Collection<DynamicScheduleItemId>,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        priorityPolicy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): List<DynamicScheduleBehavioralPriorityAdaptation> {

        val distinct = itemIds.distinct()

        return behavioralPriorityAdaptationService.adapt(
            behavioralModels =
                behavioralAdaptations(
                    itemIds = distinct,
                    limit = limit,
                    now = now,
                ).adaptations,
            weightAdaptations =
                weightAdaptations(
                    itemIds = distinct,
                    limit = limit,
                    now = now,
                    policy = weightPolicy,
                ).adaptations,
            policy = priorityPolicy,
        )
    }

    /**
     * 14.6：Decision Engine 边界对象。
     */
    suspend fun behavioralPriorityInfluence(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        now: Instant =
            kotlin.time.Clock.System.now(),

        weightPolicy: DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),

        priorityPolicy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): DynamicScheduleBehavioralPriorityInfluence =
        DynamicScheduleBehavioralPriorityInfluence.from(
            behavioralPriorityAdaptation(
                itemId = itemId,
                limit = limit,
                now = now,
                weightPolicy = weightPolicy,
                priorityPolicy = priorityPolicy,
            ),
        )

    /**
     * 14.7：把**当前 Context** 翻译成 Priority 影响。
     *
     * ```text
     * Context Match → Context Priority Influence
     * ```
     *
     * ```kotlin
     * val influences = session.contextPriorityInfluences(
     *     model = model,
     *     activeContextKeys = setOf("DORM"),
     *     contextBindings = bindings,
     * )
     *
     * influences.first().contextFactor   // 1.0 = 无影响
     * ```
     *
     * ### Context 不是 BLOCKED
     *
     * `excluded` 或 REQUIRED 缺失只**降低 Priority**，
     * 状态投影仍由 ContextBinding 层决定。
     */
    fun contextPriorityInfluences(
        model: DynamicScheduleModel,

        activeContextKeys: Set<String> = emptySet(),

        contextBindings:
            DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),

        policy: DynamicScheduleContextPriorityPolicy =
            DynamicScheduleContextPriorityPolicy(),
    ): List<DynamicScheduleContextPriorityInfluence> =
        contextPriorityService.adapt(
            model = model,
            matches =
                contextBinder
                    .bind(
                        DynamicScheduleContextBindingInput(
                            model = model,
                            bindings = contextBindings,
                            activeContextKeys =
                                activeContextKeys,
                        ),
                    )
                    .matches,
            policy = policy,
        )

    /**
     * 14.7：把历史 Priority Adaptation 与当前 Context 合并成
     * **当前有效 Priority**。
     *
     * ```text
     * historical effectivePriority × contextFactor
     *     → effectivePriority
     * ```
     *
     * 这是用户第一次真正能看到"为什么这条排在这里"的地方，
     * 所以结果把两个因子分开保留：
     *
     * ```text
     * historicalFactor   历史原因
     * contextFactor      当前场景
     * ```
     *
     * ### 仍然不写回 Schedule
     *
     * 返回的是视图对象，不会修改
     * `DynamicScheduleItem.priority`。
     */
    fun contextSensitivePriorities(
        model: DynamicScheduleModel,

        historicalAdaptations:
            List<DynamicSchedulePriorityAdaptation>,

        activeContextKeys: Set<String> = emptySet(),

        contextBindings:
            DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),

        policy: DynamicScheduleContextPriorityPolicy =
            DynamicScheduleContextPriorityPolicy(),
    ): List<DynamicScheduleContextSensitivePriority> =
        contextSensitivePriorityService.combine(
            adaptations = historicalAdaptations,
            contextInfluences =
                contextPriorityInfluences(
                    model = model,
                    activeContextKeys = activeContextKeys,
                    contextBindings = contextBindings,
                    policy = policy,
                ),
        )

    /**
     * 14.8：这次自适应**有多可信**。
     *
     * ```text
     * 历史证据 + 当前 Context + 行为稳定性 + 样本量
     *     ↓ 加权
     * Adaptation Confidence
     * ```
     *
     * ```kotlin
     * val confidences = session.adaptationConfidence(
     *     model = model,
     *     historicalAdaptations = adaptations,
     *     activeContextKeys = setOf("DORM"),
     *     contextBindings = bindings,
     *     sampleCountByItem = mapOf("course-001" to 12),
     * )
     *
     * confidences.first().value     // 0.0 .. 1.0
     * confidences.first().enabled   // 是否够格进入自动决策层
     * ```
     *
     * ### Confidence ≠ Influence
     *
     * ```text
     * priorityDelta = +0.20, confidence = 0.20
     *     → "系统认为应该产生较大影响，但证据还不够可靠"
     * ```
     *
     * ### 14.8 **不做** `effectivePriority *= confidence`
     *
     * 那会把：
     *
     * ```text
     * "系统不确定"
     * ```
     *
     * 错误地解释成：
     *
     * ```text
     * "任务不重要"
     * ```
     *
     * 两个语义完全不同，所以这里只产出 confidence，
     * 由后续阶段自行决定怎么使用。
     */
    fun adaptationConfidence(
        model: DynamicScheduleModel,

        historicalAdaptations:
            List<DynamicSchedulePriorityAdaptation>,

        activeContextKeys: Set<String> = emptySet(),

        contextBindings:
            DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),

        historicalEvidenceByItem:
            Map<String, Double> = emptyMap(),

        stabilityByItem:
            Map<String, Double> = emptyMap(),

        sampleCountByItem:
            Map<String, Int> = emptyMap(),

        contextPolicy:
            DynamicScheduleContextPriorityPolicy =
            DynamicScheduleContextPriorityPolicy(),

        policy: DynamicScheduleAdaptationConfidencePolicy =
            DynamicScheduleAdaptationConfidencePolicy(),
    ): List<DynamicScheduleAdaptationConfidence> =
        adaptationConfidenceService.calculate(
            adaptations = historicalAdaptations,

            historicalEvidenceByItem =
                historicalEvidenceByItem,

            contextInfluences =
                contextPriorityInfluences(
                    model = model,
                    activeContextKeys = activeContextKeys,
                    contextBindings = contextBindings,
                    policy = contextPolicy,
                ),

            stabilityByItem = stabilityByItem,

            sampleCountByItem = sampleCountByItem,

            policy = policy,
        )

    /**
     * 14.9：把一次自适应限制进**安全边界**。
     *
     * ```text
     * requested +0.30
     *     ↓ 单次上限 0.10
     *     ↓ 累计上限 0.20
     *     ↓ 24h 内最多 3 次
     * allowed
     * ```
     *
     * Boundary 不判断"该不该调整"，它只回答：
     *
     * ```text
     * "即使允许调整，最多允许调整多少？"
     * ```
     *
     * ```kotlin
     * val bounded = session.applyAdaptationBoundary(
     *     adaptation = adaptation,
     *     confidence = confidence,
     *     state = DynamicScheduleAdaptationBoundaryState(itemId),
     *     now = now,
     * )
     *
     * bounded.adaptationFactor   // 1.0 + allowedDelta
     * bounded.enabled
     * bounded.boundary.reasons
     * ```
     *
     * ### 用户明确设置优先
     *
     * `explicitUserSetting = true` 时自动系统完全不介入。
     * 用户手动调过的优先级不应该被后台悄悄改掉。
     */
    fun applyAdaptationBoundary(
        adaptation: DynamicSchedulePriorityAdaptation,

        confidence: DynamicScheduleAdaptationConfidence,

        state: DynamicScheduleAdaptationBoundaryState,

        now: Instant,

        policy: DynamicScheduleAdaptationBoundaryPolicy =
            DynamicScheduleAdaptationBoundaryPolicy(),
    ): DynamicScheduleBoundedAdaptation =
        adaptationBoundaryService.apply(
            adaptation = adaptation,
            confidence = confidence,
            state = state,
            now = now,
            policy = policy,
        )

    /**
     * 14.10：让**用户显式设置**覆盖自动适配。
     *
     * ```text
     * 历史行为 < Context < Confidence < Boundary
     *                                    < 用户显式 override
     * ```
     *
     * ```kotlin
     * val result = session.applyHumanOverrides(
     *     adaptations = prioritized.map { it.toPriorityAdaptation() },
     *     overrides = mapOf(itemId to DynamicScheduleHumanOverride(
     *         itemId = itemId,
     *         priorityOverride = 0.9,
     *         createdAt = now,
     *         reason = "用户明确标记重要",
     *     )),
     * )
     *
     * result.overriddenItemIds   // 哪些是用户改的
     * ```
     *
     * ### Attention override 不在这一层生效
     *
     * `attentionOverride` 只写进 diagnostics，
     * 因为 [DynamicSchedulePriorityAdaptation] 里没有它的位置。
     * 硬塞进去会让"priority"这个字段承担两种语义。
     *
     * ### 用户设置优先是**产品决定**，不只是技术决定
     *
     * 后台自动调过的优先级不能被用户手动改回后又被悄悄调走，
     * 否则用户会认为设置不生效。
     */
    fun applyHumanOverrides(
        adaptations:
            List<DynamicSchedulePriorityAdaptation>,

        overrides:
            Map<
                DynamicScheduleItemId,
                DynamicScheduleHumanOverride,
                > = emptyMap(),

        policy: DynamicScheduleHumanOverridePolicy =
            DynamicScheduleHumanOverridePolicy(),
    ): DynamicScheduleHumanOverrideResult =
        humanOverrideService.apply(
            input =
                DynamicScheduleHumanOverrideInput(
                    adaptations = adaptations,
                    overrides = overrides,
                ),
            policy = policy,
        )

    /**
     * 14.11：把一次适配结果**落盘**。
     *
     * ```kotlin
     * session.persistAdaptation(
     *     adaptation = adaptation,
     *     humanOverride = override,
     * )
     *
     * // 重启之后
     * val record = session.restoreAdaptation(itemId)
     * record.adaptation.effectivePriority
     * record.humanOverride          // 仍然分得清是不是用户设的
     * ```
     *
     * ### 它保存的是"决策状态"，不只是最终 priority
     *
     * 只存 `effectivePriority` 的话，重启后无法区分：
     *
     * ```text
     * "自动算出来的 0.9"
     * "用户手动设的 0.9"
     * ```
     *
     * 而这两者在产品上必须表现不同。
     */
    suspend fun persistAdaptation(
        adaptation: DynamicSchedulePriorityAdaptation,

        humanOverride: DynamicScheduleHumanOverride? = null,
    ): DynamicScheduleAdaptationPersistenceRecord =
        adaptationPersistenceService.persist(
            adaptation = adaptation,
            humanOverride = humanOverride,
        )

    /**
     * 14.11：读回某个 item 上一次落盘的适配状态。
     */
    suspend fun restoreAdaptation(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord? =
        adaptationPersistenceService.restore(itemId)

    /**
     * 14.12：保存一次适配的**完整输入**。
     *
     * ```text
     * DynamicScheduleModel
     * + Attention History
     * + Priority Adaptation Policy
     * ```
     *
     * 三者缺一不可。特别是 policy：
     *
     * ```text
     * 历史 factor = 1.25
     * 重放 factor = 1.10
     * ```
     *
     * 如果没存 policy，这个差异看起来像算法坏了，
     * 实际上只是 policy 被改过。
     */
    suspend fun saveAdaptationReplayInput(
        itemId: DynamicScheduleItemId,

        input: DynamicScheduleAdaptationReplayInput,
    ) {
        adaptationReplayInputStore.save(
            itemId = itemId,
            input = input,
        )
    }

    /**
     * 14.12：读回某个 item 的历史重放输入。
     */
    suspend fun restoreAdaptationReplayInput(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayInput? =
        adaptationReplayInputStore.get(itemId)

    /**
     * 14.12：按历史输入重新跑一次适配，并给出差异。
     *
     * ```kotlin
     * val result = session.replayAdaptation(itemId)
     *
     * result.equivalent     // 是否完全一致
     * result.differences    // 哪里变了
     * ```
     *
     * ### 缺数据时抛错，而不是返回"无差异"
     *
     * 返回"没有差异"会让调用方以为算法稳定，
     * 而实际上是历史不完整。
     */
    suspend fun replayAdaptation(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationReplayResult =
        adaptationReplayService.replay(itemId)

    /**
     * 14.13：重放之后，判断"这次差异到底意味着什么"。
     *
     * ```kotlin
     * val report = session.verifyAdaptation(
     *     itemId = itemId,
     *     replayEngineVersion = session.engineRegistry.current.version,
     * )
     *
     * report.status      // EXACT_MATCH / INPUT_CHANGED / ...
     * report.verified    // 只有 EXACT_MATCH 才是 true
     * ```
     *
     * ### 为什么要传"当前 policy"
     *
     * 历史输入里存的是**当时**的 policy。
     * 如果调用方不告诉这里"现在的 policy 是什么"，
     * 系统就只能比较历史 policy 和自己，
     * 永远不可能得出 `POLICY_CHANGED`。
     *
     * ```text
     * currentPolicy = null  → 用历史 policy（等价于"没改过"）
     * currentPolicy = 别的值 → 触发 POLICY_CHANGED
     * ```
     *
     * ### 分类阶梯（14.13 §十）
     *
     * ```text
     * Input 变了？   → INPUT_CHANGED
     * Policy 变了？  → POLICY_CHANGED
     * Engine 变了？  → ENGINE_CHANGED
     * 结果不一致？   → BEHAVIOR_CHANGED
     * 否则          → EXACT_MATCH
     * ```
     *
     * 顺序不能换：输入或参数变了的时候，
     * 把结果差异报成 `BEHAVIOR_CHANGED` 会把
     * "有人调了参数"说成"算法退化"。
     */
    suspend fun verifyAdaptation(
        itemId: DynamicScheduleItemId,

        historicalEngineVersion: String,

        replayEngineVersion: String,

        currentPolicy:
            DynamicSchedulePriorityAdaptationPolicy? = null,

        policy: DynamicScheduleAdaptationVerificationPolicy =
            DynamicScheduleAdaptationVerificationPolicy(),
    ): DynamicScheduleAdaptationVerificationReport {

        val record =
            adaptationStore.get(itemId)
                ?: error(
                    "Historical adaptation not found: $itemId",
                )

        val input =
            adaptationReplayInputStore.get(itemId)
                ?: error(
                    "Historical adaptation replay input " +
                        "not found: $itemId",
                )

        val replayed =
            adaptationReplayService.replay(itemId)

        return adaptationVerificationEngine.verify(
            historical = listOf(record.adaptation),

            replayed = replayed.replayed,

            historicalEngineVersion =
                historicalEngineVersion,

            replayEngineVersion = replayEngineVersion,

            /*
             * 历史 policy 指纹来自"当时真正用过的 policy"。
             */
            historicalPolicyFingerprint =
                DynamicScheduleAdaptationInputFingerprint
                    .ofPolicy(input.policy)
                    .value,

            replayPolicyFingerprint =
                DynamicScheduleAdaptationInputFingerprint
                    .ofPolicy(
                        currentPolicy ?: input.policy,
                    )
                    .value,

            policy = policy,

            historicalInputFingerprint =
                DynamicScheduleAdaptationInputFingerprint
                    .from(input)
                    .value,

            /*
             * 重放用的就是存下来的输入，
             * 所以输入指纹天然一致；
             * 调用方若要验证输入漂移，应直接调用 Engine。
             */
            replayInputFingerprint =
                DynamicScheduleAdaptationInputFingerprint
                    .from(input)
                    .value,
        )
    }

    /**
     * 14.15：构建一次决策的**可追溯视图**。
     *
     * ```text
     * INPUT
     *   └── SNAPSHOT
     *         ├── DEPENDENCY
     *         ├── CONTEXT
     *         ├── ATTENTION_MEMORY
     *         ├── TRACE
     *         ├── PRIORITY_ADAPTATION
     *         ├── HUMAN_OVERRIDE
     *         └── OUTPUT
     * ```
     *
     * ```kotlin
     * val provenance = session.provenance(
     *     itemId = itemId,
     *     generatedAt = now,
     * )
     *
     * provenance?.completeness     // COMPLETE / PARTIAL / EMPTY
     * provenance?.ancestorsOf(provenance.outputNodeId)
     * ```
     *
     * ### 它是派生数据，不单独持久化
     *
     * Provenance 由 Snapshot + Trace + Adaptation 现算出来。
     * 再存一份就等于多了一份可能过期的真相。
     *
     * ### `generatedAt` 由调用方传入
     *
     * 与 13.15 的时钟纪律一致：这里不在内部读系统时间，
     * 同样的历史数据配同样的 `generatedAt` 会得到同样的图。
     *
     * ### 不会伪造节点
     *
     * 没有 Trace 就没有 TRACE 节点，没有 override 就没有
     * HUMAN_OVERRIDE 节点，并如实降低 `completeness`。
     */
    suspend fun provenance(
        itemId: DynamicScheduleItemId,

        generatedAt: Instant,

        replayed: Boolean = false,

        verificationStatus: String? = null,
    ): AdaptiveDecisionProvenance? {

        val snapshot =
            snapshotPersistence.latest(itemId)
                ?: return null

        return provenanceForSnapshot(
            snapshot = snapshot,
            generatedAt = generatedAt,
            replayed = replayed,
            verificationStatus = verificationStatus,
        )
    }

    /**
     * 14.15：按 Snapshot ID 构建 Provenance。
     *
     * 与按 itemId 的版本的区别只有"取哪一次快照"：
     * 这里不再查最新，而是直接指向历史中的某一次决策。
     */
    suspend fun provenance(
        snapshotId: DynamicScheduleDecisionSnapshotId,

        generatedAt: Instant,

        replayed: Boolean = false,

        verificationStatus: String? = null,
    ): AdaptiveDecisionProvenance? {

        val snapshot =
            snapshotPersistence.find(snapshotId)
                ?: return null

        return provenanceForSnapshot(
            snapshot = snapshot,
            generatedAt = generatedAt,
            replayed = replayed,
            verificationStatus = verificationStatus,
        )
    }

    private suspend fun provenanceForSnapshot(
        snapshot: DynamicScheduleDecisionSnapshot,

        generatedAt: Instant,

        replayed: Boolean,

        verificationStatus: String?,
    ): AdaptiveDecisionProvenance {

        val trace =
            snapshot.traceId?.let {
                tracePersistence.find(it)
            }

        /*
         * 14.11 之后，适配结果已经有了自己的持久化。
         * 所以这里不再像 14.15 规格里那样传 emptyList()，
         * 而是真的把当时那份记录读出来。
         *
         * 读不到就如实为空——provenance 会因此降级为 PARTIAL，
         * 而不是伪造一个 adaptation。
         */
        val record =
            adaptationStore.get(snapshot.itemId)

        return provenanceBuilder.build(
            DynamicScheduleDecisionProvenanceInput(
                snapshot = snapshot,

                trace = trace,

                adaptations =
                    listOfNotNull(record?.adaptation),

                generatedAt = generatedAt,

                engineVersion =
                    engineRegistry.current.version,

                humanOverride =
                    record?.humanOverride,

                replayed = replayed,

                verificationStatus =
                    verificationStatus,
            ),
        )
    }

    /**
     * 14.16：给人看的解释。
     *
     * ```kotlin
     * val explanation = session.explainProvenance(itemId, now)
     *
     * explanation?.summary           // 一句话
     * explanation?.decisiveFactors   // 决定性因素列表
     * ```
     *
     * 产品层不需要接触 Snapshot / Trace / ProvenanceNode。
     *
     * ### 它会如实说明"这不是完整解释"
     *
     * Provenance 缺 Trace 或 Adaptation 时，
     * summary 会附上"当前 Provenance 仍然是部分完整的"。
     *
     * 解释最危险的地方是过度自信：
     * 只说"包含 1 个决定性因素"会让用户以为原因就这一个，
     * 而真实情况可能只是"我们只看到了一部分"。
     */
    suspend fun explainProvenance(
        itemId: DynamicScheduleItemId,

        generatedAt: Instant,

        replayed: Boolean = false,

        verificationStatus: String? = null,
    ): DynamicScheduleDecisionProvenanceExplanation? {

        val provenance =
            provenance(
                itemId = itemId,
                generatedAt = generatedAt,
                replayed = replayed,
                verificationStatus = verificationStatus,
            )
                ?: return null

        return provenanceExplainer.explain(provenance)
    }

    /**
     * 14.16：结构化查询。
     *
     * ```kotlin
     * val result = session.queryProvenance(
     *     DynamicScheduleDecisionProvenanceQuery(
     *         itemId = itemId,
     *         includeNonDecisive = false,
     *     ),
     *     generatedAt = now,
     * )
     *
     * result.found
     * result.nodes        // 只要 decisive
     * result.provenance   // 仍然是完整图
     * ```
     *
     * 与 [explainProvenance] 的分工：
     *
     * ```text
     * queryProvenance    → 机器可读的节点投影
     * explainProvenance  → 人可读的一句话
     * ```
     */
    suspend fun queryProvenance(
        query: DynamicScheduleDecisionProvenanceQuery,

        generatedAt: Instant,

        replayed: Boolean = false,

        verificationStatus: String? = null,
    ): DynamicScheduleDecisionProvenanceQueryResult {

        val provenance =
            when {

                query.itemId != null ->
                    provenance(
                        itemId = query.itemId,
                        generatedAt = generatedAt,
                        replayed = replayed,
                        verificationStatus = verificationStatus,
                    )

                query.snapshotId != null -> {

                    val snapshot =
                        snapshotPersistence.find(
                            query.snapshotId,
                        )

                    snapshot?.let {
                        provenanceForSnapshot(
                            snapshot = it,
                            generatedAt = generatedAt,
                            replayed = replayed,
                            verificationStatus =
                                verificationStatus,
                        )
                    }
                }

                else ->
                    null
            }

        if (provenance == null) {
            return DynamicScheduleDecisionProvenanceQueryResult(
                provenance = null,
                diagnostics =
                    listOf(
                        "No decision provenance was found.",
                    ),
            )
        }

        return DynamicScheduleDecisionProvenanceQueryResult(
            provenance = provenance,

            nodes =
                if (query.includeNonDecisive) {
                    provenance.nodes
                } else {
                    provenance.decisiveNodes
                },

            diagnostics =
                if (query.includeDiagnostics) {
                    provenance.diagnostics
                } else {
                    emptyList()
                },
        )
    }

    /**
     * 14.17：一次决策的**生命周期审计**。
     *
     * ```text
     * 10:00 CREATED
     * 10:00 SNAPSHOT_PERSISTED
     * 10:00 TRACE_PERSISTED
     * 10:30 REPLAYED
     * 10:31 VERIFIED
     * 11:00 SUPERSEDED
     * ```
     *
     * ### Audit ≠ Event Log
     *
     * 项目里已经有 TransitionLog（世界状态）与 DecisionTrace
     * （一次执行）。Audit 回答的是第三个问题：
     *
     * ```text
     * 这个决策在它的生命周期里发生了什么？
     * ```
     *
     * 所以它**不复制** Runtime 的每一步。
     *
     * ### SUPERSEDED 是它真正的价值
     *
     * 用户问"为什么昨天这么排、今天变了"时，
     * 答案不该是"系统算错了"，而应该是：
     *
     * ```text
     * Decision A（0.50）
     *     ↓ 新的 Evidence / Context / Memory
     * Decision B（0.70）
     *     ↓
     * A 被 B supersede
     * ```
     *
     * ### 它是派生的，暂不落盘（§十）
     *
     * 与 Provenance 同理：Audit 由已有证据现算，
     * 现在不做 SQLDelight。
     */
    suspend fun audit(
        itemId: DynamicScheduleItemId,

        generatedAt: Instant,
    ): DynamicScheduleDecisionAudit? {

        val snapshot =
            snapshotPersistence.latest(itemId)
                ?: return null

        return auditForSnapshot(
            snapshot = snapshot,
            generatedAt = generatedAt,
        )
    }

    /**
     * 14.17：按 Snapshot ID 审计**某一次**历史决策。
     *
     * 这里才是 SUPERSEDED 真正会出现的地方：
     * 如果同一个 item 在这之后又产生了新的 Snapshot，
     * 这次决策的状态就是 SUPERSEDED。
     */
    suspend fun audit(
        snapshotId: DynamicScheduleDecisionSnapshotId,

        generatedAt: Instant,
    ): DynamicScheduleDecisionAudit? {

        val snapshot =
            snapshotPersistence.find(snapshotId)
                ?: return null

        return auditForSnapshot(
            snapshot = snapshot,
            generatedAt = generatedAt,
        )
    }

    private suspend fun auditForSnapshot(
        snapshot: DynamicScheduleDecisionSnapshot,

        generatedAt: Instant,
    ): DynamicScheduleDecisionAudit {

        val trace =
            snapshot.traceId?.let {
                tracePersistence.find(it)
            }

        val record =
            adaptationStore.get(snapshot.itemId)

        /*
         * 只有真的存在一个更晚的 Snapshot 时才算被取代。
         */
        val supersededBy =
            snapshotPersistence
                .query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = snapshot.itemId,
                    ),
                )
                .snapshots
                .firstOrNull {
                    it.capturedAt > snapshot.capturedAt
                }
                ?.id

        return auditBuilder.build(
            snapshot = snapshot,

            trace = trace,

            generatedAt = generatedAt,

            adaptationPersisted = record != null,

            replayed = false,

            verificationStatus = null,

            supersededBy = supersededBy,
        )
    }

    /**
     * 13.14：某个 item 的决策时间线。
     *
     * **纯历史视图**：只读 Snapshot + Trace，
     * 不会触发任何 Runtime 计算。用户打开"历史记录"是安全的。
     *
     * ```kotlin
     * val timeline = session.decisionTimeline(itemId)
     *
     * timeline.latest          // 最新一次决策
     * timeline.earliest        // 最早一次
     * timeline.at(instant)     // "那个时刻当时最近的一次决策"
     * timeline.before(instant)
     * timeline.after(instant)
     * ```
     */
    suspend fun decisionTimeline(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        includeTrace: Boolean = true,
    ): DynamicScheduleDecisionTimeline =
        timelineService.timeline(
            itemId = itemId,
            limit = limit,
            includeTrace = includeTrace,
        )

    /**
     * 13.14：批量重放若干历史决策。
     *
     * 这是**计算操作**，不是查询。
     *
     * ```kotlin
     * val timeline = session.decisionTimeline(itemId, limit = 10)
     *
     * val result = session.replayHistory(
     *     snapshotIds = timeline.entries.map { it.snapshotId },
     *     policy = DynamicScheduleHistoryReplayBatchPolicy(verify = true),
     * )
     *
     * result.succeeded / result.failed / result.total
     * ```
     *
     * 它**不会**删除任何历史——生命周期管理仍然只属于
     * `compactHistory()`。
     */
    suspend fun replayHistory(
        snapshotIds: List<DynamicScheduleDecisionSnapshotId>,

        policy: DynamicScheduleHistoryReplayBatchPolicy =
            DynamicScheduleHistoryReplayBatchPolicy(),

        startedAt: Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleHistoryReplayBatchResult =
        replayBatchService.replay(
            snapshotIds = snapshotIds,
            policy = policy,
            startedAt = startedAt,
        )

    /**
     * 13.13：某个 item 的统一历史决策查询。
     *
     * ```kotlin
     * val history = session.decisionHistory(itemId)
     *
     * history.entries      // 最新在前
     * history.latest       // entries[0]
     * ```
     *
     * 默认只读 Snapshot + Trace + 已有来源链，
     * **不会**重新执行 Runtime。需要重放时显式打开：
     *
     * ```kotlin
     * session.decisionHistory(
     *     itemId = itemId,
     *     query = DynamicScheduleDecisionHistoryQuery(
     *         includeReplay = true,
     *         includeVerification = true,
     *     ),
     * )
     * ```
     *
     * 注意 `includeReplay = true` 是**昂贵**操作：
     * N 条历史就是 N 次重放。
     */
    suspend fun decisionHistory(
        itemId: DynamicScheduleItemId,

        query: DynamicScheduleDecisionHistoryQuery =
            DynamicScheduleDecisionHistoryQuery(),
    ): DynamicScheduleDecisionHistoryResult =
        historyService.query(
            query.copy(itemId = itemId),
        )

    /**
     * 13.13：某个 item 最近一次决策的统一视图。
     *
     * 走的是 `LIMIT 1` 的启动恢复路径，不会加载整条历史。
     */
    suspend fun latestHistory(
        itemId: DynamicScheduleItemId,

        includeTrace: Boolean = true,
    ): DynamicScheduleDecisionHistoryEntry? =
        historyService
            .query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = 1,
                    includeTrace = includeTrace,
                ),
            )
            .entries
            .firstOrNull()

    /**
     * 13.12：某条历史决策最近一次重放的来源链。
     *
     * App 拿到它可以渲染整条链路，而不用自己拼
     * Snapshot / Trace / ReplayInput / Compatibility / Verification：
     *
     * ```text
     * 历史引擎：12.27
     * 当前引擎：13.11
     * 兼容状态：REPLAY_COMPATIBLE
     * 迁移：无
     * Replay：已完成
     * Verification：EXACT_MATCH
     * ```
     */
    suspend fun provenanceFor(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionProvenance? =
        provenanceStore.latestForSnapshot(snapshotId)

    /**
     * 13.12：某个 item 的完整来源链时间线（按时间正序）。
     */
    suspend fun provenanceForItem(
        itemId: DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionProvenance> =
        provenanceStore.queryForItem(itemId)

    /**
     * 11.x External Schedule → 12.2 投影后的 Schedule Model。
     */
    fun project(
        schedule: ExternalSchedule,
        now: Instant,
    ): DynamicScheduleStateProjectionResult {

        val model =
            mapper.map(
                schedule = schedule,
                mappedAt = now,
            )

        return stateProjectionService.project(
            model = model,
            evaluatedAt = now,
        )
    }

    /**
     * 完整决策：投影 → Runtime → 持久化 Snapshot / Trace / Replay Input。
     */
    suspend fun evaluate(
        model: DynamicScheduleModel,

        now: Instant,

        activeContextKeys: Set<String> = emptySet(),

        contextBindings:
            DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),

        dependencies:
            DynamicScheduleDependencySet =
            DynamicScheduleDependencySet(),

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ): DynamicScheduleDecisionRuntimeEvaluation {

        return facade.evaluateAndPersist(
            input =
                DynamicScheduleIntelligenceInput(
                    model = model,
                    dependencies = dependencies,
                    contextBindings = contextBindings,
                    activeContextKeys = activeContextKeys,
                ),

            previous = previous,

            now = now,

            policy = policy,

            engineVersion = engineRegistry.current.version,
        )
    }

    /**
     * 16.20：**只评估，不持久化**。
     *
     * 与 [evaluate] 跑的是同一条 Runtime 链路，区别只有一处：
     * 不写 Snapshot / Trace / ReplayInput。
     *
     * ```kotlin
     * session.preview(model, now)   // 纯读
     * session.evaluate(model, now)  // 读 + 记录历史
     * ```
     *
     * 产品层展示"当前决策"时应该用这个，
     * 否则每打开一次界面都会新增一条历史记录。
     */
    suspend fun preview(
        model: DynamicScheduleModel,

        now: Instant,

        activeContextKeys: Set<String> = emptySet(),

        contextBindings:
            DynamicScheduleContextBindingSet =
            DynamicScheduleContextBindingSet(),

        dependencies:
            DynamicScheduleDependencySet =
            DynamicScheduleDependencySet(),

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ): DynamicScheduleIntelligenceResult {

        return facade.preview(
            input =
                DynamicScheduleIntelligenceInput(
                    model = model,
                    dependencies = dependencies,
                    contextBindings = contextBindings,
                    activeContextKeys = activeContextKeys,
                ),

            previous = previous,

            now = now,

            policy = policy,
        )
    }

    suspend fun latestSnapshotId(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshotId? =
        snapshotPersistence
            .latest(itemId)
            ?.id

    /**
     * 历史决策查询（12.15）。
     *
     * 产品化需要"我这一天被提醒过几次、为什么"，
     * 这条路径直接落在持久化层上，不依赖内存态。
     */
    suspend fun history(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult =
        snapshotPersistence.query(query)

    /**
     * 历史决策链路查询（12.15 Trace）。
     */
    suspend fun historyTraces(
        query: DynamicScheduleDecisionTraceQuery,
    ): DynamicScheduleDecisionTraceQueryResult =
        tracePersistence.query(query)

    /**
     * 启动恢复：把某个 item 最近一次决策读回来。
     *
     * 重启后不需要重新跑一遍 Runtime，
     * 就能回答"当前该提醒什么、为什么"。
     */
    suspend fun latestDecision(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? =
        snapshotPersistence.latest(itemId)

    /**
     * 16.32：[latestDecision] 的批量版本，**一次往返**。
     *
     * 用它替代"N 次 latestDecision"是这次改动的全部目的：
     * 展示层每次加载都要判断"哪些条目的决策变了"，
     * 逐条查询在 200 个条目上要 81ms，而整条评估链只要 1ms。
     */
    suspend fun latestDecisions(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot> =
        snapshotPersistence.latestForItems(itemIds)

    /**
     * 13.5：历史压缩。
     *
     * 这**不是** `clearHistory()`。
     *
     * Retention 会维护三者之间的引用关系：
     *
     * ```text
     * Snapshot ──► Trace        （无人引用才删）
     * Snapshot ──► ReplayInput  （与 Snapshot 同生共死）
     * ```
     *
     * 并且**永远不会删掉每个 item 最新的那一条 Snapshot**
     * （`policy.preserveLatest` 默认 true）。
     *
     * App 只需要：
     *
     * ```kotlin
     * session.compactHistory(
     *     itemIds = listOf(itemId),
     *     now = Clock.System.now(),
     * )
     * ```
     *
     * 而不需要自己碰三个 Store。
     */
    suspend fun compactHistory(
        itemIds: Collection<DynamicScheduleItemId>,

        now: Instant,

        policy: DynamicScheduleHistoryRetentionPolicy =
            DynamicScheduleHistoryRetentionPolicy(),
    ): DynamicScheduleHistoryCompactionBatchResult =
        historyRetention.compact(
            itemIds = itemIds,
            now = now,
            policy = policy,
        )

    /**
     * 13.7：检查某个 item 的历史是否完整。
     *
     * ```kotlin
     * val report = session.checkHistoryIntegrity(itemId)
     *
     * when {
     *     report.isValid    -> // 历史可重放、可解释
     *     report.isDegraded -> // 缺一部分，例如无法重放
     *     report.isBroken   -> // 数据自相矛盾，不可信
     * }
     * ```
     *
     * 只读：不会修改任何历史。
     */
    suspend fun checkHistoryIntegrity(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryIntegrityReport =
        historyIntegrity.check(itemId)

    /**
     * 13.7：一次检查多个 item。
     *
     * App 启动自检用这个：
     *
     * ```kotlin
     * val batch = session.checkHistoryIntegrity(itemIds)
     *
     * if (!batch.isHealthy) {
     *     // batch.degradedItems / batch.brokenItems / batch.totalIssues
     * }
     * ```
     */
    suspend fun checkHistoryIntegrity(
        itemIds: Collection<DynamicScheduleItemId>,
    ): DynamicScheduleHistoryIntegrityBatchReport =
        historyIntegrity.check(itemIds)

    /**
     * 13.8：为历史完整性检查结果生成恢复计划。
     *
     * 注意这是**规划**，不是执行：
     *
     * ```text
     * val plan = session.planHistoryRepair(itemId)
     *
     * plan.requiresAction         // 有没有问题
     * plan.requiresManualReview   // 能不能后台静默处理
     * plan.hasSafeAutomaticAction // 有没有可安全自动执行的动作
     * plan.actions                // 具体建议 + 原因
     * ```
     *
     * 结果里出现的是 `PreserveMissingTrace` 这类建议，
     * 而不是"我帮你重建了一条 Trace"——
     * 为了让状态重新变成 VALID 而修改历史，是 DISM 明确拒绝的。
     *
     * 这个方法不会写入任何数据。
     */
    suspend fun planHistoryRepair(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryRepairPlan =
        historyRepairPlanner.plan(
            historyRepairAnalyzer.analyze(itemId),
        )

    suspend fun planHistoryRepair(
        itemIds: Collection<DynamicScheduleItemId>,
    ): DynamicScheduleHistoryRepairSummary =
        DynamicScheduleHistoryRepairSummary(
            plans =
                historyRepairAnalyzer
                    .analyze(itemIds)
                    .map { analysis ->
                        historyRepairPlanner.plan(analysis)
                    },
        )

    /**
     * 13.9：在策略允许的范围内执行修复。
     *
     * ```kotlin
     * // 默认：什么都不做，只把计划原样退回
     * session.repairHistory(itemId)
     *
     * // 显式授权后，才允许清理孤儿 ReplayInput
     * session.repairHistory(
     *     itemId = itemId,
     *     policy = DynamicScheduleHistoryRepairPolicy(
     *         permission =
     *             DynamicScheduleHistoryRepairPermission.SAFE_ONLY,
     *         allowOrphanReplayInputDeletion = true,
     *     ),
     * )
     * ```
     *
     * 无论策略如何，这个入口**都不会**：
     *
     * ```text
     * 重写 Snapshot / Trace
     * 重建历史 Trace
     * 修改历史 Revision / EngineVersion
     * 伪造 ReplayInput
     * ```
     */
    suspend fun repairHistory(
        itemId: DynamicScheduleItemId,

        policy: DynamicScheduleHistoryRepairPolicy =
            DynamicScheduleHistoryRepairPolicy(),
    ): DynamicScheduleHistoryRepairResult =
        historyRepairExecutor.execute(
            itemId = itemId,

            plan =
                historyRepairPlanner.plan(
                    historyRepairAnalyzer.analyze(itemId),
                ),

            policy = policy,
        )

    /**
     * 13.10：这条历史能不能用当前引擎重放？
     *
     * UI 应该用这个，而不是去捕获 Replay 抛出的异常：
     *
     * ```kotlin
     * val diagnostic = session.versionDiagnostic(snapshotId)
     *
     * if (!diagnostic.replayAllowed) {
     *     show(diagnostic.message)
     * }
     * ```
     */
    suspend fun versionDiagnostic(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleHistoryVersionDiagnostic =
        replayService.versionDiagnostic(snapshotId)


    /**
     * Replay Input 的持久化边界。
     *
     * 暴露出来是为了让平台层能自己做保留策略
     * （例如只保留最近 N 天），而不是无限增长。
     */
    val replayInputs:
        DynamicScheduleDecisionReplayInputSnapshotPersistenceService
        get() = replayInputPersistence

    /**
     * Trace 的持久化边界。
     */
    val traces:
        DynamicScheduleDecisionTracePersistenceService
        get() = tracePersistence

    /**
     * “为什么现在提醒？”——把一次决策翻译成人类可读结论。
     */
    fun explain(
        result: DynamicScheduleIntelligenceResult,
        itemId: DynamicScheduleItemId,
    ): String {

        val explanation =
            result
                .attentionExplanations[itemId]

        val conflict =
            result
                .attentionConflicts[itemId]

        return buildString {

            appendLine(
                "item=$itemId"
            )

            appendLine(
                "priority=" +
                    (
                        conflict
                            ?.resolvedPriority
                            ?: 0.0
                        ),
            )

            appendLine(
                "attentionRequired=" +
                    (
                        conflict
                            ?.attentionRequired
                            ?: false
                        ),
            )

            appendLine(
                "hardConstraint=" +
                    (
                        conflict
                            ?.hardConstraintPresent
                            ?: false
                        ),
            )

            appendLine(
                "summary=" +
                    (
                        explanation
                            ?.summary
                            ?: "(no explanation)"
                        ),
            )

            explanation
                ?.reasons
                ?.forEach { reason ->
                    appendLine(
                        "  - " +
                            reason.type +
                            ": " +
                            reason.message,
                    )
                }

            appendLine(
                "selectedCandidates=" +
                    result
                        .attentionRuntime
                        .selection
                        .selected
                        .size,
            )
        }
    }
}
