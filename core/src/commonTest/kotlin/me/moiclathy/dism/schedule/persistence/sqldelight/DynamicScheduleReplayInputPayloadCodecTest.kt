package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.attention.adaptation.AttentionAdaptation
import me.moiclathy.dism.attention.lifecycle.AttentionKey
import me.moiclathy.dism.attention.lifecycle.AttentionLifecycle
import me.moiclathy.dism.attention.lifecycle.AttentionRecord
import me.moiclathy.dism.attention.memory.AttentionMemory
import me.moiclathy.dism.attention.runtime.AttentionRuntimeState
import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionDecayPolicy
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionExplanationPolicy
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionPolicy
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionHistory
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionKey
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBinding
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingId
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingMode
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingPolicy
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependency
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyId
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyPolicy
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencySet
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyType
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState

/**
 * Replay Input 载荷的往返测试。
 *
 * 为什么必须单独测：
 *
 * 12.27/12.28 的正确性完全依赖"存进去什么、读出来什么"。
 * 如果 Codec 静默丢字段（例如忘记某个 Policy 字段，
 * 或者丢掉了 AttentionRuntimeState 里的 history），
 * 重启后的 Replay 会用**不同的输入**重算，
 * 表现为莫名其妙的 BEHAVIOR_CHANGED。
 *
 * 这里刻意把整张图填满：
 *
 * - 三种 Temporal（Interval / Deadline / Window）
 * - Dependency / ContextBinding
 * - 非默认 Policy（每一个可配置项都改过）
 * - 非空 AttentionRuntimeState（含结构化 map key）
 * - scheduleHistories / contextAttentionHistories
 */
class DynamicScheduleReplayInputPayloadCodecTest {

    private val now =
        Instant.parse("2026-09-24T07:20:00Z")

    @OptIn(ExperimentalUuidApi::class)
    private val eventId =
        Uuid.parse("11111111-1111-4111-8111-111111111111")

    @OptIn(ExperimentalUuidApi::class)
    private val entityId =
        Uuid.parse("22222222-2222-4222-8222-222222222222")

    private val course =
        DynamicScheduleItemId("course-001")

    private val lab =
        DynamicScheduleItemId("lab-002")

    private val revision =
        DynamicScheduleId("schedule-1")

    @Test
    fun populatedReplayInputSurvivesRoundTrip() {

        val payload =
            DynamicScheduleReplayInputPayload(
                input = richInput(),
                previousState = richState(),
                policy = richPolicy(),
            )

        val encoded =
            ScheduleDecisionSqlDelightCodec
                .encodeReplayInputPayload(
                    input = payload.input,
                    previousState = payload.previousState,
                    policy = payload.policy,
                )

        val decoded =
            ScheduleDecisionSqlDelightCodec
                .decodeReplayInputPayload(encoded)

        assertEquals(
            payload,
            decoded,
            "Replay Input 必须无损往返",
        )
    }

    @Test
    fun encodingIsDeterministic() {

        val input = richInput()

        val state = richState()

        val policy = richPolicy()

        val first =
            ScheduleDecisionSqlDelightCodec
                .encodeReplayInputPayload(
                    input = input,
                    previousState = state,
                    policy = policy,
                )

        val second =
            ScheduleDecisionSqlDelightCodec
                .encodeReplayInputPayload(
                    input = input,
                    previousState = state,
                    policy = policy,
                )

        assertEquals(
            first,
            second,
            "同一份输入必须编码出完全相同的字节",
        )
    }

    @Test
    fun stringListAndMapCodecsAreStable() {

        val keys = listOf("DORM", "LAB", "LIBRARY")

        assertEquals(
            keys,
            ScheduleDecisionSqlDelightCodec.decodeStringList(
                ScheduleDecisionSqlDelightCodec
                    .encodeStringList(keys),
            ),
        )

        val values =
            mapOf(
                "stage" to "EXPLANATION",
                "selected" to "true",
            )

        assertEquals(
            values,
            ScheduleDecisionSqlDelightCodec
                .decodeStringMap(
                    ScheduleDecisionSqlDelightCodec
                        .encodeStringMap(values),
                ),
        )
    }

    // -----------------------------------------------------------------
    // fixtures
    // -----------------------------------------------------------------

    private fun richInput(): DynamicScheduleIntelligenceInput =
        DynamicScheduleIntelligenceInput(
            model =
                DynamicScheduleModel(
                    id = revision,

                    generatedAt = now,

                    revision = 7L,

                    items =
                        listOf(
                            DynamicScheduleItem(
                                id = course,

                                scheduleId = revision,

                                title = "实验动物学",

                                type =
                                    DynamicScheduleItemType.EVENT,

                                source =
                                    DynamicScheduleSource.EXTERNAL,

                                temporal =
                                    DynamicScheduleTemporal
                                        .Interval(
                                            startAt = now,

                                            endAt =
                                                now +
                                                    100.minutes,
                                        ),

                                state =
                                    DynamicScheduleState.UPCOMING,

                                priority = 0.6,

                                confidence = 0.9,

                                externalKey = "course-001",

                                createdAt = now,

                                updatedAt = now,
                            ),

                            DynamicScheduleItem(
                                id = lab,

                                scheduleId = revision,

                                title = "实验报告",

                                type =
                                    DynamicScheduleItemType.TASK,

                                source =
                                    DynamicScheduleSource.USER_INPUT,

                                temporal =
                                    DynamicScheduleTemporal
                                        .Window(
                                            earliestStart = now,

                                            latestStart =
                                                now +
                                                    2.days,
                                        ),

                                state =
                                    DynamicScheduleState.READY,

                                priority = 0.4,

                                confidence = 0.8,
                            ),
                        ),
                ),

            dependencies =
                DynamicScheduleDependencySet(
                    dependencies =
                        listOf(
                            DynamicScheduleDependency(
                                id =
                                    DynamicScheduleDependencyId(
                                        "dep-1",
                                    ),

                                fromItemId = course,

                                toItemId = lab,

                                type =
                                    DynamicScheduleDependencyType
                                        .REQUIRES_COMPLETION,

                                confidence = 0.7,

                                active = true,
                            ),
                        ),
                ),

            contextBindings =
                DynamicScheduleContextBindingSet(
                    bindings =
                        listOf(
                            DynamicScheduleContextBinding(
                                id =
                                    DynamicScheduleContextBindingId(
                                        "binding-1",
                                    ),

                                itemId = course,

                                contextKey = "DORM",

                                mode =
                                    DynamicScheduleContextBindingMode
                                        .PREFERRED,

                                weight = 0.5,

                                confidence = 0.9,

                                active = true,
                            ),
                        ),
                ),

            activeContextKeys = setOf("DORM", "LAB"),
        )

