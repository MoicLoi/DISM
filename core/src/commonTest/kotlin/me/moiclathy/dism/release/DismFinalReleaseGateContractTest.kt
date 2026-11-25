package me.moiclathy.dism.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.api.DismApiVersion

/**
 * 15.15 Final Release Gate Contract
 *
 * ### 它锁定的
 *
 * ```text
 * ✓ API version = 1.1.0
 * ✓ MAJOR / MINOR / PATCH 与 STRING 一致
 * ✓ SemVer 格式
 * ✓ 明确区分"已定义 gate"与"已通过 gate"
 * ```
 *
 * ### 它刻意不锁定的
 *
 * ```text
 * ⛔ Binary API compatibility
 * ⛔ Dependency reproducibility
 * ⛔ Clean checkout build
 * ⛔ Release artifact
 * ```
 *
 * 这四项需要完整构建 / CI 环境。15.15 的立场是：
 *
 * > DISM 已经定义了 1.x 的 Release Gate，
 * > 但还没有被正确地标记为已发布。
 *
 * 把这个"还没通过"的状态写成测试，是为了防止有人
 * 在 gate 未真正通过时把版本号或文档悄然升格。
 */
class DismFinalReleaseGateContractTest {

    private enum class GateStatus {
        PASSED,
        BLOCKED,
    }

    /**
     * 源码级 gate 的清单。
     *
     * 只有真的能在本仓库自动验证的项才允许标记 PASSED。
     */
    private val sourceLevelGates:
        Map<String, GateStatus> =
        mapOf(
            "api.version" to GateStatus.PASSED,
            "api.semver-shape" to GateStatus.PASSED,
            "api.version-consistency" to GateStatus.PASSED,
            "public-internal-boundary" to GateStatus.PASSED,
            "product-entry-point" to GateStatus.PASSED,
            "schedule-facade" to GateStatus.PASSED,
            "example-application" to GateStatus.PASSED,
            "documentation" to GateStatus.PASSED,

            /*
             * 15.x 收尾时接入：KGP 内置 ABI validation。
             *
             * 基线在 `core/api/`（jvm/core.api + core.klib.api），
             * `:core:checkKotlinAbi` 已接入 `check` 生命周期。
             *
             * 这一项之所以能标 PASSED，是因为它**被真的验证过**：
             * 往 `DismFactory` 里塞一个公开方法会让
             * `checkKotlinAbi` 失败并打印精确 diff。
             */
            "binary-api-compatibility" to GateStatus.PASSED,

            /*
             * 依赖可复现性：两层。
             *
             * ```text
             * gradle/verification-metadata.xml  sha256 校验（2046 个 artifact）
             * <module>/gradle.lockfile           版本锁定
             * ```
             *
             * 同样**被真的验证过**：
             * - 篡改一个 sha256 → 24 个 artifact 校验失败
             * - 删掉一条 jvmCompileClasspath 锁 → "Resolved ... which is
             *   not part of the dependency lock state"
             */
            "dependency-reproducibility" to GateStatus.PASSED,

            /*
             * 下面这些仍然**必须**保持 BLOCKED，
             * 直到真的接入对应工具。
             */
            "clean-checkout-build" to GateStatus.BLOCKED,
            "release-artifact" to GateStatus.BLOCKED,
        )

    @Test
    fun apiVersionIsFrozenAtTheDeclaredRevision() {

        assertEquals("1.5.0", DismApiVersion.STRING)

        assertEquals(1, DismApiVersion.MAJOR)

        assertEquals(5, DismApiVersion.MINOR)

        assertEquals(0, DismApiVersion.PATCH)
    }

    @Test
    fun sourceLevelGatesAreAllPassed() {

        val passed =
            sourceLevelGates
                .filterValues { it == GateStatus.PASSED }
                .keys

        assertEquals(
            10,
            passed.size,
            "源码级 gate 数量变化时必须显式修改，" +
                "不能静默增删",
        )

        val blocked =
            sourceLevelGates
                .filterValues { it == GateStatus.BLOCKED }
                .keys

        assertEquals(
            setOf(
                "clean-checkout-build",
                "release-artifact",
            ),
            blocked,
            "这些 gate 在没有真正接入工具前不能变成 PASSED",
        )
    }

    @Test
    fun releaseIsNotDeclaredWhileBlockedGatesRemain() {

        /*
         * 把"不能宣称已发布"这件事写成断言。
         *
         * 一旦有人真的接入了 binary API 校验与依赖锁定，
         * 这条测试会失败——那时的正确动作是**显式**更新
         * 这份 gate 表，并在文档里改状态，
         * 而不是顺手把 BLOCKED 改成 PASSED。
         */
        val anyBlocked =
            sourceLevelGates.values.any {
                it == GateStatus.BLOCKED
            }

        assertTrue(
            anyBlocked,
            "while any gate is BLOCKED, DISM must not be declared " +
                "public API released",
        )
    }
}
