package me.moiclathy.dism.event.dependency

/**
 * Event Dependency Graph 策略。
 */
data class EventDependencyPolicy(

    /**
     * 是否允许依赖关系形成环。
     *
     * 默认禁止。
     *
     * 例如：
     *
     * A → B
     * B → C
     * C → A
     *
     * 默认认为这是非法依赖图。
     */
    val allowCycles: Boolean = false,

    /**
     * 是否允许重复 Dependency。
     *
     * 默认不允许。
     */
    val allowDuplicateDependencies: Boolean = false,

    /**
     * 是否允许关联关系参与依赖图拓扑计算。
     *
     * ASSOCIATED 本身没有严格顺序，
     * 默认不参与。
     */
    val associatedParticipatesInOrdering: Boolean = false,
)