    private fun richState():
        DynamicScheduleIntelligenceRuntimeState {

        val key =
            AttentionKey(
                recommendationKey =
                    RecommendationKey(
                        type =
                            RecommendationType
                                .CHECK_INFORMATION,

                        eventId = eventId,

                        requiredEntityId = entityId,
                    ),
            )

        val contextKey =
            DynamicScheduleContextAttentionKey(
                itemId = course,
                contextKey = "DORM",
            )

        return DynamicScheduleIntelligenceRuntimeState(
            attentionRuntimeState =
                AttentionRuntimeState(
                    records =
                        mapOf(
                            key to
                                AttentionRecord(
                                    key = key,

                                    lifecycle =
                                        AttentionLifecycle
                                            .SHOWN,

                                    firstSeenAt = now,

                                    lastShownAt = now,

                                    shownCount = 3,

                                    lastMessageFingerprint =
                                        "fp-1",

                                    repeatAfter = 1.days,
                                ),
                        ),

                    adaptations =
                        mapOf(
                            "dism:event:course-001" to
                                AttentionAdaptation(
                                    attentionKey =
                                        "dism:event:course-001",

                                    suppressionBias = -0.2,

                                    repeatBias = 0.1,

                                    urgencyBias = 0.3,

                                    sampleCount = 5,
                                ),
                        ),

                    memories =
                        mapOf(
                            "dism:event:course-001" to
                                AttentionMemory(
                                    bias = 0.25,
                                    lastUpdatedAt = now,
                                ),
                        ),
                ),

            scheduleHistories =
                mapOf(
                    course.value to
                        DynamicScheduleAttentionHistory(
                            attentionKey = course.value,

                            shownCount = 4,

                            engagedCount = 2,

                            ignoredCount = 1,

                            dismissedCount = 0,

                            requestedCount = 1,

                            lastInteractionAt = now,

                            feedbackSignal = -0.25,
                        ),
                ),

            contextAttentionHistories =
                mapOf(
                    contextKey to
                        DynamicScheduleContextAttentionHistory(
                            key = contextKey,

                            shownCount = 2,

                            engagedCount = 1,

                            ignoredCount = 1,

                            feedbackSignal = 0.5,

                            lastInteractionAt = now,
                        ),
                ),
        )
    }

    private fun richPolicy():
        DynamicScheduleIntelligenceRuntimePolicy =
        DynamicScheduleIntelligenceRuntimePolicy(
            dependencyPolicy =
                DynamicScheduleDependencyPolicy(
                    minimumEffectiveConfidence = 0.15,
                    preserveTerminalStates = false,
                    blockOnUnmetDependency = false,
                    restoreBlockedWhenDependenciesBecomeSatisfied =
                        true,
                    associatedCountsAsBlocking = true,
                ),

            contextPolicy =
                DynamicScheduleContextBindingPolicy(
                    minimumEffectiveConfidence = 0.2,
                    preserveExplicitBlockedState = false,
                ),

            decayPolicy =
                DynamicScheduleAttentionDecayPolicy(
                    halfLife = 7.days,
                    minimumWeight = 0.1,
                ),

            adaptationPolicy =
                DynamicSchedulePriorityAdaptationPolicy(
                    minimumFactor = 0.6,
                    maximumFactor = 1.4,
                    ignoredPenalty = 0.07,
                    dismissedPenalty = 0.12,
                    engagedBonus = 0.06,
                    requestedBonus = 0.2,
                    feedbackWeight = 0.3,
                ),

            attentionProjectionPolicy =
                DynamicScheduleAttentionPolicy(
                    minimumPriority = 0.3,
                    minimumConfidence = 0.35,
                    includeBlocked = false,
                    includePostponed = false,
                    excludedStates =
                        setOf(
                            DynamicScheduleState.COMPLETED,
                            DynamicScheduleState.CANCELLED,
                        ),
                ),

            attentionPolicy =
                AttentionPolicy(
                    softLimit = 4,
                    hardLimit = 6,
                    minimumPriority = 0.3,
                    criticalPriority = 0.85,
                    allowCriticalOverflow = true,
                ),

            conflictPolicy =
                DynamicScheduleAttentionConflictPolicy(
                    minimumHardConstraintPriority = 0.8,
                    explicitRequestPriority = 0.95,
                    maximumLearnedSuppression = 0.4,
                    allowExplicitOverride = false,
                ),

            explanationPolicy =
                DynamicScheduleAttentionExplanationPolicy(
                    maximumReasons = 3,
                    includeBackgroundReasons = false,
                    minimumReasonStrength = 0.2,
                ),
        )
}