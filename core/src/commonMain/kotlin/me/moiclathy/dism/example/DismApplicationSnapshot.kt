package me.moiclathy.dism.example

import me.moiclathy.dism.runtime.RuntimeSnapshot

/**
 * 15.12：示例里应用层的稳定返回类型。
 *
 * 它存在的意义是让示例**不必**把 [RuntimeSnapshot] 直接
 * 当作应用层模型使用：
 *
 * ```text
 * RuntimeSnapshot   运行时视图（可能随内部实现变化）
 *        ↓
 * DismApplicationSnapshot   应用自己的模型（随应用演进）
 * ```
 *
 * 示例规模很小，所以这里只加了 `apiVersion`；
 * 真实应用通常还会在这里做脱敏、投影、缓存等处理。
 */
data class DismApplicationSnapshot(
    val apiVersion: String,
    val snapshot: RuntimeSnapshot,
) {

    /**
     * 当前世界里有多少条仍活动的关系状态。
     *
     * 应用视图提供这种"算好的数字"，是为了让 UI 层
     * 不必自己去理解运行时模型的形状。
     */
    val activeRelationCount: Int
        get() = snapshot.relationStates.size
}
