package me.moiclathy.dism.schedule.adaptation.persistence

import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * 一次 Adaptive Decision 的长期持久化记录。
 *
 * 注意：
 *
 * 这里保存的是"决策状态"，而不是只保存最终 priority。
 *
 * 这样后续：
 *
 * ```text
 * 14.12 Replay
 * 14.13 Verification
 * 14.14 Provenance
 * ```
 *
 * 都可以知道当时系统为什么得到这个结果。
 *
 * ### 为什么必须同时存 override
 *
 * 只存最终 `effectivePriority` 的话，重新加载后无法区分：
 *
 * ```text
 * "这是自动算出来的 0.9"
 * "这是用户手动设的 0.9"
 * ```
 *
 * 而这两者在产品上必须表现不同——前者可以被后续自动调整，
 * 后者不可以。
 */
data class DynamicScheduleAdaptationPersistenceRecord(
    val itemId: DynamicScheduleItemId,

    /**
     * 自动适配结果。
     */
    val adaptation:
        DynamicSchedulePriorityAdaptation,

    /**
     * 用户明确覆盖。
     *
     * `null` 表示当时没有 Human Override。
     */
    val humanOverride:
        DynamicScheduleHumanOverride? = null,

    /**
     * 记录创建时间。
     */
    val persistedAt: Instant,

    /**
     * 当前 Adaptation Schema 版本。
     */
    val schemaVersion: Long = 1L,
) {

    init {
        require(schemaVersion >= 1L)

        require(
            adaptation.itemId == itemId,
        ) {
            "adaptation itemId must match record itemId"
        }

        require(
            humanOverride == null ||
                humanOverride.itemId == itemId,
        ) {
            "human override itemId must match record itemId"
        }
    }
}
