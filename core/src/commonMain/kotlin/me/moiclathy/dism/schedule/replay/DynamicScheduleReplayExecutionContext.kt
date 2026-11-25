package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant

/**
 * 13.15.2
 *
 * 一次 Replay 的**执行上下文**。
 *
 * ### 为什么必须有这个类型
 *
 * 在此之前 `replayedAt` 实际用的是 `replayInput.evaluatedAt`，
 * 也就是把
 *
 * ```text
 * 历史发生时间
 * ```
 *
 * 冒充成
 *
 * ```text
 * 这次 Replay 执行时间
 * ```
 *
 * 真实情况可能是：
 *
 * ```text
 * 2026-09-24 08:20   历史 Decision
 * 2026-09-25 14:37   用户点击 Replay
 * ```
 *
 * 两者混在一起，UI 就无法回答"这条历史是什么时候被重新检查过的"。
 *
 * ### [executorVersion] 不是历史 Engine Version
 *
 * ```text
 * historicalEngineVersion = "13.1"    历史当时是谁算的
 * executorVersion         = "13.15"   现在是谁在跑
 * ```
 *
 * 这个差异恰恰是 Replay 最有价值的信息之一。
 *
 * ### 时间从哪里来
 *
 * **不是** Engine 内部偷取 `Clock.System.now()`。
 *
 * Core 必须保持：
 *
 * ```text
 * f(input, previous, now) = result
 * ```
 *
 * 而不是：
 *
 * ```text
 * f(input, previous, Clock.System.now())
 * ```
 *
 * 否则同一个 ReplayInput 在不同机器时间下会得到不同结果，
 * 12.27–12.29 建立的 Deterministic Replay 就白做了。
 *
 * 所以时间由调用方（Replay Service / Batch / Session）决定并往下传。
 */
data class DynamicScheduleReplayExecutionContext(
    val replayedAt: Instant,

    val executorVersion: String,
) {

    init {
        require(executorVersion.isNotBlank()) {
            "executorVersion must not be blank"
        }
    }
}
