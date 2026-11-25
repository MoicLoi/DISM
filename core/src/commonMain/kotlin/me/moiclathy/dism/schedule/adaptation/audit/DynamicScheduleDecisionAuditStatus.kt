package me.moiclathy.dism.schedule.adaptation.audit

enum class DynamicScheduleDecisionAuditStatus {

    /**
     * 当前决策仍然有效。
     */
    ACTIVE,

    /**
     * 已经被后续决策取代。
     */
    SUPERSEDED,

    /**
     * 被明确标记为无效。
     */
    INVALIDATED,

    /**
     * 历史决策只有部分审计证据。
     */
    INCOMPLETE,
}
