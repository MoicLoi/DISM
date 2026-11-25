package me.moiclathy.dism.schedule.replay

/**
 * 13.14.1
 *
 * 批量历史重放策略。
 *
 * ### `maxConcurrency` 默认 1 是故意的
 *
 * 一次历史重放本质上是：
 *
 * ```text
 * 历史输入
 *   ↓
 * 完整 DynamicSchedule Runtime
 *   ↓
 * Priority / Attention / Conflict / Explanation
 * ```
 *
 * 它不是一次数据库 SELECT。默认开几十个协程去"看起来更快"，
 * 只会给 SQLite 和 CPU 制造没必要的压力，而且难以观察和调试。
 *
 * ### 这个旋钮不是装饰
 *
 * 既然暴露了 `maxConcurrency`，它就**真的生效**：
 * 大于 1 时会真正并发执行（结果仍按输入顺序返回）。
 * 一个设了却没用的旋钮，比没有这个旋钮更糟——
 * 调用方会以为自己拿到了并发能力。
 */
data class DynamicScheduleHistoryReplayBatchPolicy(
    val maxItems: Int = 50,

    /**
     * 同时执行的重放数量。
     *
     * 默认 1：确定、可观察、不给存储层制造并发压力。
     */
    val maxConcurrency: Int = 1,

    /**
     * 单项失败时是否继续执行后续项目。
     *
     * - `true`：失败变成该项的 Batch Result，其余继续（默认）；
     * - `false`：立即终止整个批次，并把异常抛出去。
     */
    val continueOnFailure: Boolean = true,

    /**
     * 重放后是否立即校验。
     */
    val verify: Boolean = false,
) {

    init {
        require(maxItems > 0) {
            "maxItems must be positive"
        }

        require(maxConcurrency > 0) {
            "maxConcurrency must be positive"
        }
    }
}
