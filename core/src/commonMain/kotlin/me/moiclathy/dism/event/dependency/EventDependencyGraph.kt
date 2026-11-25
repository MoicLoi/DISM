package me.moiclathy.dism.event.dependency

/**
 * Event Dependency Graph。
 *
 * Graph 本身是不可变的。
 *
 * Node:
 *     Event ID
 *
 * Edge:
 *     EventDependency
 */
data class EventDependencyGraph(
    val dependencies: List<EventDependency> = emptyList(),
) {

    /**
     * 当前图中的所有 Event ID。
     */
    val eventIds: Set<String>
        get() =
            dependencies
                .flatMap {
                    listOf(
                        it.subjectEventId,
                        it.objectEventId,
                    )
                }
                .toSet()

    /**
     * 获取某个 Event 直接依赖的其他 Event。
     */
    fun dependenciesOf(
        eventId: String,
    ): List<EventDependency> =
        dependencies.filter {
            it.subjectEventId == eventId
        }

    /**
     * 获取依赖当前 Event 的其他 Event。
     */
    fun dependentsOf(
        eventId: String,
    ): List<EventDependency> =
        dependencies.filter {
            it.objectEventId == eventId
        }

    /**
     * 判断是否已经存在指定 Dependency。
     */
    fun contains(
        dependency: EventDependency,
    ): Boolean =
        dependencies.contains(dependency)

    /**
     * 添加 Dependency。
     *
     * 返回新的 Graph。
     */
    fun add(
        dependency: EventDependency,
    ): EventDependencyGraph =
        copy(
            dependencies =
                dependencies + dependency,
        )

    /**
     * 删除 Dependency。
     *
     * 返回新的 Graph。
     */
    fun remove(
        dependency: EventDependency,
    ): EventDependencyGraph =
        copy(
            dependencies =
                dependencies - dependency,
        )
}
