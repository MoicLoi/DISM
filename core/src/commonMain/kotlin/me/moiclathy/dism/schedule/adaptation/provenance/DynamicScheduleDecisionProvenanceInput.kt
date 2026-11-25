package me.moiclathy.dism.schedule.adaptation.provenance

import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAdaptationBoundary
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 14.14 §9 / 14.15
 *
 * 构建 Provenance 所需的全部输入。
 *
 * ### 为什么 14.14 当年没做，现在做了
 *
 * 14.14 规格明确写道"这个文件现在不要创建"，理由是中段引用的
 * [DynamicScheduleAdaptationBoundary]（14.9）、
 * [DynamicScheduleHumanOverride]（14.10）、
 * 以及 14.11–14.13 当时都还只是概念设计。
 *
 * 在本项目里这些已经全部落地并且测试通过，
 * 所以这个前置条件已经满足，可以用完整形态一次性建出来，
 * 而不是先建一个残缺版、14.15 再改一次。
 *
 * ### 关键约束：不要伪造节点
 *
 * 这些字段全部是可选的，**只有调用方真的提供了**
 * 才会生成对应节点。
 *
 * 否则会出现：
 *
 * ```text
 * Provenance:  Human Override → xxx
 * 真实 Runtime: 根本没有这个 Override
 * ```
 *
 * 那会直接破坏 Provenance 的唯一价值：可信。
 */
data class DynamicScheduleDecisionProvenanceInput(
    val snapshot:
        DynamicScheduleDecisionSnapshot,

    val trace:
        DynamicScheduleDecisionTraceRecord?,

    val adaptations:
        List<DynamicSchedulePriorityAdaptation> = emptyList(),

    val generatedAt:
        Instant,

    val engineVersion:
        String,

    /**
     * 14.9 的边界结果。
     */
    val boundary:
        DynamicScheduleAdaptationBoundary? = null,

    /**
     * 14.10 的用户显式覆盖。
     */
    val humanOverride:
        DynamicScheduleHumanOverride? = null,

    /**
     * 这次结果是否来自重放。
     */
    val replayed: Boolean = false,

    /**
     * 14.13 的验证结论（状态名）。
     *
     * 刻意用 `String` 而不是枚举：
     * 14.13 的枚举属于 verification 包，
     * Provenance 不应该因此多一个硬依赖方向。
     */
    val verificationStatus: String? = null,
)
