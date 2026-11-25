package me.moiclathy.dism.runtime

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.engine.ReactiveEvaluationResult
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM Core 对 App 的唯一收口。
 *
 * App 只面对这个接口，不需要知道内部存在：
 * SQLite / SQLDelight / Reducer / Transition /
 * Propagation / RequirementRisk / Recommendation / Attention。
 *
 * 只暴露与"用户现实模型"相关的能力：
 * - observe：只维护世界状态（纯入口）
 * - observeAndEvaluate：在维护状态的同时跑传播 → 建议 → 注意力
 * - currentStates / queryAt：当前与某时刻的世界状态
 * - snapshot：一次性读取长期状态（供调试/诊断）
 */
@DismPublicApi
interface DismRuntimeApi {

    /**
     * 录入一条观察，只更新世界状态，不产生用户输出。
     */
    fun observe(
        draft: SemanticObservationDraft,
        context: Context,
    ): ObserveResult

    /**
     * 录入观察并立即跑完整反应链：
     * StateChange → Propagation → RequirementRisk
     * → Recommendation → Attention。
     */
    fun observeAndEvaluate(
        draft: SemanticObservationDraft,
        context: Context,
        userId: Uuid,
        attentionPolicy: AttentionPolicy =
            AttentionPolicy(),
    ): ReactiveEvaluationResult

    /**
     * 当前仍活动的全部关系状态。
     */
    fun currentStates():
        List<RelationState>

    /**
     * 查询某一具体时间点上有效的世界状态。
     *
     * 参数与 SemanticStateEngine.queryAt 保持一致，
     * predicate 使用领域类型 Predicate，不退化成 String。
     */
    fun queryAt(
        instant: Instant,
        subjectId: Uuid? = null,
        predicate: Predicate? = null,
        objectId: Uuid? = null,
        minimumConfidence: Double = 0.0,
    ): List<RelationState>

    /**
     * 读取长期状态快照。
     *
     * 不暴露 RestoredCoreState / Repository，
     * 内部存储结构变化时 App 无需跟随修改。
     */
    fun snapshot(): RuntimeSnapshot

    /**
     * 15.1 / 15.3：取一条装配好的**动态调度**决策链路。
     *
     * ```kotlin
     * val schedule = runtime.dynamicSchedule(
     *     identityByItem = mapOf(itemId to "external-key"),
     * )
     *
     * schedule.evaluate(model, now)
     * schedule.latestDecision(itemId)
     * schedule.history.query(itemId)
     * ```
     *
     * ### 为什么它必须在这里
     *
     * 15.1 之前 `dynamicSchedule` 只在实现类 `DismRuntime` 上。
     * 而 App 通过 `Dism.runtime` 拿到的是 `DismRuntimeApi`——
     * 也就是说**公共入口反而拿不到这个能力**。
     *
     * ### 15.3：返回的是 Facade，不是 Session
     *
     * `DynamicScheduleSession` 挂载了 History / Replay /
     * Integrity / Repair / Timeline / Feature / Interpretation /
     * Behavior / Weight / Attention / Priority / Context /
     * Confidence / Boundary / Override / Adaptation Persistence /
     * Replay / Verification / Provenance / Explanation / Audit……
     *
     * 它是一个很好的**内部集成会话**，但不是已经适合冻结的
     * 公共 API。所以 15.3 在这里返回
     * [DynamicScheduleFacade]——只暴露 evaluate /
     * latestDecision / history 三件产品真正需要的事。
     *
     * @param identityByItem schedule item → external key
     *        （11.7 Entity Binding 落地前的显式映射）
     */
    fun dynamicSchedule(
        identityByItem:
            Map<DynamicScheduleItemId, String> = emptyMap(),
    ): DynamicScheduleFacade
}
