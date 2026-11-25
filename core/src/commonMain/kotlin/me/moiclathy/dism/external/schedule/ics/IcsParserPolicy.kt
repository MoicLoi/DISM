package me.moiclathy.dism.external.schedule.ics

/**
 * ICS Parser 基础策略。
 *
 * 11.4 只处理单个 VEVENT。
 * RRULE / RDATE 等递归规则留给 11.6。
 */
data class IcsParserPolicy(
    val requireVersion: Boolean = true,
    val requireEventUid: Boolean = false,
    val requireSummary: Boolean = true,
    val allowMissingDateTime: Boolean = true,
    val allowDateOnly: Boolean = true,
) {

    init {
        if (!allowDateOnly) {
            require(
                allowMissingDateTime
            ) {
                "allowDateOnly=false requires explicit datetime handling."
            }
        }
    }
}
