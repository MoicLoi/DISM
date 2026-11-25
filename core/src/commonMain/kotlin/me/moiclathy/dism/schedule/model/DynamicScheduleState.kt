package me.moiclathy.dism.schedule.model

import me.moiclathy.dism.api.DismPublicApi
@DismPublicApi
enum class DynamicScheduleState {
    UNSCHEDULED,
    UPCOMING,
    READY,
    ACTIVE,
    BLOCKED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    POSTPONED,
}
