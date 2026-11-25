package me.moiclathy.dism.external.schedule.e2e

enum class ExternalScheduleTimeFlowE2EPhase {

    SOURCE,

    PARSE,

    NORMALIZE,

    RECURRENCE,

    ENTITY_BINDING,

    EVENT_DRAFT,

    EVENT_FACTORY,

    PERSISTENCE,

    RECOVERY,

    TIME_FLOW,

    FINAL,
}
