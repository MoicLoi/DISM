package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

class DynamicScheduleDecisionRuntimeFacade(
    private val runtime:
        DynamicScheduleIntelligenceRuntime,

    private val persistenceBridge:
        DynamicScheduleDecisionPersistenceBridge,
) {

    suspend fun evaluateAndPersist(
        input:
            DynamicScheduleIntelligenceInput,

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        now: Instant,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),

        engineVersion: String = "12.23",
    ): DynamicScheduleDecisionRuntimeEvaluation {

        val intelligence =
            runtime.evaluate(
                input = input,

                previous = previous,

                now = now,

                policy = policy,
            )

        val persistence =
            persistenceBridge.persist(
                DynamicScheduleDecisionPersistenceInput(
                    intelligence =
                        intelligence,

                    /*
                     * 12.26A：把 Runtime 真正收到的 Input 与 Policy
                     * 一并交给 Persistence Bridge，
                     * Replay 时才不需要从 Result 反推。
                     */
                    input =
                        input,

                    previous =
                        previous,

                    now =
                        now,

                    policy =
                        policy,

                    engineVersion =
                        engineVersion,
                ),
            )

        return DynamicScheduleDecisionRuntimeEvaluation(
            intelligence =
                intelligence,

            persistence =
                persistence,
        )
    }

    /**
     * 16.20：**只评估，不持久化**。
     *
     * 与 [evaluateAndPersist] 跑的是同一个 `runtime.evaluate`，
     * 唯一区别是不写 Snapshot / Trace / ReplayInput。
     *
     * ### 为什么必须有它
     *
     * UI 想"显示当前决策"时，`evaluateAndPersist` 是唯一入口——
     * 于是每次打开界面都会给每个 item 写一条新历史：
     *
     * ```text
     * 打开 1 次    → 每个 item 1 条
     * 打开 100 次  → 每个 item 100 条重复记录
     * ```
     *
     * "评估（纯计算）"与"记录（写历史）"是两个不同的意图。
     * 之前只有一个入口把两者绑在一起，调用方无法只取其一。
     */
    fun preview(
        input:
            DynamicScheduleIntelligenceInput,

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        now: Instant,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ): DynamicScheduleIntelligenceResult =
        runtime.evaluate(
            input = input,
            previous = previous,
            now = now,
            policy = policy,
        )

    fun applyFeedback(
        state:
            DynamicScheduleIntelligenceRuntimeState,

        feedback:
            DynamicScheduleAttentionFeedback,
    ): DynamicScheduleIntelligenceRuntimeState {

        return runtime.applyFeedback(
            state = state,
            feedback = feedback,
        )
    }
}
