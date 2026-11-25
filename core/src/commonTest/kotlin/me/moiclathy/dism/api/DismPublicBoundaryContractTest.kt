package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.diagnostics.DismDiagnosticLevel
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration
import me.moiclathy.dism.provider.DismProvider
import me.moiclathy.dism.provider.DismProviderCapability
import me.moiclathy.dism.provider.DismProviderContext
import me.moiclathy.dism.provider.DismProviderResult
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 15.10.9 Public Boundary Contract
 *
 * 规格的目录树里列了这个文件，但没有给出代码。
 *
 * ### 它证明的是什么
 *
 * 这个文件**整份没有 `@OptIn(DismInternalApi::class)`**。
 *
 * 15.2 起，任何触碰 internal 类型的代码都会收到
 * "This is an internal DISM API" 的警告；如果哪天变成
 * ERROR 级别（15.x 的既定方向），这个文件会**编译失败**。
 *
 * 也就是说：这份文件本身就是"公共 API 白名单"的可执行形式。
 *
 * ```text
 * 能在这里出现的类型 == 承诺兼容的公共 API
 * ```
 *
 * ### 它不能证明什么
 *
 * 它**不能**证明某个公共签名的返回类型里没有藏 internal 类型——
 * 那需要反射，而 commonMain 没有可靠的反射。
 * 那部分由各自的契约测试（如
 * `DynamicScheduleFacadeContractTest` 里用公共类型直接
 * 构造 decision view）间接保证。
 */
class DismPublicBoundaryContractTest {

    private class BoundaryProvider : DismProvider {

        override val id = "boundary"

        override val name = "Boundary Provider"

        override val version = "1.0.0"

        override val capabilities =
            setOf(DismProviderCapability.READ)
    }

    @Test
    fun fullProductPathWorksWithoutAnyInternalType() = runTest {

        /*
         * 1) 配置（config 包）
         */
        val configuration =
            DismConfiguration(
                diagnostics =
                    DismDiagnosticsConfiguration(
                        enabled = true,
                    ),
            )

        /*
         * 2) 产品入口（api 包）
         */
        val dism = DismFactory.create(configuration)

        assertEquals(
            DismApiVersion.STRING,
            dism.apiVersion,
        )

        /*
         * 3) capability 广告
         */
        val capabilities = dism.capabilities

        assertTrue(capabilities.semanticState)
        assertTrue(capabilities.localFirst)

        /*
         * 4) Runtime（runtime 包）
         */
        val runtime = dism.runtime

        assertNotNull(runtime.snapshot())

        assertTrue(runtime.currentStates().isEmpty())

        /*
         * 5) Schedule Facade（schedule.api 包）
         */
        val schedule: DynamicScheduleFacade =
            runtime.dynamicSchedule()

        assertNotNull(schedule.history)

        /*
         * 6) schedule model（schedule.model 包）
         */
        assertTrue(
            schedule.latestDecision(
                DynamicScheduleItemId("boundary-1"),
            ) == null,
        )

        /*
         * 7) Provider SPI（provider 包）
         */
        val provider: DismProvider = BoundaryProvider()

        assertEquals("1.0.0", provider.version)

        val providerResult:
            DismProviderResult<String> =
            DismProviderResult.Success(value = "ok")

        assertTrue(providerResult is DismProviderResult.Success)

        val providerContext =
            DismProviderContext(
                requestedAt = kotlin.time.Clock.System.now(),
            )

        assertTrue(providerContext.attributes.isEmpty())

        /*
         * 8) Error model（api 包）
         */
        val failure =
            DismFailure(
                code = DismFailureCode.INVALID_INPUT,
                message = "boundary check",
            )

        assertEquals(
            "INVALID_INPUT: boundary check",
            DismFailureFormatter.format(failure),
        )

        /*
         * 9) Diagnostics（diagnostics 包）
         */
        val event =
            DismDiagnosticLevel.entries.first()

        assertEquals(DismDiagnosticLevel.DEBUG, event)
    }
}
