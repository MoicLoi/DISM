package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.14 / 14.15
 *
 * 确定性 Provenance Builder。
 *
 * 节点结构与 [DynamicScheduleDecisionProvenanceNodeType] 一一对应：
 *
 * ```text
 * INPUT
 *   └── SNAPSHOT
 *         ├── DEPENDENCY
 *         ├── CONTEXT
 *         ├── ATTENTION_MEMORY
 *         ├── TRACE
 *         ├── PRIORITY_ADAPTATION   ← 只有真有 adaptation 时
 *         ├── ADAPTATION_BOUNDARY   ← 只有真的传了 boundary 时
 *         ├── HUMAN_OVERRIDE        ← 只有真的传了 override 时
 *         ├── REPLAY                ← 只有这次结果来自重放时
 *         ├── VERIFICATION          ← 只有真的有验证结论时
 *         └── OUTPUT
 * ```
 *
 * 所有节点 id 都是由 snapshot / itemId 拼出的**确定性字符串**，
 * 不用随机数、不用时钟。同样的输入永远得到同样的图。
 */
class DeterministicDynamicScheduleDecisionProvenanceBuilder :
    DynamicScheduleDecisionProvenanceBuilder {

    override fun build(
        input: DynamicScheduleDecisionProvenanceInput,
    ): DynamicScheduleDecisionProvenance {

        val snapshot =
            input.snapshot

        val trace =
            input.trace

        val engineVersion =
            input.engineVersion

        require(engineVersion.isNotBlank())

        val nodes =
            mutableListOf<
                DynamicScheduleDecisionProvenanceNode
                >()

        /*
         * ---------------------------------------------------------
         * INPUT
         * ---------------------------------------------------------
         */

        val inputNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "input:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType.INPUT,

                decisive =
                    false,

                message =
                    "Runtime input produced this decision.",

                metadata =
                    mapOf(
                        "itemId" to
                            snapshot.itemId.toString(),

                        "scheduleRevision" to
                            snapshot.scheduleRevision.toString(),
                    ),
            )

        nodes += inputNode

        /*
         * ---------------------------------------------------------
         * SNAPSHOT
         * ---------------------------------------------------------
         */

        val snapshotNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "snapshot:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType.SNAPSHOT,

                parentIds =
                    listOf(
                        inputNode.id,
                    ),

                decisive =
                    true,

                message =
                    "Historical decision snapshot.",

                metadata =
                    mapOf(
                        "snapshotId" to
                            snapshot.id.toString(),

                        "capturedAt" to
                            snapshot.capturedAt.toString(),

                        "engineVersion" to
                            snapshot.engineVersion,
                    ),
            )

        nodes += snapshotNode

        /*
         * ---------------------------------------------------------
         * TRACE
         * ---------------------------------------------------------
         */

        val traceNode =
            trace?.let {

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "trace:${it.traceId}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType.TRACE,

                        parentIds =
                            listOf(
                                snapshotNode.id,
                            ),

                        decisive =
                            it.trace.decisiveEntries.isNotEmpty(),

                        message =
                            "Runtime decision trace.",

                        metadata =
                            mapOf(
                                "traceId" to
                                    it.traceId.toString(),

                                "entryCount" to
                                    it.trace.entries.size.toString(),

                                "decisiveEntryCount" to
                                    it.trace
                                        .decisiveEntries
                                        .size
                                        .toString(),
                            ),
                    )

                nodes += node

                node
            }

        /*
         * ---------------------------------------------------------
         * DEPENDENCY
         * ---------------------------------------------------------
         */

        val dependency =
            snapshot.dependency

        val dependencyNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "dependency:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType.DEPENDENCY,

                parentIds =
                    listOf(
                        snapshotNode.id,
                    ),

                decisive =
                    !dependency.requiredSatisfied,

                message =
                    if (
                        dependency.requiredSatisfied
                    ) {
                        "Required dependencies were satisfied."
                    } else {
                        "Dependency constraints affected the decision."
                    },

                metadata =
                    mapOf(
                        "requiredSatisfied" to
                            dependency.requiredSatisfied
                                .toString(),

                        "requiredDependencyCount" to
                            dependency.requiredDependencyCount
                                .toString(),

                        "missingDependencyCount" to
                            dependency.missingDependencyIds
                                .size
                                .toString(),
                    ),
            )

        nodes += dependencyNode

        /*
         * ---------------------------------------------------------
         * CONTEXT
         * ---------------------------------------------------------
         *
         * 注意字段命名陷阱：
         *
         * DynamicScheduleContextSnapshot.excludedSatisfied
         *   true  = **没有**被排除的 Context 生效（好）
         *
         * 而 DynamicScheduleContextMatch.excludedSatisfied
         *   true  = **有**被排除的 Context 生效（坏）
         *
         * 两者语义相反。这里处理的是 Snapshot，
         * 所以 decisive 的条件是 !excludedSatisfied。
         */

        val context =
            snapshot.context

        val contextNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "context:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType.CONTEXT,

                parentIds =
                    listOf(
                        snapshotNode.id,
                    ),

                decisive =
                    !context.requiredSatisfied ||
                        !context.excludedSatisfied,

                message =
                    "Context state used by the decision.",

                metadata =
                    mapOf(
                        "activeContextCount" to
                            context.activeContextKeys
                                .size
                                .toString(),

                        "requiredSatisfied" to
                            context.requiredSatisfied
                                .toString(),

                        "excludedSatisfied" to
                            context.excludedSatisfied
                                .toString(),
                    ),
            )

        nodes += contextNode

        /*
         * ---------------------------------------------------------
         * ATTENTION MEMORY
         * ---------------------------------------------------------
         */

        val memory =
            snapshot.attentionMemory

        val memoryNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "attention-memory:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType
                        .ATTENTION_MEMORY,

                parentIds =
                    listOf(
                        snapshotNode.id,
                    ),

                decisive =
                    memory.effectiveFactor != 1.0,

                message =
                    "Attention memory influenced this decision.",

                metadata =
                    mapOf(
                        "globalFactor" to
                            memory.globalFactor.toString(),

                        "contextFactor" to
                            memory.contextFactor.toString(),

                        "adaptationFactor" to
                            memory.adaptationFactor.toString(),

                        "effectiveFactor" to
                            memory.effectiveFactor.toString(),
                    ),
            )

        nodes += memoryNode

        /*
         * ---------------------------------------------------------
         * PRIORITY ADAPTATION
         * ---------------------------------------------------------
         */

        val adaptation =
            input.adaptations.firstOrNull {
                it.itemId == snapshot.itemId
            }

        val adaptationNode =
            adaptation?.let {

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "priority-adaptation:${it.itemId}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType
                                .PRIORITY_ADAPTATION,

                        parentIds =
                            listOf(
                                memoryNode.id,
                                contextNode.id,
                            ),

                        decisive =
                            it.adaptationFactor != 1.0,

                        message =
                            "Historical attention behavior adapted priority.",

                        metadata =
                            mapOf(
                                "basePriority" to
                                    it.basePriority.toString(),

                                "adaptationFactor" to
                                    it.adaptationFactor.toString(),

                                "effectivePriority" to
                                    it.effectivePriority.toString(),

                                "reasonCount" to
                                    it.reasons.size.toString(),
                            ),
                    )

                nodes += node

                node
            }

        /*
         * ---------------------------------------------------------
         * ADAPTATION BOUNDARY（14.9）
         * ---------------------------------------------------------
         */

        val boundaryNode =
            input.boundary?.let { boundary ->

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "adaptation-boundary:${snapshot.itemId}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType
                                .ADAPTATION_BOUNDARY,

                        parentIds =
                            listOfNotNull(
                                adaptationNode?.id,
                                memoryNode.id,
                            ),

                        decisive =
                            boundary.allowedDelta !=
                                boundary.requestedDelta,

                        message =
                            if (
                                boundary.allowedDelta !=
                                boundary.requestedDelta
                            ) {
                                "Adaptation boundary limited the automatic change."
                            } else {
                                "Adaptation boundary allowed the automatic change."
                            },

                        metadata =
                            mapOf(
                                "requestedDelta" to
                                    boundary.requestedDelta.toString(),

                                "allowedDelta" to
                                    boundary.allowedDelta.toString(),

                                "limitedBySingleChange" to
                                    boundary.limitedBySingleChange
                                        .toString(),

                                "limitedByCumulativeChange" to
                                    boundary.limitedByCumulativeChange
                                        .toString(),

                                "limitedByFrequency" to
                                    boundary.limitedByFrequency
                                        .toString(),

                                "limitedByExplicitUserSetting" to
                                    boundary.limitedByExplicitUserSetting
                                        .toString(),
                            ),
                    )

                nodes += node

                node
            }

        /*
         * ---------------------------------------------------------
         * HUMAN OVERRIDE（14.10）
         * ---------------------------------------------------------
         */

        val overrideNode =
            input.humanOverride?.let { override ->

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "human-override:${snapshot.itemId}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType
                                .HUMAN_OVERRIDE,

                        parentIds =
                            listOfNotNull(
                                adaptationNode?.id,
                                memoryNode.id,
                            ),

                        decisive = true,

                        message =
                            "A human override took precedence over the automatic adaptation.",

                        metadata =
                            buildMap<String, String> {
                                put(
                                    "locked",
                                    override.locked.toString(),
                                )

                                put(
                                    "createdAt",
                                    override.createdAt.toString(),
                                )

                                override.priorityOverride?.let {
                                    put(
                                        "priorityOverride",
                                        it.toString(),
                                    )
                                }

                                override.attentionOverride?.let {
                                    put(
                                        "attentionOverride",
                                        it.toString(),
                                    )
                                }

                                override.reason?.let {
                                    put("reason", it)
                                }
                            },
                    )

                nodes += node

                node
            }

        /*
         * ---------------------------------------------------------
         * REPLAY（14.12）
         * ---------------------------------------------------------
         */

        val replayNode =
            if (input.replayed) {

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "replay:${snapshot.id}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType
                                .REPLAY,

                        parentIds =
                            listOf(
                                snapshotNode.id,
                            ),

                        decisive = false,

                        message =
                            "This decision was reproduced by replaying stored input.",

                        metadata =
                            mapOf(
                                "snapshotId" to
                                    snapshot.id.toString(),
                            ),
                    )

                nodes += node

                node

            } else {
                null
            }

        /*
         * ---------------------------------------------------------
         * VERIFICATION（14.13）
         * ---------------------------------------------------------
         */

        val verificationNode =
            input.verificationStatus?.let { status ->

                val node =
                    DynamicScheduleDecisionProvenanceNode(
                        id =
                            "verification:${snapshot.id}",

                        type =
                            DynamicScheduleDecisionProvenanceNodeType
                                .VERIFICATION,

                        parentIds =
                            listOfNotNull(
                                replayNode?.id,
                                snapshotNode.id,
                            ),

                        decisive = false,

                        message =
                            "Replay verification concluded: $status",

                        metadata =
                            mapOf(
                                "status" to status,
                            ),
                    )

                nodes += node

                node
            }

        /*
         * ---------------------------------------------------------
         * OUTPUT
         * ---------------------------------------------------------
         */

        val outputParents =
            buildList {

                add(snapshotNode.id)

                traceNode?.let {
                    add(it.id)
                }

                add(dependencyNode.id)
                add(contextNode.id)
                add(memoryNode.id)

                adaptationNode?.let {
                    add(it.id)
                }

                boundaryNode?.let {
                    add(it.id)
                }

                overrideNode?.let {
                    add(it.id)
                }

                replayNode?.let {
                    add(it.id)
                }

                verificationNode?.let {
                    add(it.id)
                }
            }.distinct()

        val outputNode =
            DynamicScheduleDecisionProvenanceNode(
                id =
                    "output:${snapshot.id}",

                type =
                    DynamicScheduleDecisionProvenanceNodeType.OUTPUT,

                parentIds =
                    outputParents,

                decisive =
                    true,

                message =
                    "Final decision represented by the snapshot.",

                metadata =
                    mapOf(
                        "itemId" to
                            snapshot.itemId.toString(),

                        "scheduleRevision" to
                            snapshot.scheduleRevision
                                .toString(),

                        "engineVersion" to
                            engineVersion,
                    ),
            )

        nodes += outputNode

        /*
         * ---------------------------------------------------------
         * Completeness
         * ---------------------------------------------------------
         */

        val completeness =
            when {
                trace != null && adaptation != null ->
                    DynamicScheduleDecisionProvenanceCompleteness
                        .COMPLETE

                trace == null && adaptation == null ->
                    DynamicScheduleDecisionProvenanceCompleteness
                        .EMPTY

                else ->
                    DynamicScheduleDecisionProvenanceCompleteness
                        .PARTIAL
            }

        val diagnostics =
            buildList {

                if (trace == null) {
                    add(
                        "Decision trace is unavailable.",
                    )
                }

                if (adaptation == null) {
                    add(
                        "Priority adaptation is unavailable.",
                    )
                }
            }

        return DynamicScheduleDecisionProvenance(
            itemId =
                snapshot.itemId,

            generatedAt =
                input.generatedAt,

            snapshotId =
                snapshot.id,

            traceId =
                trace?.traceId,

            engineVersion =
                engineVersion,

            nodes =
                nodes,

            rootNodeId =
                inputNode.id,

            outputNodeId =
                outputNode.id,

            completeness =
                completeness,

            diagnostics =
                diagnostics,
        )
    }
}
