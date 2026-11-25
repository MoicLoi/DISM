package me.moiclathy.dism.context.source

/**
 * Context Source 注册表。
 *
 * Registry 只负责管理 Source。
 * 不负责：
 * - 冲突解决
 * - Context 推导
 * - Priority
 * - Attention
 */
class ContextSourceRegistry {

    private val sources =
        linkedMapOf<ContextSourceId, ContextSource>()

    /**
     * 注册 Source。
     *
     * 相同 ID 会被拒绝，避免无意覆盖。
     */
    fun register(source: ContextSource) {
        require(sources[source.id] == null) {
            "Context source already registered: ${source.id}"
        }

        sources[source.id] = source
    }

    /**
     * 批量注册。
     */
    fun registerAll(
        values: Iterable<ContextSource>,
    ) {
        values.forEach(::register)
    }

    /**
     * 根据 ID 获取 Source。
     */
    fun find(
        id: ContextSourceId,
    ): ContextSource? {
        return sources[id]
    }

    /**
     * 判断是否存在。
     */
    fun contains(
        id: ContextSourceId,
    ): Boolean {
        return sources.containsKey(id)
    }

    /**
     * 获取全部 Source。
     *
     * 保持注册顺序，方便确定性测试和调试。
     */
    fun all(): List<ContextSource> {
        return sources.values.toList()
    }

    /**
     * 从全部 Source 收集候选 ContextClaim。
     *
     * 注意：
     * 这里仍然不进行 Resolution。
     */
    fun collectAll(
        input: ContextSourceInput,
    ): List<ContextSourceResult> {
        return sources.values.map {
            it.collect(input)
        }
    }

    /**
     * 当前注册数量。
     */
    val size: Int
        get() = sources.size
}
