package me.moiclathy.dism.schedule.context

enum class DynamicScheduleContextBindingMode {
    /**
     * 当前情境必须满足，否则该日程项不能正常进入 READY。
     */
    REQUIRED,

    /**
     * 当前情境越匹配，该日程项越相关。
     */
    PREFERRED,

    /**
     * 当前情境存在时，该日程项应被抑制。
     */
    EXCLUDED,
}
