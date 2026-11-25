package me.moiclathy.dism.context.runtime

/**
 * Context Runtime Persistence。
 *
 * 当前阶段只规定边界，
 * 不绑定 SQLDelight。
 *
 * 后续可以像 7.13 Attention Persistence 一样，
 * 再增加 SQLDelight Adapter。
 */
interface ContextRuntimePersistence {

    fun persist(
        events: Iterable<ContextRuntimeEvent>,
    )

    fun loadEvents(): List<ContextRuntimeEvent>
}
