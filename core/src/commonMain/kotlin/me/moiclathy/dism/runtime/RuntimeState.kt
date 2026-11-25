package me.moiclathy.dism.runtime

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.bootstrap.RestoredCoreState
import me.moiclathy.dism.semantic.engine.SemanticStateEngine

/**
 * 启动恢复 + 运行时 Engine 的捆绑视图。
 *
 * 注意：这不再是 App 的主要入口——
 * DismRuntime 构造时即完成恢复并在内部持有同一个 Engine，
 * App 应直接使用 DismRuntime / DismRuntimeApi。
 *
 * 本类型保留用于 Debug / 启动诊断 / 测试 / 开发工具，
 * 需要在同一处同时拿到「恢复快照」与「Engine」的场景。
 */
@DismInternalApi
data class RuntimeState(
    val restored: RestoredCoreState,
    val engine: SemanticStateEngine,
)
