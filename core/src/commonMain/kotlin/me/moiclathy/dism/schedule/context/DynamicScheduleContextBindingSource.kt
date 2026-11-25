package me.moiclathy.dism.schedule.context

enum class DynamicScheduleContextBindingSource {
    /**
     * 用户直接提供。
     */
    USER_INPUT,

    /**
     * 由语义引擎或其他规则推导。
     */
    DERIVED,

    /**
     * 来自外部数据源。
     */
    EXTERNAL,

    /**
     * 系统内部产生。
     */
    SYSTEM,
}
