package me.moiclathy.dism.runtime

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.bootstrap.CoreBootstrapper
import me.moiclathy.dism.bootstrap.RestoredScheduleState
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.engine.DefaultDynamicScheduleEngineRegistry
import me.moiclathy.dism.schedule.engine.DynamicScheduleEngineRegistry
import me.moiclathy.dism.schedule.integration.DynamicScheduleIntegrationAssembly
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.integration.DefaultDynamicScheduleFacade
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.engine.ReactiveEvaluationResult
import me.moiclathy.dism.semantic.engine.SemanticStateEngine
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM Core Runtime —— App 面对的单实例 Facade。
 *
 * Runtime 本身就是一个已经启动的 DISM Core：
 *
 * ```text
 * DismRuntime
 *   ├── persistence  （私有，实现细节）
 *   ├── restored     （私有，启动诊断快照）
 *   └── engine       （构造时创建一次，整个生命周期复用）
 * ```
 *
 * 不再需要 App 调 restore() 才能拿到 engine，
 * 也不会每次 restore() 都重建 Engine / Bootstrapper。
 *
 * Engine 构造时已通过 stateRepository.loadAll() 恢复世界状态；
 * restored 仅作为启动诊断快照保留，不二次注入 Engine
 * （避免内存态与 repository 成为两份真相）。
 *
 * ### 15.1：Persistence 是 Runtime 的实现细节，不是 App API
 *
 * 15.1 之前这里是 `val persistence`，App 可以直接拿到
 * Repository / SQLDelight Store。那样 `DismRuntimeApi`
 * 就没有真正成为 Facade——以后把 SQLDelight 换成别的
 * driver、或者改成远程同步，App 都会被绑死。
 *
 * 现在改成 private，并**没有新增任何替代入口**：
 * App 需要的东西全部通过 [DismRuntimeApi] 上的方法取得。
 */
@DismInternalApi
class DismRuntime(
    private val persistence: DismPersistence,

    /**
     * 13.11：当前引擎与版本策略。
     *
     * 默认值是"当前版本只认识自己"，
     * 因此既有调用点 `DismRuntime(persistence)` 继续有效。
     */
    val engineRegistry:
        DynamicScheduleEngineRegistry =
        DefaultDynamicScheduleEngineRegistry.create(),
) : DismRuntimeApi {

    /**
     * 13.4：Bootstrapper 是 Runtime 生命周期的一部分，
     * 不是每次调用恢复 API 时临时 new 出来的对象。
     *
     * ```text
     * DismRuntime
     *  ├── persistence
     *  ├── bootstrapper
     *  ├── restored      （启动那一刻的诊断快照）
     *  └── engine
     * ```
     */
    private val bootstrapper =
        CoreBootstrapper(
            persistence,
        )

    /**
     * 启动恢复结果，供 Debug / 启动诊断 / 开发工具使用。
     *
     * 15.1 起它是 **private**：之前的 public property 与
     * "这不是 App 的主要入口"这句注释本身是矛盾的。
     *
     * 以后调试工具真的需要它，应该通过一个明确的
     * `RuntimeDiagnostics` 暴露——而不是把
     * `RestoredCoreState` 直接变成公共 API。
     */
    private val restored =
        bootstrapper.restore()

    private val engine =
        SemanticStateEngine(
            entities = persistence.entities,
            evidence = persistence.evidence,
            stateRepository = persistence.relationStates,
            transitionLogRepository = persistence.transitionLog,
            stateCommitRepository = persistence.stateCommit,
        )

    override fun observe(
        draft: SemanticObservationDraft,
        context: Context,
    ): ObserveResult {
        return engine.observe(
            draft = draft,
            context = context,
        )
    }

    override fun observeAndEvaluate(
        draft: SemanticObservationDraft,
        context: Context,
        userId: Uuid,
        attentionPolicy: AttentionPolicy,
    ): ReactiveEvaluationResult {
        return engine.observeAndEvaluate(
            draft = draft,
            context = context,
            userId = userId,
            attentionPolicy = attentionPolicy,
        )
    }

    override fun currentStates():
        List<RelationState> {
        return engine.currentStates()
    }

    override fun queryAt(
        instant: Instant,
        subjectId: Uuid?,
        predicate: Predicate?,
        objectId: Uuid?,
        minimumConfidence: Double,
    ): List<RelationState> {
        return engine.queryAt(
            instant = instant,
            subjectId = subjectId,
            predicate = predicate,
            objectId = objectId,
            minimumConfidence = minimumConfidence,
        )
    }

    override fun snapshot(): RuntimeSnapshot {
        // 实时从 persistence 读取，反映 observe 之后的当前状态；
        // restored 只是启动那一刻的诊断快照。
        return RuntimeSnapshot(
            entities = persistence.entities.all(),
            evidence = persistence.evidence.all(),
            relationStates = persistence.relationStates.loadAll(),
            transitions = persistence.transitionLog.all(),
        )
    }

    /**
     * 启动恢复 12.x 调度决策历史。
     *
     * ```kotlin
     * val restoredSchedule =
     *     runtime.restoreSchedule(listOf(itemId))
     *
     * restoredSchedule.snapshotFor(itemId)  // 最近一次决策
     * restoredSchedule.traceFor(itemId)     // 决策链路
     * ```
     *
     * 恢复出来的 Snapshot / Trace 就是持久化层里那一份，
     * 因此和 `core.replay()` / `core.verifyReplay()` 用的是同一份历史。
     */
    suspend fun restoreSchedule(
        itemIds: List<DynamicScheduleItemId>,
    ): RestoredScheduleState =
        bootstrapper.restoreSchedule(itemIds)

    /**
     * 取一条装配好的调度决策链路（11.x → 12.x → 13.x）。
     *
     * 存储来自构造时注入的 persistence，
     * 所以平台层把 driver 指向真实数据库时，
     * 这里产出的 Snapshot / Trace / Replay Input 就会落盘。
     *
     * 15.1 起它是 [DismRuntimeApi] 的一部分：
     * 在这之前 `dism.runtime` 的静态类型是 `DismRuntimeApi`，
     * 而 `dynamicSchedule` 只在实现类 `DismRuntime` 上，
     * 于是通过公共入口拿到的东西**反而没有这个方法**。
     *
     * @param identityByItem schedule item → external key
     *        （11.7 Entity Binding 落地前的显式映射）
     */
    override fun dynamicSchedule(
        identityByItem:
            Map<DynamicScheduleItemId, String>,
    ): DynamicScheduleFacade {

        /*
         * 15.3：Session 是内部集成会话，Facade 才是产品 API。
         *
         * 这里是**唯一**的桥：
         *
         * ```text
         * Product  DynamicScheduleFacade
         *               ▲
         *               │  只有这一处
         * Internal  DynamicScheduleSession
         * ```
         *
         * Runtime 自己知道 Session 是什么，App 完全不知道。
         */
        val session =
            DynamicScheduleIntegrationAssembly
                .from(
                    persistence = persistence,
                    engineRegistry = engineRegistry,
                )
                .build(identityByItem)

        return DefaultDynamicScheduleFacade(
            session = session,
        )
    }
}
