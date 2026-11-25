package me.moiclathy.dism.schedule.adaptation

class DeterministicDynamicScheduleHumanOverrideAdapter :
    DynamicScheduleHumanOverrideAdapter {

    override fun apply(
        input: DynamicScheduleHumanOverrideInput,
        policy: DynamicScheduleHumanOverridePolicy,
    ): DynamicScheduleHumanOverrideResult {

        val result =
            mutableListOf<DynamicSchedulePriorityAdaptation>()

        val overridden =
            mutableSetOf<me.moiclathy.dism.schedule.model.DynamicScheduleItemId>()

        val diagnostics =
            mutableListOf<String>()

        input.adaptations.forEach { adaptation ->

            val override =
                input.overrides[adaptation.itemId]

            if (override == null) {
                result += adaptation
                return@forEach
            }

            if (!override.isActive) {
                diagnostics +=
                    "Inactive human override ignored for ${adaptation.itemId}"

                result += adaptation
                return@forEach
            }

            var effectivePriority =
                adaptation.effectivePriority

            val reasons =
                adaptation.reasons.toMutableList()

            /*
             * Human priority override。
             *
             * 用户明确指定的 priority
             * 优先于自动适配结果。
             */
            if (
                policy.allowPriorityOverride &&
                override.priorityOverride != null
            ) {

                effectivePriority =
                    override.priorityOverride

                overridden += adaptation.itemId

                reasons +=
                    "Human priority override=$effectivePriority"

                override.reason?.let {
                    reasons +=
                        "Human override reason=$it"
                }
            }

            /*
             * Attention override 当前不直接修改
             * DynamicSchedulePriorityAdaptation。
             *
             * 它只记录到 diagnostics，
             * 等 Attention Runtime 层消费。
             */
            if (
                policy.allowAttentionOverride &&
                override.attentionOverride != null
            ) {

                diagnostics +=
                    "Human attention override=" +
                        "${override.attentionOverride} " +
                        "for ${adaptation.itemId}"
            }

            result +=
                adaptation.copy(
                    effectivePriority =
                        effectivePriority
                            .coerceIn(0.0, 1.0),

                    reasons =
                        reasons + "Human override has precedence",
                )
        }

        return DynamicScheduleHumanOverrideResult(
            adaptations = result,
            overriddenItemIds = overridden,
            diagnostics = diagnostics,
        )
    }
}
