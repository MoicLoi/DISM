package me.moiclathy.dism.external.schedule.event

/**
 * External Schedule Event Factory。
 *
 * T = DISM 当前 Event 类型。
 *
 * 这个接口是 11.9 和 10.x Event Domain 的正式连接点。
 */
interface ExternalScheduleEventFactory<T> {

    fun create(
        input: ExternalScheduleEventFactoryInput,
    ): ExternalScheduleEventFactoryResult<T>
}
