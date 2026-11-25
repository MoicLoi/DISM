package me.moiclathy.dism.event.dependency

/**
 * Event Dependency Engine。
 *
 * 负责：
 *
 * - 添加依赖
 * - 删除依赖
 * - 验证依赖图
 * - 检测循环
 *
 * 不负责：
 *
 * - 时间计算
 * - Alarm
 * - Replanning
 * - Attention
 * - Persistence
 */
interface EventDependencyEngine {

    fun addDependency(
        graph: EventDependencyGraph,
        dependency: EventDependency,
    ): EventDependencyValidationResult

    fun removeDependency(
        graph: EventDependencyGraph,
        dependency: EventDependency,
    ): EventDependencyGraph

    fun validate(
        graph: EventDependencyGraph,
    ): EventDependencyValidationResult
}
