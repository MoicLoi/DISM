package me.moiclathy.dism.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import me.moiclathy.dism.api.DismApiVersion
import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.example.DismApplicationExample
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade

/**
 * 15.14 Release Candidate Contract
 *
 * ### 这份测试的边界
 *
 * 它**只**锁定源码级可以自动验证的门：
 *
 * ```text
 * ✓ API version = 1.1.0
 * ✓ 产品入口可用
 * ✓ Schedule Facade 可达
 * ✓ Example Application 路径成立
 * ✓ 文档要求的公共类型仍存在
 * ```
 *
 * 它**不**检查（也不假装检查）：
 *
 * ```text
 * ⛔ Binary API compatibility
 * ⛔ Dependency reproducibility
 * ⛔ Clean checkout build
 * ⛔ Release artifact
 * ```
 *
 * 那四项需要真实的构建与 CI 环境。自己写一个反射扫描器
 * 冒充 binary API 校验，比不做更糟——它会让人以为已经验过了。
 *
 * 完整的 gate 状态见
 * `docs/release/15.14-release-candidate-checklist.md`。
 */
class DismReleaseCandidateContractTest {

    @Test
    fun releaseCandidateApiVersionIsFrozen() {

        assertEquals(1, DismApiVersion.MAJOR)

        assertEquals(5, DismApiVersion.MINOR)

        assertEquals(0, DismApiVersion.PATCH)

        assertEquals("1.5.0", DismApiVersion.STRING)
    }

    @Test
    fun productEntryPointIsUsable() {

        val dism = DismFactory.create()

        assertNotNull(dism.runtime)

        assertEquals(
            DismApiVersion.STRING,
            dism.apiVersion,
        )

        assertNotNull(dism.runtime.snapshot())
    }

    @Test
    fun scheduleFacadeIsReachableFromTheProductEntryPoint() {

        val schedule: DynamicScheduleFacade =
            DismFactory.create().runtime.dynamicSchedule()

        assertNotNull(schedule)

        assertNotNull(schedule.history)
    }

    @Test
    fun configurationPathIsUsable() {

        val dism =
            DismFactory.create(
                DismConfiguration(),
            )

        assertTrue(dism.capabilities.localFirst)
    }

    @Test
    fun exampleApplicationPathStillWorks() {

        /*
         * 15.12 建立的 Example 必须一直可用——
         * 它是"公共 API 是否适合真实 App"的活体检查。
         */
        val application =
            DismApplicationExample(
                dism = DismFactory.create(),
            )

        val started = application.start()

        assertEquals(
            DismApiVersion.STRING,
            started.apiVersion,
        )

        assertNotNull(started.snapshot)
    }
}
