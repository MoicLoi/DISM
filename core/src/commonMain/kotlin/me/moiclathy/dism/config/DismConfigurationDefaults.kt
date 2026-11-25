package me.moiclathy.dism.config

import me.moiclathy.dism.api.DismPublicApi

/**
 * Default product configuration.
 */
@DismPublicApi
object DismConfigurationDefaults {

    val standard:
        DismConfiguration =
        DismConfiguration(
            storage =
                DismStorageConfiguration(
                    mode = DismStorageMode.IN_MEMORY,
                ),

            runtime =
                DismRuntimeConfiguration(
                    enableAttention = true,
                    enableDynamicSchedule = true,
                    enableAdaptation = true,
                ),
        )
}
