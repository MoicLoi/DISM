package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 15.1：Core API Freeze 的契约测试。
 *
 * 这些断言的用途不是"验证实现"，而是**在有人无意中
 * 改动公共 API 时立刻失败**。
 *
 * 例如把 `DismApiVersion.MINOR` 从 0 改成 1、
 * 或者把某个 capability 默认关掉，都应该是一个
 * 需要显式决定的行为，而不是顺手改掉。
 */
class DismApiContractTest {

    @Test
    fun apiVersionIsStable() {

        assertEquals(
            "1.5.0",
            DismApiVersion.STRING,
        )

        assertEquals(1, DismApiVersion.MAJOR)

        assertEquals(5, DismApiVersion.MINOR)

        assertEquals(0, DismApiVersion.PATCH)
    }

    @Test
    fun defaultCapabilitiesExposeStableCore() {

        val capabilities =
            DismCapabilities()

        assertTrue(capabilities.semanticState)

        assertTrue(capabilities.dynamicSchedule)

        assertTrue(capabilities.attention)

        assertTrue(capabilities.persistence)

        assertTrue(capabilities.replay)

        assertTrue(capabilities.verification)

        assertTrue(capabilities.adaptation)

        assertTrue(capabilities.localFirst)
    }

    @Test
    fun runtimeApiExposesScheduleSession() {

        /*
         * 15.1 的关键收口：`dynamicSchedule` 必须在**接口**上，
         * 而不是只在实现类 `DismRuntime` 上。
         *
         * 否则 App 通过 `Dism.runtime`（静态类型 DismRuntimeApi）
         * 拿到的对象反而没有这个能力。
         *
         * 这里不构造真正的 Runtime，而是把"以接口类型调用它"
         * 写成一个函数值——如果方法从接口上消失，
         * 这个文件**编译不过**，比运行时断言更早发现。
         */
        val callDynamicSchedule:
            (
                me.moiclathy.dism.runtime.DismRuntimeApi,
                Map<
                    me.moiclathy.dism.schedule.model
                        .DynamicScheduleItemId,
                    String,
                    >,
            ) -> me.moiclathy.dism.schedule.api
                .DynamicScheduleFacade =
            { api, identityByItem ->
                api.dynamicSchedule(identityByItem)
            }

        assertTrue(callDynamicSchedule is Function2<*, *, *>)

        /*
         * 默认参数也必须存在于接口上，
         * 这样 `api.dynamicSchedule()` 仍然可调用。
         */
        val callWithDefaults:
            (me.moiclathy.dism.runtime.DismRuntimeApi) ->
                me.moiclathy.dism.schedule.api
                    .DynamicScheduleFacade =
            { api -> api.dynamicSchedule() }

        assertTrue(callWithDefaults is Function1<*, *>)
    }
}
