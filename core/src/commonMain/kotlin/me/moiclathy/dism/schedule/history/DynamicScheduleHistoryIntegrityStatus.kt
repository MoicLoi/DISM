package me.moiclathy.dism.schedule.history

/**
 * 13.7
 *
 * 一条历史决策的完整性状态。
 *
 * 刻意把 [DEGRADED] 和 [BROKEN] 分开：
 *
 * > "数据缺了一部分"和"数据互相矛盾"是两个完全不同的问题。
 *
 * - [DEGRADED]：历史还能用，只是少了某一部分
 *   （例如没有 ReplayInput，所以无法重放，但"当时怎么看"还在）；
 * - [BROKEN]：数据自相矛盾
 *   （例如 Snapshot 说这是 item-1，Trace 说是 item-2），
 *   这时候任何基于它的推理都不可信。
 */
enum class DynamicScheduleHistoryIntegrityStatus {

    /**
     * Snapshot / Trace / ReplayInput 全部存在且元数据一致。
     */
    VALID,

    /**
     * 数据存在，但部分历史信息已经缺失。
     */
    DEGRADED,

    /**
     * 存在明确的结构或引用矛盾。
     */
    BROKEN,
}
