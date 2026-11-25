package me.moiclathy.dism.external.schedule.source

/**
 * 外部日程来源注册表。
 *
 * 负责管理具体 Source Adapter。
 */
class ExternalScheduleSourceRegistry {

    private val sources =
        linkedMapOf<
            ExternalScheduleSourceId,
            ExternalScheduleSource,
        >()

    /**
     * 注册一个 Source。
     */
    fun register(
        source: ExternalScheduleSource,
    ) {

        require(
            !sources.containsKey(source.id)
        ) {
            "External schedule source already registered: ${source.id}"
        }

        sources[source.id] =
            source
    }

    /**
     * 注册多个 Source。
     */
    fun registerAll(
        sources: Iterable<ExternalScheduleSource>,
    ) {
        sources.forEach(::register)
    }

    /**
     * 查找 Source。
     */
    fun find(
        id: ExternalScheduleSourceId,
    ): ExternalScheduleSource? =
        sources[id]

    /**
     * 是否存在。
     */
    fun contains(
        id: ExternalScheduleSourceId,
    ): Boolean =
        id in sources

    /**
     * 当前所有 Source。
     */
    fun all():
        List<ExternalScheduleSource> =
        sources.values.toList()

    /**
     * 移除 Source。
     */
    fun remove(
        id: ExternalScheduleSourceId,
    ): Boolean =
        sources.remove(id) != null

    /**
     * 当前 Source 数量。
     */
    val size: Int
        get() =
            sources.size
}
