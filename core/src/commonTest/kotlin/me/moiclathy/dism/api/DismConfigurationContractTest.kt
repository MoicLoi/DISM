package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismRuntimeConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode

class DismConfigurationContractTest {

    @Test
    fun defaultConfigurationIsUsable() {

        val dism =
            DismFactory.create()

        assertTrue(
            dism.capabilities.dynamicSchedule,
        )

        assertTrue(
            dism.capabilities.attention,
        )

        assertTrue(
            dism.capabilities.adaptation,
        )
    }

    @Test
    fun runtimeCapabilitiesFollowConfiguration() {

        val configuration =
            DismConfiguration(
                storage =
                    DismStorageConfiguration(
                        mode = DismStorageMode.IN_MEMORY,
                    ),

                runtime =
                    DismRuntimeConfiguration(
                        enableAttention = false,
                        enableDynamicSchedule = true,
                        enableAdaptation = false,
                    ),
            )

        val dism =
            DismFactory.create(
                configuration,
            )

        assertTrue(
            dism.capabilities.dynamicSchedule,
        )

        assertFalse(
            dism.capabilities.attention,
        )

        assertFalse(
            dism.capabilities.adaptation,
        )
    }
}
