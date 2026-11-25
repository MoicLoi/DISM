package me.moiclathy.dism.context

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class ContextEvent(
    val eventId: Uuid,
    val startAt: Instant,
    val endAt: Instant? = null,
)