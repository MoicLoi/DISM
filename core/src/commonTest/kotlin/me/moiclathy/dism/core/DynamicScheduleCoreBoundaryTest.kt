package me.moiclathy.dism.core

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 13.1
 *
 * 当前阶段的 Boundary Test。
 *
 * 真正的 package dependency enforcement
 * 后续可以交给 Gradle / ArchUnit 类工具。
 *
 * commonTest 这里先锁定架构语义。
 */
class DynamicScheduleCoreBoundaryTest {

    @Test
    fun publicCoreBoundaryExists() {

        val coreApi =
            DynamicScheduleCore::class

        assertTrue(
            coreApi.simpleName ==
                "DynamicScheduleCore",
        )
    }

    @Test
    fun capabilitiesAreExplicitlyDeclared() {

        val capabilities =
            DynamicScheduleCoreCapabilities.current()

        assertTrue(
            capabilities
                .historicalReplay,
        )

        assertTrue(
            capabilities
                .replayVerification,
        )
    }
}
