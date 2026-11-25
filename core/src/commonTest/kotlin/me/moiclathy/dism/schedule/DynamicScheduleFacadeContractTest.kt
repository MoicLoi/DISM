package me.moiclathy.dism.schedule

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 15.10.3 Schedule Facade Contract
 *
 * 从**产品入口**一路走到 schedule facade，而不是直接构造 Session。
 *
 * ### 规格这一版有三处编译不过
 *
 * 1. `import ...schedule.api.DynamicScheduleModel` —— 这个类型在
 *    `schedule.model` 包，不在 `schedule.api`
 * 2. `DynamicScheduleModel(items = emptyList())` —— `id` 是必填参数
 * 3. `facade.evaluate(...)` 是 `suspend`，测试必须跑在协程里
 *
 * 三处都是"规格里的代码没有真正编译过"的典型症状。
 */
class DynamicScheduleFacadeContractTest {

    @Test
    fun runtimeExposesPublicScheduleFacade() {

        val dism = DismFactory.create()

        val facade = dism.runtime.dynamicSchedule()

        assertTrue(facade is DynamicScheduleFacade)
    }

    @Test
    fun evaluationReturnsStructuredResult() = runTest {

        val dism = DismFactory.create()

        val facade = dism.runtime.dynamicSchedule()

        val model =
            DynamicScheduleModel(
                id = DynamicScheduleId("15.10-schedule"),
                items = emptyList(),
            )

        val result =
            facade.evaluate(
                model = model,
                now = Clock.System.now(),
            )

        assertNotNull(result)

        assertNotNull(result.decisions)

        /*
         * 空 model 必须得到空决策，而不是抛错。
         */
        assertTrue(result.decisions.isEmpty())

        assertTrue(result.attentionRequiredCount == 0)
    }
}
