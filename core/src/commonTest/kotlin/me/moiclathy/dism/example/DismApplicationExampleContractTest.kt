package me.moiclathy.dism.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismApiVersion
import me.moiclathy.dism.api.DismCapabilities
import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.runtime.RuntimeSnapshot
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 15.12：Example Application 契约。
 *
 * ### 这个测试真正在证明什么
 *
 * 它**不**测试任何算法。它证明的是：
 *
 * ```text
 * 一个只使用 DISM Public API 的最小应用路径能够成立
 * ```
 *
 * 尤其是：应用可以在**完全不知道** Persistence / Repository /
 * SQLDelight / RuntimeFactory 的情况下把东西跑起来。
 *
 * 这里用两个 test double 来证明这一点——
 * 它们各自只实现公共接口，不触碰任何 internal 类型。
 */
class DismApplicationExampleContractTest {

    @Test
    fun exampleConsumesOnlyDismContract() {

        val dism =
            FakeDism(
                runtime = FakeRuntime(),
            )

        val application =
            DismApplicationExample(
                dism = dism,
            )

        val result =
            application.start()

        assertEquals(
            DismApiVersion.STRING,
            result.apiVersion,
        )

        assertNotNull(result.snapshot)
    }

    @Test
    fun exampleReadsProductCapabilities() {

        val application =
            DismApplicationExample(
                dism = FakeDism(runtime = FakeRuntime()),
            )

        val capabilities = application.capabilities

        assertTrue(capabilities.semanticState)

        assertTrue(capabilities.dynamicSchedule)

        assertEquals(
            DismApiVersion.STRING,
            application.apiVersion,
        )

        assertNotNull(application.runtime)
    }

    @Test
    fun snapshotViewExposesApplicationLevelNumbers() {

        val result =
            DismApplicationExample(
                dism = FakeDism(runtime = FakeRuntime()),
            ).start()

        /*
         * 应用视图应该给出"算好的数字"，
         * 而不是逼 UI 自己去数。
         */
        assertEquals(
            0,
            result.activeRelationCount,
        )
    }

    @Test
    fun realFactorySatisfiesTheSameExamplePath() {

        /*
         * 用**真实的** DismFactory 再走一遍同一个应用路径。
         *
         * 如果只能靠 test double 跑通，那这个 Example
         * 就只是在证明"接口能被实现"，而不是"产品可用"。
         */
        val application =
            DismApplicationExample(
                dism = DismFactory.create(),
            )

        val result = application.start()

        assertEquals(
            DismApiVersion.STRING,
            result.apiVersion,
        )

        assertTrue(result.snapshot.entities.isEmpty())

        assertTrue(result.snapshot.relationStates.isEmpty())
    }
}

/**
 * 只实现公共接口的 test double。
 */
private class FakeDism(
    override val runtime: DismRuntimeApi,
) : Dism {

    override val apiVersion: String =
        DismApiVersion.STRING

    override val capabilities: DismCapabilities =
        DismCapabilities()
}

/**
 * 一个"只想读快照"的应用被迫实现的全部 Runtime 方法。
 *
 * 除了 snapshot() 之外全部 `TODO(...)`——
 * 这正是 15.12 要暴露的那个架构事实：
 *
 * ```text
 * 一个 Example Application
 *          ↓
 * 被强迫依赖整个 Runtime Contract
 * ```
 *
 * 在 15.x 之后把 `DismRuntimeApi` 拆小之前，
 * 这就是真实消费者的最低成本。
 */
private class FakeRuntime : DismRuntimeApi {

    override fun observe(
        draft:
            me.moiclathy.dism.semantic.observation
                .SemanticObservationDraft,
        context: me.moiclathy.dism.context.Context,
    ): me.moiclathy.dism.semantic.engine.ObserveResult =
        TODO("Example contract does not execute observation")

    override fun observeAndEvaluate(
        draft:
            me.moiclathy.dism.semantic.observation
                .SemanticObservationDraft,
        context: me.moiclathy.dism.context.Context,
        userId: kotlin.uuid.Uuid,
        attentionPolicy:
            me.moiclathy.dism.attention.AttentionPolicy,
    ): me.moiclathy.dism.semantic.engine
        .ReactiveEvaluationResult =
        TODO("Example contract does not execute evaluation")

    override fun currentStates():
        List<me.moiclathy.dism.state.model.RelationState> =
        emptyList()

    override fun queryAt(
        instant: kotlin.time.Instant,
        subjectId: kotlin.uuid.Uuid?,
        predicate: me.moiclathy.dism.domain.relation.Predicate?,
        objectId: kotlin.uuid.Uuid?,
        minimumConfidence: Double,
    ): List<me.moiclathy.dism.state.model.RelationState> =
        emptyList()

    override fun snapshot(): RuntimeSnapshot =
        RuntimeSnapshot(
            entities = emptyList(),
            evidence = emptyList(),
            relationStates = emptyList(),
            transitions = emptyList(),
        )

    override fun dynamicSchedule(
        identityByItem:
            Map<DynamicScheduleItemId, String>,
    ): DynamicScheduleFacade =
        TODO("Example contract does not execute scheduling")
}
