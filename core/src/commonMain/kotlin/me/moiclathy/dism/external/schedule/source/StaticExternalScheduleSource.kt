package me.moiclathy.dism.external.schedule.source

import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * 静态 External Schedule Source。
 *
 * 用于：
 *
 *     单元测试
 *     E2E
 *     Fixture
 *     Adapter 验证
 *
 * 不访问网络、不读取文件。
 */
class StaticExternalScheduleSource(
    override val id: ExternalScheduleSourceId,
    private val schedule: ExternalSchedule,
) : ExternalScheduleSource {

    override val origin: ExternalScheduleOrigin
        get() =
            schedule.source

    override fun fetch(
        input: ExternalScheduleSourceInput,
    ): ExternalScheduleSourceResult {

        return ExternalScheduleSourceResult.success(
            sourceId =
                id,

            schedule =
                schedule,

            fetchedAt =
                input.requestedAt,
        )
    }
}
