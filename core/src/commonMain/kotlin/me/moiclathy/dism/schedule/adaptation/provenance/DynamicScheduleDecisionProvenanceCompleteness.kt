package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.15 §八
 *
 * Provenance 的完整程度。
 *
 * ### 为什么不是 `complete: Boolean`
 *
 * 14.14 第一版写的是：
 *
 * ```kotlin
 * complete = trace != null && adaptation != null
 * ```
 *
 * 在真实源码里这会得到一个很尴尬的状态：
 *
 * ```text
 * Snapshot ✓
 * Trace    ✓
 * Adaptation ✗  ← 这一层还没接进来
 * ```
 *
 * 于是只能诚实地报 `complete = false`。
 *
 * 但 `false` 读起来像"Provenance 坏了"，
 * 而真实情况是"**暂时只做到这一步**"。
 * 两件事必须分开表达，所以改成三态。
 */
enum class DynamicScheduleDecisionProvenanceCompleteness {

    /**
     * Trace 与 Adaptation 都在。
     */
    COMPLETE,

    /**
     * 只有其中一部分。
     *
     * 这不是错误状态，只是"还剩一层没接上"。
     */
    PARTIAL,

    /**
     * 除了 Snapshot 自身之外，什么都没有。
     */
    EMPTY,
}
