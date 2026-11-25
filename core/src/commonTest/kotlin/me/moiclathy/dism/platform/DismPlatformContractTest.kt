package me.moiclathy.dism.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DismPlatformContractTest {

    @Test
    fun platformInfoCanDescribeRuntime() {

        val info =
            DismPlatformInfo(
                platform = DismPlatform.JVM,
                runtimeName = "JVM",
                runtimeVersion = "21",
                capabilities =
                    setOf(
                        DismPlatformCapability.LOCAL_STORAGE,
                        DismPlatformCapability.FILE_SYSTEM,
                    ),
            )

        assertEquals(
            DismPlatform.JVM,
            info.platform,
        )

        assertEquals(
            "JVM",
            info.runtimeName,
        )

        assertTrue(
            DismPlatformCapability.LOCAL_STORAGE
                in info.capabilities,
        )
    }

    @Test
    fun defaultPlatformIsUnknown() {

        val entryPoint =
            DefaultDismPlatformEntryPoint()

        assertEquals(
            DismPlatform.UNKNOWN,
            entryPoint.platform.platform,
        )
    }
}
