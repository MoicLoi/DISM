package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState

/**
 * Replay Input 的持久化载荷。
 *
 * 12.26A 的 [me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot]
 * 里只有 `input` / `previousState` / `policy` 是深层对象图；
 * `decisionSnapshot` 已经在 `schedule_decision_snapshot` 表里结构化保存，
 * 所以这里不再重复存一份，读回时按 `snapshot_id` 关联。
 *
 * 这几个类型都在 commonMain 且带 `@Serializable`，
 * 因此 JVM / Android / iOS / JS / Wasm 共用同一份编解码逻辑。
 */
@Serializable
internal data class DynamicScheduleReplayInputPayload(
    val input: DynamicScheduleIntelligenceInput,

    val previousState:
        DynamicScheduleIntelligenceRuntimeState,

    val policy:
        DynamicScheduleIntelligenceRuntimePolicy,
)

/**
 * 13.3：Json 配置就是 **Codec Contract**，必须和 Codec 绑在一起。
 *
 * 不允许这些配置散落到 Runtime / App / Server / Test，
 * 否则迟早会出现：
 *
 * ```text
 * App JSON configuration ≠ persistence JSON configuration
 *        ↓
 * restart → decode → 不同结构
 *        ↓
 * Replay → BEHAVIOR_CHANGED
 * ```
 *
 * 所以这里是 persistence 层唯一的 Json 实例，
 * 所有 ReplayInput 编解码都共用它。
 *
 * 三项设置都是确定性重放的必要条件：
 *
 * - `allowStructuredMapKeys`
 *   `AttentionRuntimeState.records` 的 key 是
 *   [me.moiclathy.dism.attention.lifecycle.AttentionKey]（data class），
 *   不是 primitive，必须显式允许结构化 map key。
 *
 * - `encodeDefaults`
 *   Policy / State 里大量字段有默认值；
 *   写默认值可以保证"存进去什么、读出来什么"完全一致。
 *
 * - `ignoreUnknownKeys = false`
 *   读到未知字段说明数据库里的格式比当前代码新，
 *   必须显式失败，而不是静默丢字段后继续重放。
 */
internal object ScheduleDecisionPersistenceJson {

    const val VERSION: Long = 1L

    val instance: Json = Json {
        encodeDefaults = true

        allowStructuredMapKeys = true

        ignoreUnknownKeys = false

        prettyPrint = false
    }
}
