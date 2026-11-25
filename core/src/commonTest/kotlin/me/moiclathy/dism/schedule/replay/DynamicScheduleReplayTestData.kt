package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionProjector
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionIdentityResolver
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.runtime.DeterministicDynamicScheduleIntelligenceRuntime
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 12.29
 *
 * Replay 测试数据入口。
 *
 * 注意：
 *
 * 这里只提供**真实的**最小历史数据
 * （Model / Input / Policy / Previous State / 时间），
 * 不复制任何生产 Runtime 逻辑，也不制造假的中间结果。
 *
 * Runtime / Projector / IdentityResolver 与生产一致，
 * 只是 identity 解析用固定的确定性 Uuid。
 */
@OptIn(ExperimentalUuidApi::class)
object DynamicScheduleReplayTestData {

    val scheduleId =
        DynamicScheduleId("schedule-1")

    val itemId =
        DynamicScheduleItemId("item-1")

    val evaluatedAt: Instant =
        Instant.parse("2026-09-24T08:00:00Z")

    private val eventId =
        Uuid.parse("00000000-0000-0000-0000-000000000001")

    private val requiredEntityId =
        Uuid.parse("00000000-0000-0000-0000-000000000002")

    /**
     * 与生产同构的 identity resolver：
     * 只是用固定 Uuid 保证确定性。
     */
    fun identityResolver():
        DynamicScheduleAttentionIdentityResolver =
        DynamicScheduleAttentionIdentityResolver { id ->
            if (id == itemId) {
                RecommendationKey(
                    type = RecommendationType.CHECK_INFORMATION,
                    eventId = eventId,
                    requiredEntityId = requiredEntityId,
                )
            } else {
                null
            }
        }

    fun projector():
        DeterministicDynamicScheduleAttentionProjector =
        DeterministicDynamicScheduleAttentionProjector(
            identityResolver = identityResolver(),
        )

    fun runtime():
        DeterministicDynamicScheduleIntelligenceRuntime =
        DeterministicDynamicScheduleIntelligenceRuntime(
            attentionProjector = projector(),
        )

    fun model(): DynamicScheduleModel =
        DynamicScheduleModel(
            id = scheduleId,
            items = listOf(
                DynamicScheduleItem(
                    id = itemId,
                    scheduleId = scheduleId,
                    title = "Bring charger",
                    type = DynamicScheduleItemType.TASK,
                    source = DynamicScheduleSource.DERIVED,
                    state = DynamicScheduleState.READY,
                    priority = 0.6,
                    confidence = 1.0,
                ),
            ),
        )

    fun runtimeInput():
        DynamicScheduleIntelligenceInput =
        DynamicScheduleIntelligenceInput(
            model = model(),
        )

    fun policy():
        DynamicScheduleIntelligenceRuntimePolicy =
        DynamicScheduleIntelligenceRuntimePolicy()

    fun previousState():
        DynamicScheduleIntelligenceRuntimeState =
        DynamicScheduleIntelligenceRuntimeState()

    fun decisionSnapshot():
        DynamicScheduleDecisionSnapshot =
        DynamicScheduleDecisionSnapshot(
            itemId = itemId,

            capturedAt = evaluatedAt,

            scheduleRevision = model().revision,

            engineVersion = ENGINE_VERSION,

            item =
                DynamicScheduleItemSnapshot(
                    itemId = itemId,
                    title = "Bring charger",
                    state = DynamicScheduleState.READY,
                    priority = 0.6,
                    confidence = 1.0,
                ),

            context = DynamicScheduleContextSnapshot(),

            attentionMemory =
                DynamicScheduleAttentionMemorySnapshot(),

            dependency =
                DynamicScheduleDependencySnapshot(
                    requiredSatisfied = true,
                ),

            policy =
                DynamicSchedulePolicySnapshot(
                    minimumAttentionPriority = 0.25,
                    minimumAttentionConfidence = 0.25,
                    minimumHardConstraintPriority = 0.75,
                    explicitRequestPriority = 1.0,
                    maximumLearnedSuppression = 0.5,
                ),

            /*
             * 历史决策当时确实产生了 Explanation，
             * 否则 Verification 会认为
             * "历史无解释 / 重放有解释" 而报 BEHAVIOR_CHANGED。
             */
            traceId = DynamicScheduleDecisionTraceId(),
        )

    fun create():
        DynamicScheduleDecisionReplayInputSnapshot =
        DynamicScheduleDecisionReplayInputSnapshot(
            decisionSnapshot = decisionSnapshot(),

            input = runtimeInput(),

            previousState = previousState(),

            policy = policy(),

            evaluatedAt = evaluatedAt,

            scheduleRevision =
                model().revision,

            engineVersion = ENGINE_VERSION,
        )

    const val ENGINE_VERSION = "12.27"
}
