package me.moiclathy.dism.schedule.adaptation.audit

enum class DynamicScheduleDecisionAuditEventType {

    /**
     * 决策第一次产生。
     */
    CREATED,

    /**
     * Decision Snapshot 被持久化。
     */
    SNAPSHOT_PERSISTED,

    /**
     * Decision Trace 被持久化。
     */
    TRACE_PERSISTED,

    /**
     * Adaptation 被持久化。
     */
    ADAPTATION_PERSISTED,

    /**
     * Human Override 产生。
     */
    HUMAN_OVERRIDE_APPLIED,

    /**
     * Adaptation Boundary 限制了调整。
     */
    ADAPTATION_BOUNDARY_APPLIED,

    /**
     * 历史决策被 Replay。
     */
    REPLAYED,

    /**
     * Replay Verification 执行。
     */
    VERIFIED,

    /**
     * Verification 发现行为变化。
     */
    BEHAVIOR_CHANGED,

    /**
     * 决策被后续版本取代。
     */
    SUPERSEDED,

    /**
     * 决策被明确撤销。
     */
    INVALIDATED,
}
