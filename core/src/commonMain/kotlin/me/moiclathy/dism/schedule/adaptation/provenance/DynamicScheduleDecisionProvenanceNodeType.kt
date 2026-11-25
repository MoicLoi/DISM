package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.14
 *
 * Adaptive Decision Provenance 中的节点类型。
 *
 * Provenance 不负责产生新的决策，
 * 只负责描述一次决策是由哪些事实和处理阶段形成的。
 */
enum class DynamicScheduleDecisionProvenanceNodeType {

    /**
     * Runtime 实际接收到的输入。
     */
    INPUT,

    /**
     * Schedule Snapshot。
     */
    SNAPSHOT,

    /**
     * Decision Trace。
     */
    TRACE,

    /**
     * Attention History / Memory。
     */
    ATTENTION_MEMORY,

    /**
     * Dynamic Schedule Priority Adaptation。
     */
    PRIORITY_ADAPTATION,

    /**
     * Adaptation Boundary。
     */
    ADAPTATION_BOUNDARY,

    /**
     * Human Override。
     */
    HUMAN_OVERRIDE,

    /**
     * Context Binding / Context Adaptation。
     */
    CONTEXT,

    /**
     * Dependency Resolution。
     */
    DEPENDENCY,

    /**
     * Attention Projection。
     */
    ATTENTION_PROJECTION,

    /**
     * Conflict Resolution。
     */
    CONFLICT,

    /**
     * 最终 Attention Selection。
     */
    ATTENTION_SELECTION,

    /**
     * Replay。
     */
    REPLAY,

    /**
     * Replay Verification。
     */
    VERIFICATION,

    /**
     * 最终输出。
     */
    OUTPUT,
}
