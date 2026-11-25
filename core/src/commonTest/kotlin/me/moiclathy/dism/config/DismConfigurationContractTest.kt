package me.moiclathy.dism.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.api.DismFactory

class DismConfigurationContractTest {

    @Test
    fun defaultConfigurationCreatesRuntime() {

        val dism =
            DismFactory.create()

        assertTrue(
            dism.capabilities.dynamicSchedule,
        )
    }

    @Test
    fun configurationCanControlCapabilities() {

        val configuration =
            DismConfiguration(
                storage =
                    DismStorageConfiguration(
                        mode =
                            DismStorageMode.IN_MEMORY,
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

        /*
         * 规格这一版有意只断言"没坏的那一项"，
         * 但那样 assertFalse(capabilities.attention) 就不会被覆盖。
         * configuration → capabilities 的映射是 15.5 的核心承诺，
         * 这里把它完整断言掉。
         */
        assertFalse(
            dism.capabilities.attention,
        )

        assertFalse(
            dism.capabilities.adaptation,
        )
    }
}
