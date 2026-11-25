package me.moiclathy.dism.schedule.history

/**
 * 13.9
 *
 * Repair 执行权限。
 *
 * 默认是 [PLAN_ONLY]：
 *
 * > App 不会因为"调了一次 Repair API"就突然开始改数据库。
 */
enum class DynamicScheduleHistoryRepairPermission {

    /**
     * 只生成 Repair Plan，不允许修改任何持久化数据。
     */
    PLAN_ONLY,

    /**
     * 允许执行明确标记为安全的操作
     * （当前只有孤儿 ReplayInput 清理）。
     */
    SAFE_ONLY,

    /**
     * 允许执行全部**已实现**的 Repair Action。
     *
     * 当前版本实际上仍然不会允许任何覆盖原始历史的操作，
     * 这个枚举主要为未来扩展保留。
     */
    ALL_SUPPORTED,
}
