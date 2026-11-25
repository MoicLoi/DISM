package me.moiclathy.dism.compatibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.api.DismApiVersion
import me.moiclathy.dism.api.DismFactory

/**
 * 15.11 Public API Compatibility Contract
 *
 * 这一阶段**只能**落地与版本号有关的契约。
 *
 * 规格原文把 15.11 拆成两块：
 *
 * ```text
 * ✓ API version contract
 * ✓ SemVer compatibility contract
 * ✓ Public compatibility rules
 * ⏳ Gradle version lock
 * ⏳ Kotlin / AGP / KMP version lock
 * ⏳ dependency lock
 * ⏳ reproducible build
 * ⏳ binary API validation
 * ⏳ CI compatibility verification
 * ```
 *
 * 后六项需要完整的 Gradle 根工程（`settings.gradle.kts`、
 * `gradle/libs.versions.toml`、wrapper）。规格明确说
 * **不要**在没有构建环境的时候假装它们已经完成——
 * 那正是这一节自己的结论，这里照办。
 */
class DismApiCompatibilityContractTest {

    /**
     * 兼容性规则本身。
     *
     * 把它写成数据而不是注释，是为了让"规则"能被断言，
     * 而不只是贴在文档里。
     */
    private enum class CompatibilityRule(
        val change: String,
        val versionComponent: String,
    ) {
        BREAKING_PUBLIC_API("删除/改名/改签名", "MAJOR"),
        ADDITIVE_PUBLIC_API("新增可选参数/新类型", "MINOR"),
        COMPATIBLE_FIX("行为修复、文档、内部重构", "PATCH"),
    }

    @Test
    fun versionFollowsSemVerShape() {

        val string = DismApiVersion.STRING

        assertTrue(
            Regex("""^\d+\.\d+\.\d+$""").matches(string),
            "DismApiVersion.STRING must have MAJOR.MINOR.PATCH " +
                "shape; got=$string",
        )

        assertEquals(3, string.split(".").size)
    }

    @Test
    fun stringMatchesItsOwnComponents() {

        /*
         * 这条防的是最低级也最常见的错误：
         * 有人改了 MAJOR 却忘了 STRING（或反过来）。
         */
        assertEquals(
            "${DismApiVersion.MAJOR}." +
                "${DismApiVersion.MINOR}." +
                "${DismApiVersion.PATCH}",
            DismApiVersion.STRING,
        )
    }

    @Test
    fun majorIsAtLeastOneForTheFrozenSurface() {

        /*
         * 15.1 起公开面已经冻结，MAJOR 不应该停在 0。
         * 0.x 在 SemVer 里意味着"任何东西都可能变"。
         */
        assertTrue(DismApiVersion.MAJOR >= 1)

        assertTrue(DismApiVersion.MINOR >= 0)

        assertTrue(DismApiVersion.PATCH >= 0)
    }

    @Test
    fun runtimeAdvertisesTheDeclaredApiVersion() {

        /*
         * 真正的跨层校验：
         *
         * ```text
         * DismApiVersion.STRING  ==  dism.apiVersion
         * ```
         *
         * 这两处一旦漂移，宿主就无法判断自己面对的是哪个 API 版本。
         */
        val dism = DismFactory.create()

        assertEquals(
            DismApiVersion.STRING,
            dism.apiVersion,
        )
    }

    @Test
    fun compatibilityRulesCoverAllThreeSemVerComponents() {

        val components =
            CompatibilityRule.entries
                .map { it.versionComponent }
                .toSet()

        assertEquals(
            setOf("MAJOR", "MINOR", "PATCH"),
            components,
            "每一类变更都必须明确映射到一个 SemVer 分量",
        )
    }
}
