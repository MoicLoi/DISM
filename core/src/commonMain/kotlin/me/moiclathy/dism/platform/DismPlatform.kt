package me.moiclathy.dism.platform

import me.moiclathy.dism.api.DismPublicApi

/**
 * Runtime platform on which DISM is running.
 */
@DismPublicApi
enum class DismPlatform {

    /**
     * Android application/runtime.
     */
    ANDROID,

    /**
     * JVM desktop/runtime.
     */
    JVM,

    /**
     * Server-side JVM runtime.
     */
    SERVER,

    /**
     * JavaScript browser/runtime.
     */
    JS,

    /**
     * WebAssembly runtime.
     */
    WASM,

    /**
     * Kotlin/Native Apple runtime.
     */
    NATIVE,

    /**
     * Unknown or custom platform.
     */
    UNKNOWN,
}
