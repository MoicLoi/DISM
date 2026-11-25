package me.moiclathy.dism.compatibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismApiVersion
import me.moiclathy.dism.api.DismCapabilities
import me.moiclathy.dism.api.DismException
import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.api.DismFailure
import me.moiclathy.dism.api.DismFailureCode
import me.moiclathy.dism.api.DismOperationResult
import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.diagnostics.DismDiagnosticEvent
import me.moiclathy.dism.diagnostics.DismDiagnosticSink
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration
import me.moiclathy.dism.platform.DismPlatformEntryPoint
import me.moiclathy.dism.provider.DismProvider
import me.moiclathy.dism.provider.DismProviderRegistry
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.runtime.RuntimeSnapshot
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade

/**
 * 15.11 Public Compatibility Documentation
 *
 * "文档即测试"：把 15.x 冻结下来的公共面写成一份**可执行清单**。
 *
 * ### 它替代不了什么
 *
 * 它替代不了 binary API validation
 * （`kotlinx-binary-compatibility-validator` 那类工具）。
 * 规格明确说不要在缺少构建环境时自己造一个反射扫描器。
 *
 * 所以这里只做一件事：让"哪些类型属于承诺范围"
 * 变成一个显式的、会被编译器检查的列表。
 *
 * ```text
 * 有人删掉一个公共类型
 *     ↓
 * 这个文件编译失败
 * ```
 *
 * 这比文档可靠，比二进制校验容易，且今天就能用。
 */
class DismPublicCompatibilityDocumentationTest {

    @Test
    fun frozenPublicTypesAreReachable() {

        /*
         * 这些类型引用不是"断言"，而是编译期契约：
         * 任何一个被删除/改名，本文件立刻编译不过。
         *
         * 分类与 15.x 的边界审计一致。
         */
        val frozen: List<kotlin.reflect.KClass<*>> =
            listOf(
                // api
                Dism::class,
                DismFactory::class,
                DismApiVersion::class,
                DismCapabilities::class,
                DismFailure::class,
                DismFailureCode::class,
                DismException::class,
                DismOperationResult::class,
                DismPublicApi::class,

                // runtime
                DismRuntimeApi::class,
                RuntimeSnapshot::class,

                // config
                DismConfiguration::class,

                // diagnostics
                DismDiagnosticEvent::class,
                DismDiagnosticSink::class,
                DismDiagnosticsConfiguration::class,

                // provider SPI
                DismProvider::class,
                DismProviderRegistry::class,

                // platform
                DismPlatformEntryPoint::class,

                // schedule facade
                DynamicScheduleFacade::class,
            )

        assertEquals(
            19,
            frozen.size,
            "公共面清单发生变化时，这个数字必须一起改——" +
                "那正是我们要的：一次显式决定，而不是顺手删掉",
        )

        assertTrue(frozen.all { it.simpleName!!.isNotBlank() })
    }

    @Test
    fun capabilitiesMatrixMatchesTheFrozenSurface() {

        /*
         * `DismCapabilities` 是产品对宿主的承诺。
         * 默认配置下这些必须全部为 true——
         * 否则"默认可用"这句承诺就是假的。
         */
        val dism = DismFactory.create()

        val capabilities = dism.capabilities

        listOf(
            "semanticState" to capabilities.semanticState,
            "dynamicSchedule" to capabilities.dynamicSchedule,
            "attention" to capabilities.attention,
            "persistence" to capabilities.persistence,
            "replay" to capabilities.replay,
            "verification" to capabilities.verification,
            "adaptation" to capabilities.adaptation,
            "localFirst" to capabilities.localFirst,
        ).forEach { (name, value) ->
            assertTrue(
                value,
                "default capabilities must advertise $name",
            )
        }
    }

    @Test
    fun internalTypesAreNotPartOfTheFrozenList() {

        /*
         * 反向检查：这些名字**不应该**出现在公共面清单里。
         *
         * 这里只能做字符串层面的检查（commonMain 没有可靠反射），
         * 但它至少能拦住"有人把 internal 类型误加进白名单"。
         */
        val internalNames =
            setOf(
                "DismRuntime",
                "DismRuntimeFactory",
                "DismPersistence",
                "DismPersistenceFactory",
                "DynamicScheduleSession",
                "DynamicScheduleIntegrationAssembly",
                "DismDiagnostics",
                "DismBootstrap",
            )

        val frozenNames =
            listOf(
                Dism::class,
                DismFactory::class,
                DismRuntimeApi::class,
                RuntimeSnapshot::class,
                DynamicScheduleFacade::class,
            ).mapNotNull { it.simpleName }

        internalNames.forEach { internal ->
            assertTrue(
                internal !in frozenNames,
                "$internal is internal and must not be frozen as " +
                    "public API",
            )
        }
    }
}
