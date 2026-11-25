package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * 确定性的 Time-flow Runtime Recovery。
 *
 * Recovery 策略：
 *
 * 1. 读取全部持久化 Runtime 事件
 * 2. 找到最后一条事件
 * 3. 使用最后一条事件恢复 Runtime State
 * 4. 不重新执行历史 Event
 * 5. 不重新生成 Alarm
 */
class DeterministicEventTimeRuntimeRecovery(
    private val persistence: EventTimeRuntimePersistence,
) : EventTimeRuntimeRecovery {

    override fun recover():
        EventTimeRuntimeRecoveryResult {

        val history =
            persistence.all()

        val latest =
            history.lastOrNull()

        if (latest == null) {

            return EventTimeRuntimeRecoveryResult(
                recovered =
                    false,

                state =
                    EventTimeRuntimeState.STOPPED,

                restoredAt =
                    Instant.fromEpochMilliseconds(0),

                restoredFromEventId =
                    null,

                processedEventCount =
                    0,

                generatedAlarmCount =
                    0,

                historySize =
                    0,
            )
        }

        return EventTimeRuntimeRecoveryResult(
            recovered =
                true,

            state =
                latest.runtimeState,

            restoredAt =
                latest.evaluatedAt,

            restoredFromEventId =
                latest.id,

            processedEventCount =
                latest.processedEventCount,

            generatedAlarmCount =
                latest.generatedAlarmCount,

            historySize =
                history.size,
        )
    }
}
