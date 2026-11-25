package me.moiclathy.dism.external.schedule.e2e

data class ExternalScheduleTimeFlowE2ETrace(
    val steps: List<ExternalScheduleTimeFlowE2EStep>,

    val externalScheduleId: String,

    val externalEntryCount: Int,

    val normalizedEntryCount: Int,

    val eventDraftCount: Int,

    val persistedRecordCount: Int,

    val recoveredRecordCount: Int,

    val mappedEventCount: Int,

    val timeFlowProcessedCount: Int,
) {

    init {
        require(externalScheduleId.isNotBlank()) {
            "externalScheduleId must not be blank"
        }

        require(externalEntryCount >= 0) {
            "externalEntryCount must not be negative"
        }

        require(normalizedEntryCount >= 0) {
            "normalizedEntryCount must not be negative"
        }

        require(eventDraftCount >= 0) {
            "eventDraftCount must not be negative"
        }

        require(persistedRecordCount >= 0) {
            "persistedRecordCount must not be negative"
        }

        require(recoveredRecordCount >= 0) {
            "recoveredRecordCount must not be negative"
        }

        require(mappedEventCount >= 0) {
            "mappedEventCount must not be negative"
        }

        require(timeFlowProcessedCount >= 0) {
            "timeFlowProcessedCount must not be negative"
        }
    }

    val successful: Boolean
        get() =
            steps.isNotEmpty() &&
                steps.all {
                    it.successful
                }
}
