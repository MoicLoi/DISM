package me.moiclathy.dism.schedule.adaptation.verification

import kotlin.math.abs
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation

/**
 * 14.13
 *
 * 第一版确定性 Adaptation Verification Engine。
 *
 * ### 分类阶梯（§十）
 *
 * 顺序非常重要：
 *
 * ```text
 * Input 变了？    → INPUT_CHANGED
 * Policy 变了？   → POLICY_CHANGED
 * Engine 变了？   → ENGINE_CHANGED
 * 结果不一致？    → BEHAVIOR_CHANGED
 * 否则           → EXACT_MATCH
 * ```
 *
 * 为什么输入 / policy / engine 要排在结果差异之前：
 *
 * ```text
 * 历史 factor = 1.25
 * 重放 factor = 1.10
 * ```
 *
 * 如果只是 policy 被改过，报 `BEHAVIOR_CHANGED` 会把
 * "有人调了参数"说成"算法退化"，这是最误导人的一种错报。
 *
 * 注意：规格里的代码块把 Engine 判在 Policy 之前，
 * 但同一节的分类图把 Policy 判在 Engine 之前。
 * 这里按**分类图**实现（后者是最终定稿的那张图）。
 *
 * ### 差异顺序必须确定
 *
 * 规格原文用 `toSet()` 遍历 itemId，顺序取决于哈希布局。
 * 这里统一 `sortedBy { it.value }`。
 */
