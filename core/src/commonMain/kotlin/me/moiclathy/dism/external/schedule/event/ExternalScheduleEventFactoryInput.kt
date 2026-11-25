package me.moiclathy.dism.external.schedule.event

/**
 * 11.9 Event Factory 的输入。
 *
 * 它保留 11.8 产生的稳定 externalKey，
 * 使后续 Event Identity / Diff 可以继续使用。
 */
data class ExternalScheduleEventFactoryInput(
    val draft: ExternalScheduleEventDraft,
) {

    val externalKey: String
        get() = draft.externalKey

    val sourceEntryId: String
        get() = draft.sourceEntryId.value
}
