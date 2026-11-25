package me.moiclathy.dism.external.schedule.ics

/**
 * ICS 文档级解析结果。
 */
data class IcsParseResult(
    val events: List<List<IcsProperty>>,
    val calendarName: String? = null,
    val version: String? = null,
    val warnings: List<String> = emptyList(),
) {

    init {
        require(
            warnings.all { it.isNotBlank() }
        ) {
            "ICS warnings must not be blank."
        }
    }

    val eventCount: Int
        get() =
            events.size
}
