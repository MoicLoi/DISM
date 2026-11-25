package me.moiclathy.dism.external.schedule.event

import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry

/**
 * External Schedule → Event Draft 编译器。
 *
 * 这是 11.x 与 10.x 的边界。
 */
interface ExternalScheduleEventCompiler {

    fun compile(
        schedule: ExternalSchedule,
        policy: ExternalScheduleEventCompilationPolicy =
            ExternalScheduleEventCompilationPolicy(),
    ): ExternalScheduleEventCompilationResult

    fun compileEntry(
        entry: ExternalScheduleEntry,
        policy: ExternalScheduleEventCompilationPolicy =
            ExternalScheduleEventCompilationPolicy(),
    ): ExternalScheduleEventCompilationResult
}
