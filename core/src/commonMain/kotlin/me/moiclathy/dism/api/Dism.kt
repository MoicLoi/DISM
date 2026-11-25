package me.moiclathy.dism.api

import me.moiclathy.dism.runtime.DismRuntimeApi

/**
 * DISM Public API Facade.
 *
 * App / Desktop / Android / Server / CLI
 * 应优先通过这里获得 Runtime。
 *
 * 不直接暴露 Persistence implementation。
 */
@DismPublicApi
interface Dism {

    /**
     * 当前 Public API 版本。
     */
    val apiVersion:
        String

    /**
     * 当前产品能力。
     */
    val capabilities:
        DismCapabilities

    /**
     * DISM Runtime。
     */
    val runtime:
        DismRuntimeApi
}
