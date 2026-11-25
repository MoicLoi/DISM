package me.moiclathy.dism.schedule.adaptation.audit

import kotlin.time.Instant

data class DynamicScheduleDecisionAuditEvent(
    val id: String,

    val type:
        DynamicScheduleDecisionAuditEventType,

    val occurredAt:
        Instant,

    /**
     * 产生该事件的对象。
     *
     * 例如：
     *
     * snapshot:xxx
     * trace:xxx
     * replay:xxx
     */
    val subjectId:
        String,

    /**
     * 可选的上游事件。
     */
    val parentEventIds:
        List<String> = emptyList(),

    val message:
        String,

    val metadata:
        Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank())
        require(subjectId.isNotBlank())
        require(message.isNotBlank())
        require(parentEventIds.none { it.isBlank() })
    }
}
