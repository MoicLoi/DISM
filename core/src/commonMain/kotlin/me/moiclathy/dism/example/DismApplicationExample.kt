package me.moiclathy.dism.example

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismCapabilities
import me.moiclathy.dism.runtime.DismRuntimeApi

/**
 * 15.12：面向应用的**最小** DISM 用法示例。
 *
 * ```text
 * Application
 *      │
 *      ▼
 *   DismFactory
 *      │
 *      ▼
 *     Dism
 *      │
 *      ▼
 *  DismRuntimeApi
 *      ├── observe
 *      ├── observeAndEvaluate
 *      ├── queryAt
 *      ├── snapshot
 *      └── dynamicSchedule
 * ```
 *
 * ### 它刻意不出现的东西
 *
 * ```text
 * DismPersistence / DismPersistenceFactory
 * DismRuntimeFactory
 * DatabaseDriverFactory
 * Repository
 * SQLDelight Query
 * Reducer / Propagation / Attention / Adaptation / Replay Engine
 * ```
 *
 * 应用收到的是一个**已经装配好的** [Dism]；
 * 平台如何解析数据库路径不属于示例的职责。
 * 这正是 15.4–15.5 建立那条边界的意义。
 *
 * ### 15.12 暴露出来、但**故意不在本阶段修**的问题
 *
 * 一个只想调 `runtime.snapshot()` 的应用，也必须面对
 * [DismRuntimeApi] 的全部六个方法。
 *
 * ```text
 * 一个 Example Application
 *          ↓
 * 被强迫依赖整个 Runtime Contract
 * ```
 *
 * 这说明 15.x 之后应该继续拆成：
 *
 * ```text
 * Dism
 *  ├── runtime
 *  ├── schedule
 *  ├── state
 *  ├── diagnostics
 *  └── providers
 * ```
 *
 * 规格要求在 15.12 **记录**这个问题而不是顺手修——
 * 因为 15.12 的职责恰恰是让真实消费者把 API 的问题暴露出来。
 */
class DismApplicationExample(
    private val dism: Dism,
) {

    val runtime: DismRuntimeApi
        get() = dism.runtime

    val apiVersion: String
        get() = dism.apiVersion

    val capabilities: DismCapabilities
        get() = dism.capabilities

    /**
     * 应用启动时读一次当前世界状态。
     *
     * 注意这里只用了公共 API：
     * `runtime.snapshot()` 与 `dism.apiVersion`。
     */
    fun start(): DismApplicationSnapshot {

        val snapshot = runtime.snapshot()

        return DismApplicationSnapshot(
            apiVersion = apiVersion,
            snapshot = snapshot,
        )
    }
}
