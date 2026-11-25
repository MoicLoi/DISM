package me.moiclathy.dism.schedule.adaptation.persistence

import kotlin.time.Clock

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * 确定性持久化服务。
 *
 * ### 关于时钟
 *
 * 13.15 的纪律是"时钟由调用方传入"。这里 [clock] 是可注入的，
 * 默认 `Clock.System`，**不是**在内部直接读系统时间——
 * 测试可以传入固定时钟，产品化走默认。
 */
class DeterministicDynamicScheduleAdaptationPersistenceService(
    private val store:
        DynamicScheduleAdaptationPersistenceStore,

    private val clock:
        Clock = Clock.System,
) : DynamicScheduleAdaptationPersistenceService {

    override suspend fun persist(
        adaptation: DynamicSchedulePriorityAdaptation,
        humanOverride: DynamicScheduleHumanOverride?,
    ): DynamicScheduleAdaptationPersistenceRecord {

        require(
            humanOverride == null ||
                humanOverride.itemId == adaptation.itemId,
        ) {
            "Human override itemId must match adaptation itemId"
        }

        val record =
            DynamicScheduleAdaptationPersistenceRecord(
                itemId = adaptation.itemId,
                adaptation = adaptation,
                humanOverride = humanOverride,
                persistedAt = clock.now(),
            )

        store.save(record)

        return record
    }

    override suspend fun restore(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord? {
        return store.get(itemId)
    }
}
