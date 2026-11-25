package me.moiclathy.dism.event.model

/**
 * DISM Dynamic Event 基础模型。
 *
 * Event 表示：
 *
 * “现实中的一件具有语义身份和时间信息的事情。”
 *
 * 10.1 只建立 Event Model。
 *
 * State / Dependency / Duration / Slack /
 * Alarm / Replanning 在后续阶段实现。
 */
data class Event(
    val id: EventId,

    /**
     * 人类可读名称。
     */
    val title: String,

    /**
     * Event 类型。
     */
    val type: EventType,

    /**
     * 时间语义。
     */
    val temporal: EventTemporal =
        EventTemporal.Unscheduled,

    /**
     * 来源。
     */
    val source: EventSource =
        EventSource.UNKNOWN,

    /**
     * 可选外部 ID。
     *
     * 例如：
     *
     * 喜鹊儿课程 ID。
     */
    val externalId: String? = null,

    /**
     * 扩展元数据。
     */
    val metadata: EventMetadata =
        EventMetadata(),
) {

    init {
        require(title.isNotBlank()) {
            "Event title must not be blank."
        }

        if (externalId != null) {
            require(externalId.isNotBlank()) {
                "externalId must not be blank."
            }
        }
    }
}
