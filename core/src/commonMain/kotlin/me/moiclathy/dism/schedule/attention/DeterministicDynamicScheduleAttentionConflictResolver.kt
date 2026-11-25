package me.moiclathy.dism.schedule.attention

class DeterministicDynamicScheduleAttentionConflictResolver :
    DynamicScheduleAttentionConflictResolver {

    override fun resolve(
        input: DynamicScheduleAttentionConflictInput,
        policy: DynamicScheduleAttentionConflictPolicy,
    ): DynamicScheduleAttentionConflictResult {

        val signal = input.signal

        val originalPriority =
            (
                signal.schedulePriority *
                    signal.globalMemoryFactor *
                    signal.contextMemoryFactor
            )
                .coerceIn(0.0, 1.0)

        val hardConstraints =
            signal.hardConstraints

        /*
         * ---------------------------------------------------------
         * 1. No hard constraint.
         *
         * Learned memory is allowed to influence attention normally.
         * ---------------------------------------------------------
         */
        if (hardConstraints.isEmpty()) {

            return DynamicScheduleAttentionConflictResult(
                resolvedPriority =
                    originalPriority,

                conflict = null,

                hardConstraintPresent = false,

                attentionRequired =
                    originalPriority > 0.0,

                reasons =
                    listOf(
                        "No hard attention constraint",
                        "Learned attention factors applied normally",
                    ),
            )
        }

        /*
         * ---------------------------------------------------------
         * 2. Explicit user request.
         * ---------------------------------------------------------
         */

        val explicitRequest =
            hardConstraints
                .firstOrNull {
                    it.type ==
                        DynamicScheduleAttentionConstraintType
                            .EXPLICIT_USER_REQUEST
                }

        if (
            explicitRequest != null &&
            policy.allowExplicitOverride
        ) {

            val resolved =
                maxOf(
                    originalPriority,
                    policy.explicitRequestPriority,
                )
                    .coerceIn(0.0, 1.0)

            return DynamicScheduleAttentionConflictResult(
                resolvedPriority = resolved,

                conflict =
                    DynamicScheduleAttentionConflict(
                        itemId = signal.itemId,
                        type =
                            DynamicScheduleAttentionConflictType
                                .EXPLICIT_REQUEST_OVERRIDE,
                        originalPriority =
                            originalPriority,
                        resolvedPriority =
                            resolved,
                        overriddenBy =
                            explicitRequest,
                        reasons =
                            listOf(
                                "Explicit user request overrides learned suppression",
                            ),
                    ),

                hardConstraintPresent = true,

                attentionRequired = true,

                reasons =
                    listOf(
                        "Explicit user attention request",
                    ),
            )
        }

        /*
         * ---------------------------------------------------------
         * 3. Required dependency / context / hard anchor.
         *
         * Learned memory cannot suppress below the hard floor.
         * ---------------------------------------------------------
         */

        val strongestHardConstraint =
            hardConstraints
                .maxByOrNull {
                    it.strength
                }

        val hardFloor =
            (
                policy.minimumHardConstraintPriority *
                    (strongestHardConstraint?.strength ?: 1.0)
            )
                .coerceIn(0.0, 1.0)

        val resolved =
            maxOf(
                originalPriority,
                hardFloor,
            )
                .coerceIn(0.0, 1.0)

        val conflictType =
            if (originalPriority < hardFloor) {

                when (
                    strongestHardConstraint?.type
                ) {
                    DynamicScheduleAttentionConstraintType.REQUIRED_CONTEXT ->
                        DynamicScheduleAttentionConflictType
                            .CONTEXT_MEMORY_CONFLICT

                    else ->
                        DynamicScheduleAttentionConflictType
                            .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT
                }

            } else {
                DynamicScheduleAttentionConflictType.NONE
            }

        val conflict =
            if (
                conflictType !=
                    DynamicScheduleAttentionConflictType.NONE
            ) {
                DynamicScheduleAttentionConflict(
                    itemId =
                        signal.itemId,
                    type =
                        conflictType,
                    originalPriority =
                        originalPriority,
                    resolvedPriority =
                        resolved,
                    overriddenBy =
                        strongestHardConstraint,
                    reasons =
                        listOf(
                            "Learned attention suppression was overridden",
                            "Hard constraint floor=$hardFloor",
                        ),
                )
            } else {
                null
            }

        return DynamicScheduleAttentionConflictResult(
            resolvedPriority = resolved,

            conflict = conflict,

            hardConstraintPresent = true,

            attentionRequired =
                resolved > 0.0,

            reasons =
                buildList {
                    add(
                        "Hard constraint present",
                    )

                    if (
                        originalPriority <
                            hardFloor
                    ) {
                        add(
                            "Learned memory could not suppress below hard floor",
                        )
                    }
                },
        )
    }
}
