package me.moiclathy.dism.evidence

enum class EvidenceType {

    /**
     * 用户直接明确表达的事实。
     *
     * 例如：
     * “充电宝放宿舍了”
     */
    EXPLICIT_STATEMENT,

    /**
     * 用户完成某个 Checklist 项所产生的证据。
     *
     * 例如：
     * ✓ 把充电宝放回宿舍
     */
    CHECKLIST_COMPLETION,

    /**
     * 从外部日程源导入的信息。
     *
     * 例如：
     * 喜鹊儿课程表、ICS、学校系统。
     */
    SCHEDULE_IMPORT,

    /**
     * 用户在应用中的直接行为。
     *
     * 例如：
     * 确认某个提醒、选择某条路线。
     */
    USER_ACTION,

    /**
     * 系统基于已有 Evidence 推导出的结果。
     *
     * 注意：
     * 推理结果不能和用户明确陈述拥有同等可信等级。
     */
    INFERENCE,
}