package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant

/**
 * 13.15.13
 *
 * 一次**批量重放**的执行上下文。
 *
 * ### 为什么批次内统一时间（13.15.14）
 *
 * 如果每条历史都自己取一次当前时间：
 *
 * ```text
 * A = 14:37:01
 * B = 14:37:02
 * C = 14:37:03
 * ```
 *
 * 虽然真实，但在审计上会看起来像**三次互不相关的用户操作**。
 *
 * 而 Batch 本质上是：
 *
 * > **一次用户发起的历史重演操作。**
 *
 * 所以批次内统一使用 [startedAt]：
 *
 * ```text
 * Replay Batch
 * startedAt = 14:37:00
 *   ├── Decision A
 *   ├── Decision B
 *   └── Decision C
 * ```
 *
 * 这同时让批量重放在测试里保持确定性。
 */
data class DynamicScheduleReplayBatchExecutionContext(
    val startedAt: Instant,

    val replayEngineVersion: String,
) {

    init {
        require(replayEngineVersion.isNotBlank()) {
            "replayEngineVersion must not be blank"
        }
    }
}
