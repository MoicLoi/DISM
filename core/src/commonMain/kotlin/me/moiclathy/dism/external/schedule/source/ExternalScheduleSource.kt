package me.moiclathy.dism.external.schedule.source

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin

/**
 * 外部日程来源接口。
 *
 * Source 表示一个具体的数据获取入口。
 */
interface ExternalScheduleSource {

    /**
     * 该来源实例的稳定 ID。
     */
    val id: ExternalScheduleSourceId

    /**
     * 该来源属于哪一种来源类型。
     */
    val origin: ExternalScheduleOrigin

    /**
     * 获取外部日程。
     */
    fun fetch(
        input: ExternalScheduleSourceInput,
    ): ExternalScheduleSourceResult
}