class DeterministicDynamicScheduleAdaptationVerificationEngine :
    DynamicScheduleAdaptationVerificationEngine {

    override fun verify(
        historical:
            List<DynamicSchedulePriorityAdaptation>,

        replayed:
            List<DynamicSchedulePriorityAdaptation>,

        historicalEngineVersion: String,

        replayEngineVersion: String,

        historicalPolicyFingerprint: String,

        replayPolicyFingerprint: String,

        policy:
            DynamicScheduleAdaptationVerificationPolicy,

        historicalInputFingerprint: String?,

        replayInputFingerprint: String?,
    ): DynamicScheduleAdaptationVerificationReport {

        val differences =
            mutableListOf<
                DynamicScheduleAdaptationVerificationDifference
            >()

        /*
         * ---------------------------------------------------------
         * Engine Version
         * ---------------------------------------------------------
         */

        val engineChanged =
            historicalEngineVersion !=
                replayEngineVersion

        /*
         * ---------------------------------------------------------
         * Policy
         * ---------------------------------------------------------
         */

        val policyChanged =
            historicalPolicyFingerprint !=
                replayPolicyFingerprint

        /*
         * ---------------------------------------------------------
         * Input
         * ---------------------------------------------------------
         *
         * 两边都给了才比较。
         * 只给一边说明调用方没打算做这一级判断，
         * 不能当成"输入变了"。
         */

        val inputChanged =
            historicalInputFingerprint != null &&
                replayInputFingerprint != null &&
                historicalInputFingerprint !=
                replayInputFingerprint

        /*
         * ---------------------------------------------------------
         * Item Map
         * ---------------------------------------------------------
         */

        val historicalById =
            historical.associateBy {
                it.itemId
            }

        val replayedById =
            replayed.associateBy {
                it.itemId
            }

        val allIds =
            (historicalById.keys + replayedById.keys)
                .sortedBy { it.value }

        allIds.forEach { itemId ->

            val historicalItem =
                historicalById[itemId]

            val replayedItem =
                replayedById[itemId]

            /*
             * -----------------------------------------------------
             * Item Added
             * -----------------------------------------------------
             */

            if (
                historicalItem == null &&
                replayedItem != null
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "item",

                        historicalValue =
                            null,

                        replayedValue =
                            replayedItem.toString(),

                        description =
                            "Item exists only in replayed adaptation.",
                    )

                return@forEach
            }

            /*
             * -----------------------------------------------------
             * Item Missing
             * -----------------------------------------------------
             */

            if (
                historicalItem != null &&
                replayedItem == null
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "item",

                        historicalValue =
                            historicalItem.toString(),

                        replayedValue =
                            null,

                        description =
                            "Item exists only in historical adaptation.",
                    )

                return@forEach
            }

            check(historicalItem != null)
            check(replayedItem != null)

            /*
             * -----------------------------------------------------
             * Base Priority
             * -----------------------------------------------------
             */

            if (
                !approximatelyEqual(
                    historicalItem.basePriority,
                    replayedItem.basePriority,
                    policy.priorityTolerance,
                )
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "basePriority",

                        historicalValue =
                            historicalItem
                                .basePriority
                                .toString(),

                        replayedValue =
                            replayedItem
                                .basePriority
                                .toString(),

                        description =
                            "Base priority differs.",
                    )
            }

            /*
             * -----------------------------------------------------
             * Adaptation Factor
             * -----------------------------------------------------
             */

            if (
                !approximatelyEqual(
                    historicalItem.adaptationFactor,
                    replayedItem.adaptationFactor,
                    policy.adaptationFactorTolerance,
                )
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "adaptationFactor",

                        historicalValue =
                            historicalItem
                                .adaptationFactor
                                .toString(),

                        replayedValue =
                            replayedItem
                                .adaptationFactor
                                .toString(),

                        description =
                            "Adaptation factor differs.",
                    )
            }

            /*
             * -----------------------------------------------------
             * Effective Priority
             * -----------------------------------------------------
             */

            if (
                !approximatelyEqual(
                    historicalItem.effectivePriority,
                    replayedItem.effectivePriority,
                    policy.priorityTolerance,
                )
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "effectivePriority",

                        historicalValue =
                            historicalItem
                                .effectivePriority
                                .toString(),

                        replayedValue =
                            replayedItem
                                .effectivePriority
                                .toString(),

                        description =
                            "Effective priority differs.",
                    )
            }

            /*
             * -----------------------------------------------------
             * Reasons
             * -----------------------------------------------------
             */

            if (
                policy.requireReasonParity &&
                historicalItem.reasons !=
                replayedItem.reasons
            ) {

                differences +=
                    DynamicScheduleAdaptationVerificationDifference(
                        itemId =
                            itemId.toString(),

                        factor =
                            "reasons",

                        historicalValue =
                            historicalItem
                                .reasons
                                .joinToString(),

                        replayedValue =
                            replayedItem
                                .reasons
                                .joinToString(),

                        description =
                            "Adaptation reasons differ.",
                    )
            }
        }

        /*
         * ---------------------------------------------------------
         * 最终分类
         * ---------------------------------------------------------
         */

        val status =
            when {

                inputChanged &&
                    policy.requireSameInput ->
                    DynamicScheduleAdaptationVerificationStatus
                        .INPUT_CHANGED

                policyChanged &&
                    policy.requireSamePolicy ->
                    DynamicScheduleAdaptationVerificationStatus
                        .POLICY_CHANGED

                engineChanged &&
                    policy.requireSameEngineVersion ->
                    DynamicScheduleAdaptationVerificationStatus
                        .ENGINE_CHANGED

                differences.isEmpty() ->
                    DynamicScheduleAdaptationVerificationStatus
                        .EXACT_MATCH

                else ->
                    DynamicScheduleAdaptationVerificationStatus
                        .BEHAVIOR_CHANGED
            }

        val diagnostics =
            buildList {

                if (inputChanged) {

                    add(
                        "Historical input fingerprint " +
                            "'$historicalInputFingerprint' " +
                            "differs from replay input fingerprint " +
                            "'$replayInputFingerprint'.",
                    )
                }

                if (policyChanged) {

                    add(
                        "Historical policy fingerprint " +
                            "'$historicalPolicyFingerprint' " +
                            "differs from replay policy fingerprint " +
                            "'$replayPolicyFingerprint'.",
                    )
                }

                if (engineChanged) {

                    add(
                        "Historical engine version " +
                            "'$historicalEngineVersion' " +
                            "differs from replay engine version " +
                            "'$replayEngineVersion'.",
                    )
                }

                if (differences.isNotEmpty()) {

                    add(
                        "Adaptation verification produced " +
                            "${differences.size} difference(s).",
                    )
                }
            }

        return DynamicScheduleAdaptationVerificationReport(
            status =
                status,

            historicalEngineVersion =
                historicalEngineVersion,

            replayEngineVersion =
                replayEngineVersion,

            historicalPolicyFingerprint =
                historicalPolicyFingerprint,

            replayPolicyFingerprint =
                replayPolicyFingerprint,

            differences =
                differences,

            diagnostics =
                diagnostics,
        )
    }

    private fun approximatelyEqual(
        left: Double,
        right: Double,
        tolerance: Double,
    ): Boolean =
        abs(left - right) <= tolerance
}
