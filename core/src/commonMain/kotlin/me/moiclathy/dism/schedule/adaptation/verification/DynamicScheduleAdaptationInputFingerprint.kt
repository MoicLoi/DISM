package me.moiclathy.dism.schedule.adaptation.verification

import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInput
import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInputPayloadCodec
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 14.13
 *
 * 一次 Adaptation Replay 输入的指纹。
 *
 * ### 它不是安全哈希
 *
 * 与 12.29 的 `DynamicScheduleReplayDeterminismFingerprint` 一致，
 * 这里用的是**稳定 Canonical Representation**，不是 SHA-256：
 *
 * ```text
 * Canonical JSON
 *     ↓
 * Fingerprint
 * ```
 *
 * 理由：
 *
 * - KMP commonMain 没有现成的 SHA-256
 * - 它只用于判定"这两份输入是不是同一份"
 * - 真正的持久化 ID 由 13.x 的 Snapshot / Trace 负责
 *
 * ### 关键设计：输入指纹**不包含** policy
 *
 * §十 的分类阶梯把 Input 和 Policy 分成两级：
 *
 * ```text
 * Input 变了？   → INPUT_CHANGED
 * Policy 变了？  → POLICY_CHANGED
 * ```
 *
 * 如果输入指纹把 policy 也算进去，那么"改了 policy"
 * 必然同时改变输入指纹，`INPUT_CHANGED` 会永远抢先命中，
 * `POLICY_CHANGED` 就成了死代码。
 *
 * 所以这里明确划分：
 *
 * ```text
 * from(input)  → 只看 model + histories
 * ofPolicy(p)  → 只看 policy
 * ```
 *
 * ### 为什么必须排序
 *
 * `histories` 是 `Map`，遍历顺序取决于构造方式。
 * 不排序的话，同一份输入在不同进程里可能得到不同指纹，
 * 于是产生 `INPUT_CHANGED` 假警报。
 */
data class DynamicScheduleAdaptationInputFingerprint(
    val value: String,
) {

    override fun toString(): String = value

    companion object {

        /**
         * Model + Attention History 的指纹。
         *
         * **不含 policy**，见类注释。
         */
        fun from(
            input: DynamicScheduleAdaptationReplayInput,
        ): DynamicScheduleAdaptationInputFingerprint {

            val json =
                DynamicScheduleAdaptationReplayInputPayloadCodec.json

            val model =
                json.encodeToString(
                    DynamicScheduleModel.serializer(),
                    input.model,
                )

            val histories =
                input.histories
                    .entries
                    .sortedBy { it.key }
                    .joinToString("|") { (key, history) ->
                        key +
                            "=" +
                            json.encodeToString(
                                me.moiclathy.dism.schedule.adaptation
                                    .DynamicScheduleAttentionHistory
                                    .serializer(),
                                history,
                            )
                    }

            return DynamicScheduleAdaptationInputFingerprint(
                value =
                    "model:$model|histories:$histories",
            )
        }

        /**
         * Policy 自己的指纹。
         */
        fun ofPolicy(
            policy:
                DynamicSchedulePriorityAdaptationPolicy,
        ): DynamicScheduleAdaptationInputFingerprint =
            DynamicScheduleAdaptationInputFingerprint(
                value =
                    "policy:" +
                        DynamicScheduleAdaptationReplayInputPayloadCodec
                            .json
                            .encodeToString(
                                DynamicSchedulePriorityAdaptationPolicy
                                    .serializer(),
                                policy,
                            ),
            )
    }
}
