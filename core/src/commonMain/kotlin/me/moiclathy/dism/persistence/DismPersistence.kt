package me.moiclathy.dism.persistence

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.persistence.repository.EntityRepository
import me.moiclathy.dism.persistence.repository.EvidenceRepository
import me.moiclathy.dism.persistence.repository.InMemoryEntityRepository
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.persistence.repository.InMemoryRelationStateRepository
import me.moiclathy.dism.persistence.repository.InMemoryStateCommitRepository
import me.moiclathy.dism.persistence.repository.InMemoryTransitionLogRepository
import me.moiclathy.dism.persistence.repository.RelationStateRepository
import me.moiclathy.dism.persistence.repository.StateCommitRepository
import me.moiclathy.dism.persistence.repository.TransitionLogRepository
import me.moiclathy.dism.schedule.adaptation.persistence.DynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.adaptation.persistence.InMemoryDynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.adaptation.replay.InMemoryDynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.runtime.InMemoryDynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.3
 *
 * 持久化依赖容器 —— 它**不是数据库**，
 * 而是「DISM 需要的 Persistence Ports 集合」。
 *
 * ```text
 *                        ┌──────────────────────┐
 *                        │       DISM Core      │
 *                        └──────────┬───────────┘
 *                                   │
 *                                   ▼
 *                        ┌──────────────────────┐
 *                        │  Persistence Ports   │
 *                        │  (this container)    │
 *                        └──────────┬───────────┘
 *                                   │
 *                     ┌─────────────┴─────────────┐
 *                     ▼                           ▼
 *           ┌──────────────────┐       ┌──────────────────┐
 *           │ InMemory Adapter │       │ SQLDelight       │
 *           └──────────────────┘       └──────────────────┘
 * ```
 *
 * 所以两种用法都是合法的，而 Runtime 完全不知道区别：
 *
 * ```kotlin
 * // 纯内存：测试、Demo、无数据库环境
 * val persistence = DismPersistence()
 *
 * // 真实落盘：由平台层的 DismPersistenceFactory 注入
 * val persistence = DismPersistenceFactory(driverFactory).create()
 * ```
 *
 * 不负责创建 Repository——只组装已就绪的实现，
 * 让上层（CoreBootstrapper / SemanticStateEngine）只面对容器。
 */
@DismInternalApi
class DismPersistence(
    val entities: EntityRepository =
        InMemoryEntityRepository(),

    val evidence: EvidenceRepository =
        InMemoryEvidenceRepository(),

    val relationStates: RelationStateRepository =
        InMemoryRelationStateRepository(),

    val transitionLog: TransitionLogRepository =
        InMemoryTransitionLogRepository(),

    val stateCommit: StateCommitRepository =
        InMemoryStateCommitRepository(
            relationStates,
            transitionLog,
        ),

    /**
     * 12.15 Decision Snapshot 存储。
     */
    val scheduleSnapshots:
        DynamicScheduleDecisionSnapshotStore =
        InMemoryDynamicScheduleDecisionSnapshotStore(),

    /**
     * 12.15 Decision Trace 存储。
     */
    val scheduleTraces:
        DynamicScheduleDecisionTraceStore =
        InMemoryDynamicScheduleDecisionTraceStore(),

    /**
     * 12.26A Replay Input 存储。
     */
    val scheduleReplayInputs:
        DynamicScheduleDecisionReplayInputSnapshotStore =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore(),

    /**
     * 13.12 Decision Provenance 存储。
     *
     * 与 Snapshot / Trace 不同，它**不参与 Retention 级联删除**：
     * 当旧 Snapshot 被压缩掉之后，
     * Provenance 往往是唯一还能说明"这里曾经发生过什么"的东西。
     */
    val scheduleProvenance:
        DynamicScheduleDecisionProvenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore(),

    /**
     * 13.6
     *
     * Snapshot + Trace + ReplayInput 的统一**原子**持久化边界。
     *
     * 一个 Decision 的三份投影必须一起成功或一起失败，
     * 不能出现"有 Snapshot 但没有 ReplayInput"的半截历史。
     *
     * 默认是 InMemory 实现（带 rollback 语义），
     * 平台层通过 [DismPersistenceFactory] 注入 SQLDelight 实现。
     */
    val scheduleDecisionAtomicPersistence:
        DynamicScheduleDecisionAtomicPersistence =
        InMemoryDynamicScheduleDecisionAtomicPersistence(
            snapshotStore = scheduleSnapshots,
            traceStore = scheduleTraces,
            replayInputStore = scheduleReplayInputs,
        ),

    /**
     * 14.11
     *
     * Adaptive Decision 的长期持久化。
     *
     * 保存的是整条适配记录（自动适配结果 + 用户显式覆盖），
     * 而不是只有最终 priority——否则重启后就再也分不清
     * "自动算出来的"和"用户手动设的"。
     */
    val scheduleAdaptations:
        DynamicScheduleAdaptationPersistenceStore =
        InMemoryDynamicScheduleAdaptationPersistenceStore(),

    /**
     * 14.12
     *
     * Adaptation Replay 所需的**历史输入**。
     *
     * 与 [scheduleAdaptations] 一一对应，但故意分成两张表：
     *
     * ```text
     * scheduleAdaptations            → "当时算出了什么"
     * scheduleAdaptationReplayInputs → "当时用什么算的"
     * ```
     *
     * 结果会被后续自动调整覆盖，输入必须保持原样。
     */
    val scheduleAdaptationReplayInputs:
        DynamicScheduleAdaptationReplayInputStore =
        InMemoryDynamicScheduleAdaptationReplayInputStore(),
)
