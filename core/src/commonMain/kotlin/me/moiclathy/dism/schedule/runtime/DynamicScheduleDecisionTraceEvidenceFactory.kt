package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceEvidence

/**
 * 13.2
 *
 * Runtime 侧负责把 [DynamicScheduleIntelligenceResult]
 * 投影成 Trace 需要的 Evidence。
 *
 * ```text
 * Runtime
 *   │
 *   ├── IntelligenceResult
 *   │
 *   ├── 提取 Decision Evidence
 *   ▼
 * DynamicScheduleDecisionTraceEvidence
 *   │
 *   ▼
 * TraceAssembler
 *   │
 *   ▼
 * DecisionTrace
 * ```
 *
 * 这样做的好处是：
 *
 * - Trace 包完全不知道 Runtime 类型的存在；
 * - "哪些 key 进了最终选择"这种 Runtime 内部知识
 *   只在 Runtime 侧被解释一次，而不是泄漏到 Trace 里。
 */
object DynamicScheduleDecisionTraceEvidenceFactory {

    fun from(
        item: DynamicScheduleItem,

        intelligence:
            DynamicScheduleIntelligenceResult,

        now: Instant,
    ): DynamicScheduleDecisionTraceEvidence {

        val selection =
            intelligence
                .attentionRuntime
                .selection

        return DynamicScheduleDecisionTraceEvidence(
            item = item,

            dependencyResult =
                intelligence.dependencyResult,

            contextResult =
                intelligence.contextResult,

            priorityAdaptation =
                intelligence.priorityAdaptation,

            attentionProjection =
                intelligence.attentionProjection,

            /*
             * key 可能为 null：identity resolver 在
             * 无法解析 item 身份时会返回 null。
             * 这类候选没有稳定身份，因此不可能"按 key 被选中"，
             * 直接排除，而不是塞一个 null 进去。
             */
            attentionSelectedKeys =
                selection
                    .selected
                    .mapNotNull { it.key }
                    .toSet(),

            attentionSuppressedKeys =
                selection
                    .suppressed
                    .mapNotNull { it.key }
                    .toSet(),

            conflict =
                intelligence
                    .attentionConflicts[item.id],

            explanation =
                intelligence
                    .attentionExplanations[item.id],

            /*
             * 与 TraceAssembler 原来的口径一致：
             * OUTPUT 阶段记录的是"进入投影前"的 revision。
             *
             * 注意不要在这里改成 projectedModel.revision，
             * 那会改变已落盘 Trace 的内容。
             */
            scheduleRevision =
                intelligence
                    .inputModel
                    .revision,

            now = now,
        )
    }
}
