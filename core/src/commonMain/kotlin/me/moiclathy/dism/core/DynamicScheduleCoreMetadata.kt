package me.moiclathy.dism.core

/**
 * 13.1
 *
 * Core 对外元信息。
 */
data class DynamicScheduleCoreMetadata(
    val name:
        String = "DISM Core",

    val version:
        String = "13.1",

    val architecture:
        String =
        "local-first personal semantic state and adaptive scheduling engine",

    val capabilities:
        DynamicScheduleCoreCapabilities =
        DynamicScheduleCoreCapabilities.current(),
)
