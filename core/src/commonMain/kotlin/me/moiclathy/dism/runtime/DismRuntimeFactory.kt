package me.moiclathy.dism.runtime

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.DismPersistenceFactory
import me.moiclathy.dism.schedule.engine.DefaultDynamicScheduleEngineRegistry
import me.moiclathy.dism.schedule.engine.DynamicScheduleEngineRegistry

/**
 * Runtime implementation factory.
 *
 * ### 15.4：Runtime 只真正依赖 Persistence，不再依赖 PersistenceFactory
 *
 * 旧形态：
 *
 * ```text
 * DismRuntimeFactory
 *     ↓ 必须
 * DismPersistenceFactory
 *     ↓
 * DatabaseDriverFactory / SQLDelight
 * ```
 *
 * 新形态：
 *
 * ```text
 * DismRuntimeFactory
 *     ↓ 只依赖
 * DismPersistence
 * ```
 *
 * 两种装配方式：
 *
 * ```text
 * 1. 已经拥有 Persistence
 *      DismPersistence
 *          ↓
 *      DismRuntimeFactory(persistence)
 *
 * 2. 旧平台装配路径
 *      DismPersistenceFactory
 *          ↓
 *      DismRuntimeFactory(persistenceFactory)
 * ```
 *
 * 第二条路径只保留给**迁移期内部代码**，
 * 它最终会被 15.5 的 Configuration / Bootstrap 取代。
 *
 * ### 为什么用 `persistenceProvider`
 *
 * [DismRuntime] 的构造会立刻恢复世界状态。
 * 用一个 `() -> DismPersistence` 而不是直接持有实例，
 * 是为了让"装配"和"启动"在时间上分开：
 * `create()` 被调用时才真正打开数据库。
 */
@DismInternalApi
class DismRuntimeFactory private constructor(
    private val persistenceProvider: () -> DismPersistence,

    private val engineRegistry:
        DynamicScheduleEngineRegistry =
        DefaultDynamicScheduleEngineRegistry.create(),
) {

    /**
     * 用已经准备好的 Persistence Ports 装配。
     */
    internal constructor(
        persistence: DismPersistence,

        engineRegistry:
            DynamicScheduleEngineRegistry =
            DefaultDynamicScheduleEngineRegistry.create(),
    ) : this(
        persistenceProvider = { persistence },
        engineRegistry = engineRegistry,
    )

    /**
     * 迁移期兼容入口。
     *
     * 新代码请先构造 [DismPersistence]，再用 `persistence` 重载。
     */
    internal constructor(
        persistenceFactory: DismPersistenceFactory,

        engineRegistry:
            DynamicScheduleEngineRegistry =
            DefaultDynamicScheduleEngineRegistry.create(),
    ) : this(
        persistenceProvider = {
            persistenceFactory.create()
        },
        engineRegistry = engineRegistry,
    )

    fun create(): DismRuntime =
        DismRuntime(
            persistence = persistenceProvider(),
            engineRegistry = engineRegistry,
        )
}
