package me.moiclathy.dism.schedule.context

/**
 * 14.7
 *
 * ```text
 * Context Match → Context Priority Influence
 * ```
 */
interface DynamicScheduleContextPriorityAdapter {

    fun adapt(
        match: DynamicScheduleContextMatch,
        policy: DynamicScheduleContextPriorityPolicy =
            DynamicScheduleContextPriorityPolicy(),
    ): DynamicScheduleContextPriorityInfluence
}
