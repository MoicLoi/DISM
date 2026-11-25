package me.moiclathy.dism.event.dependency

/**
 * Event Dependency。
 *
 * subjectEventId depends on objectEventId。
 *
 * 例如：
 *
 * "实验课" depends on "到达实验楼"
 *
 * subject = 实验课
 * object  = 到达实验楼
 */
data class EventDependency(
    val subjectEventId: String,
    val objectEventId: String,
    val type: EventDependencyType,
    val reason: String? = null,
) {

    init {
        require(subjectEventId.isNotBlank()) {
            "subjectEventId must not be blank."
        }

        require(objectEventId.isNotBlank()) {
            "objectEventId must not be blank."
        }

        require(subjectEventId != objectEventId) {
            "An event cannot depend on itself."
        }

        if (reason != null) {
            require(reason.isNotBlank()) {
                "reason must not be blank."
            }
        }
    }
}
