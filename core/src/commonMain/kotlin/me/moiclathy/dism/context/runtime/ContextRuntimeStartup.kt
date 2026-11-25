package me.moiclathy.dism.context.runtime

/**
 * Context Runtime 启动恢复入口。
 */
class ContextRuntimeStartup(
    private val persistence:
        ContextRuntimePersistence,

    private val recovery:
        ContextRuntimeRecovery =
        ContextRuntimeRecovery(),
) {

    fun restore(): ContextRuntimeState {

        val events =
            persistence.loadEvents()

        return recovery.recover(
            events = events,
        )
    }

    fun persist(
        events: Iterable<ContextRuntimeEvent>,
    ) {
        persistence.persist(events)
    }
}
