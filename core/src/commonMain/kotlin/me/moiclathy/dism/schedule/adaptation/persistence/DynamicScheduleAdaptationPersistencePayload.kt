package me.moiclathy.dism.schedule.adaptation.persistence

import kotlinx.serialization.Serializable

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation

/**
 * 14.11
 *
 * 落盘时的 JSON 形态。
 *
 * ### 为什么 humanOverride 在 payload 里，而不是单独一列
 *
 * 规格第一版把它拆成了两列：
 *
 * ```text
 * adaptation_json
 * human_override_json
 * ```
 *
 * 但 `adaptation_json` 里本来就已经包含了 override，
 * 于是同一个 override 被存了两份，而且两份还可能不一致
 * （`human_override_json` 那一列实际是把整个 payload 又编了一次）。
 *
 * 简化成单列 `payload_json` 之后：
 *
 * ```text
 * "一条记录" == "一行" == "一次 JSON"
 * ```
 *
 * 读取时不需要再判断"以哪列为准"。
 */
@Serializable
data class DynamicScheduleAdaptationPersistencePayload(
    val adaptation:
        DynamicSchedulePriorityAdaptation,

    val humanOverride:
        DynamicScheduleHumanOverride?,

    val schemaVersion: Long,
)
