package me.moiclathy.dism.schedule.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.ics.IcsScheduleParser
import me.moiclathy.dism.external.schedule.normalization.DeterministicExternalScheduleNormalizer
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseInput
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBinding
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingId
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingMode
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 产品化缝合 Vertical Slice（v0.1）。
 *
 * 目标：证明下面这条链**真的能跑通**，而不是各自单独通过单元测试：
 *
 * ```text
 * ICS 文本
 *   ↓ 11.4 IcsScheduleParser
 * ExternalSchedule
 *   ↓ 11.5 Normalizer
 * NormalizedExternalSchedule
 *   ↓ ExternalScheduleToDynamicScheduleMapper（本次新增的缝合层）
 * 12.1 DynamicScheduleModel
 *   ↓ 12.2 State Projection
 * 状态投影后的 Schedule
 *   ↓ 13.1 DynamicScheduleCore.evaluate()
 * 12.10 Runtime（Dependency → Context → Attention → Conflict → Explanation）
 *   ↓ 12.23 Persistence Bridge
 * Snapshot + Trace + Replay Input
 *   ↓ 13.1 core.replay() / core.verifyReplay()
 * 历史决策可重放、可校验
 * ```
 */
class DynamicScheduleVerticalSliceTest {

    private val now =
        Instant.parse("2026-09-24T07:20:00Z")

    private val ics =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:course-001
        SUMMARY:实验动物学
        DTSTART:20260924T080000Z
        DTEND:20260924T094000Z
        LOCATION:实验楼201
        X-COURSE-CODE:LAB101
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    @Test
    fun icsBecomesRunnableScheduleDecision() = runTest {

        /*
         * 1) 11.4 ICS → ExternalSchedule
         */
        val parseResult =
            IcsScheduleParser()
                .parse(
                    ExternalScheduleParseInput(
                        sourceId =
                            me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId(
                                "ics-test",
                            ),

                        rawContent = ics,
                        origin = ExternalScheduleOrigin.ICS,
                    ),
                )

        assertTrue(
            parseResult.success,
            "ICS should parse",
        )

        val schedule =
            assertNotNull(
                parseResult.schedule,
            )

        /*
         * 2) 11.5 Normalize
         */
        val normalized =
            DeterministicExternalScheduleNormalizer()
                .normalize(
                    schedule = schedule,
                    normalizedAt = now,
                )

        assertTrue(
            normalized.success,
            "schedule should normalize",
        )

        val normalizedSchedule =
            assertNotNull(
                normalized.schedule,
            )

        val scheduleModel =
            ExternalScheduleToDynamicScheduleMapper()
                .map(
                    schedule = schedule,
                    mappedAt = now,
                )

        assertEquals(
            1,
            scheduleModel.size,
        )

        val itemId =
            DynamicScheduleItemId("course-001")

        /*
         * 3) 13.1 装配（含 12.x Runtime / Persistence / Replay / Verification）
         */
        val session =
            DynamicScheduleIntegrationAssembly()
                .build(
                    identityByItem =
                        mapOf(
                            itemId to
                                schedule
                                    .entries
                                    .first()
                                    .id
                                    .value,
                        ),
                )

        /*
         * 4) 12.2 State Projection：把 ExternalSchedule 投影成当前状态
         */
        val projection =
            session.project(
                schedule = schedule,
                now = now,
            )

        val model =
            projection.projectedModel

        val contextBindings =
            DynamicScheduleContextBindingSet(
                bindings =
                    listOf(
                        DynamicScheduleContextBinding(
                            id =
                                DynamicScheduleContextBindingId(
                                    "binding-1"
                                ),
                            itemId = itemId,
                            contextKey = "DORM",
                            mode =
                                DynamicScheduleContextBindingMode
                                    .PREFERRED,
                            weight = 0.5,
                        ),
                    ),
            )

        /*
         * 5) 13.1 Core.evaluate()：真实完整决策 + 持久化
         */
        val evaluation =
            session.evaluate(
                model = model,

                now = now,

                activeContextKeys = setOf("DORM"),

                contextBindings = contextBindings,
            )

        val decision =
            evaluation.intelligence

        assertTrue(
            decision.attentionProjection.candidateCount > 0,
            "decision should produce attention candidates",
        )

        val explanation =
            assertNotNull(
                decision.attentionExplanations[itemId],
                "decision must be explainable",
            )

        assertTrue(
            explanation.summary.isNotBlank(),
        )

        val conflict =
            assertNotNull(
                decision.attentionConflicts[itemId],
            )

        assertTrue(
            conflict.attentionRequired,
            "an upcoming class requires attention",
        )

        /*
         * 6) 持久化确实发生：Snapshot / Trace / Replay Input 都可查到
         */
        assertEquals(
            1,
            evaluation.persistence.snapshots.size,
        )

        assertEquals(
            1,
            evaluation.persistence.traces.size,
        )

        assertEquals(
            1,
            evaluation.persistence.replayInputCount,
        )

        val snapshotId =
            assertNotNull(
                session.latestSnapshotId(itemId),
                "snapshot should be persisted",
            )

        /*
         * 7) 13.1 core.replay()：真正重跑历史 Runtime Input
         *
         * 13.15：显式传入执行时间。
         *
         * 重放**不应该**使用历史时间——
         * `replayedAt` 是"现在什么时候重新运行"，
         * 与历史评估时间是两条时间线。
         * 显式传值同时让这个测试保持确定性。
         */
        val replayedAt =
            Instant.parse("2026-09-25T14:30:00Z")

        val replay =
            session.core.replay(
                snapshotId = snapshotId,
                replayedAt = replayedAt,
            )

        assertEquals(
            itemId,
            replay.replayInput
                .decisionSnapshot
                .itemId,
        )

        assertTrue(
            replay.replayedResult
                .attentionProjection
                .candidateCount > 0,
            "replayed runtime must reproduce the decision",
        )

        assertEquals(
            replayedAt,
            replay.execution.replayedAt,
            "replayedAt 必须是本次重放的执行时间",
        )

        assertEquals(
            now,
            replay.replayInput.evaluatedAt,
            "历史评估时间必须原封不动地保留在 ReplayInput 上",
        )

        assertNotEquals(
            replay.execution.replayedAt,
            replay.replayInput.evaluatedAt,
            "历史时间与重放时间不能混为一谈",
        )

        /*
         * 8) 13.1 core.verifyReplay()：历史决策 vs 重放结果
         */
        val verification =
            session.core.verifyReplay(
                snapshotId = snapshotId,
                replayedAt = replayedAt,
                verifiedAt = replayedAt,
            )

        assertEquals(
            me.moiclathy.dism.schedule.replay
                .DynamicScheduleReplayVerificationStatus
                .EXACT_MATCH,
            verification.status,
            buildString {
                append("replay must reproduce the historical decision exactly; ")
                append("differences=")
                append(
                    verification.differences.joinToString {
                        "${it.factor}(${it.historicalValue} -> ${it.replayedValue})"
                    },
                )
                append(" diagnostics=")
                append(verification.diagnostics.joinToString())
            },
        )

        assertTrue(
            verification.verified,
        )

        /*
         * 8.5) 14.7：当前 Context 对 Priority 的影响 + 与历史适配的合并
         *
         * 这条链是**纯视图**：它不会修改 model。
         */
        val priorityBeforeContextSensitiveCombination =
            model.items.first().priority

        val contextInfluences =
            session.contextPriorityInfluences(
                model = model,
                activeContextKeys = setOf("DORM"),
                contextBindings = contextBindings,
            )

        assertEquals(1, contextInfluences.size)

        val contextInfluence =
            contextInfluences.first()

        assertTrue(
            contextInfluence.contextFactor > 1.0,
            "DORM is a fully matched PREFERRED context, " +
                "factor=${contextInfluence.contextFactor}",
        )

        val prioritized =
            session.contextSensitivePriorities(
                model = model,

                historicalAdaptations =
                    listOf(
                        me.moiclathy.dism.schedule.adaptation
                            .DynamicSchedulePriorityAdaptation(
                                itemId = itemId,
                                basePriority = 0.5,
                                adaptationFactor = 1.0,
                                effectivePriority = 0.5,
                            ),
                    ),

                activeContextKeys = setOf("DORM"),
                contextBindings = contextBindings,
            )

        assertEquals(1, prioritized.size)

        val sensitive =
            prioritized.first()

        assertEquals(
            1.0,
            sensitive.historicalFactor,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            contextInfluence.contextFactor,
            sensitive.contextFactor,
            absoluteTolerance = 0.000001,
        )

        /*
         * effectivePriority = 0.5 × 1.30 = 0.65
         */
        assertEquals(
            0.65,
            sensitive.effectivePriority,
            absoluteTolerance = 0.000001,
        )

        /*
         * 关键：原始 model 没有被改动。
         *
         * 注意这里比较的是**调用前记录的值**，而不是某个常数——
         * 12.2 的 state projection 已经先动过 priority。
         */
        assertEquals(
            priorityBeforeContextSensitiveCombination,
            model.items.first().priority,
            absoluteTolerance = 0.000001,
            message = "14.7 must not mutate the schedule item",
        )

        /*
         * 8.6) 14.8：这次自适应有多可信
         *
         * 注意这里**不**做 `effectivePriority *= confidence`。
         */
        val confidences =
            session.adaptationConfidence(
                model = model,

                historicalAdaptations =
                    listOf(
                        me.moiclathy.dism.schedule.adaptation
                            .DynamicSchedulePriorityAdaptation(
                                itemId = itemId,
                                basePriority = 0.5,
                                adaptationFactor = 1.0,
                                effectivePriority = 0.5,
                            ),
                    ),

                activeContextKeys = setOf("DORM"),
                contextBindings = contextBindings,

                sampleCountByItem = mapOf("course-001" to 12),
            )

        assertEquals(1, confidences.size)

        val confidence = confidences.first()

        assertTrue(
            confidence.value in 0.0..1.0,
        )

        assertTrue(
            confidence.enabled,
            "12 samples + full context match should clear " +
                "the default 0.50 threshold, value=${confidence.value}",
        )

        /*
         * 关键：confidence 只描述"有多确定"，
         * 它**不**进入 effectivePriority。
         */
        assertEquals(
            sensitive.effectivePriority,
            prioritized.first().effectivePriority,
            absoluteTolerance = 0.000001,
        )

        /*
         * 8.7) 14.15：把这次决策变成可追溯的因果图
         */
        val partialProvenance =
            assertNotNull(
                session.provenance(
                    itemId = itemId,
                    generatedAt = now,
                ),
                "a persisted snapshot must yield a provenance view",
            )

        assertEquals(itemId, partialProvenance.itemId)

        assertEquals(snapshotId, partialProvenance.snapshotId)

        assertTrue(
            partialProvenance.contains(
                me.moiclathy.dism.schedule.adaptation.provenance
                    .DynamicScheduleDecisionProvenanceNodeType.TRACE,
            ),
            "the trace was persisted, so a TRACE node must exist",
        )

        /*
         * 这个 session 没有落过 adaptation，所以只能是 PARTIAL。
         * 关键是它**没有伪造**一个 PRIORITY_ADAPTATION 节点。
         */
        assertEquals(
            me.moiclathy.dism.schedule.adaptation.provenance
                .DynamicScheduleDecisionProvenanceCompleteness.PARTIAL,
            partialProvenance.completeness,
        )

        assertTrue(
            !partialProvenance.contains(
                me.moiclathy.dism.schedule.adaptation.provenance
                    .DynamicScheduleDecisionProvenanceNodeType
                    .PRIORITY_ADAPTATION,
            ),
        )

        /*
         * 现在真的把适配结果落盘，再看一次。
         */
        session.persistAdaptation(
            adaptation =
                me.moiclathy.dism.schedule.adaptation
                    .DynamicSchedulePriorityAdaptation(
                        itemId = itemId,
                        basePriority = 0.5,
                        adaptationFactor = 1.2,
                        effectivePriority = 0.6,
                    ),
        )

        val completeProvenance =
            assertNotNull(
                session.provenance(
                    itemId = itemId,
                    generatedAt = now,
                ),
            )

        assertEquals(
            me.moiclathy.dism.schedule.adaptation.provenance
                .DynamicScheduleDecisionProvenanceCompleteness.COMPLETE,
            completeProvenance.completeness,
        )

        assertTrue(
            completeProvenance
                .ancestorsOf(completeProvenance.outputNodeId)
                .any {
                    it.type ==
                        me.moiclathy.dism.schedule.adaptation
                            .provenance
                            .DynamicScheduleDecisionProvenanceNodeType
                            .PRIORITY_ADAPTATION
                },
            "the persisted adaptation must be reachable from the output",
        )

        /*
         * 按 snapshotId 查同一张图。
         */
        assertEquals(
            completeProvenance.nodes.map { it.id },
            session.provenance(
                snapshotId = snapshotId,
                generatedAt = now,
            )?.nodes?.map { it.id },
        )

        /*
         * 8.8) 14.16：把图变成人话，以及结构化投影
         */
        val provenanceExplanation =
            assertNotNull(
                session.explainProvenance(
                    itemId = itemId,
                    generatedAt = now,
                ),
            )

        assertTrue(provenanceExplanation.complete)

        assertTrue(provenanceExplanation.summary.isNotBlank())

        assertTrue(
            provenanceExplanation.decisiveFactors.isNotEmpty(),
        )

        assertTrue(
            provenanceExplanation.factors.none {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.provenance
                        .DynamicScheduleDecisionProvenanceNodeType.INPUT
            },
            "product-facing factors must exclude pipeline ends",
        )

        val onlyDecisive =
            session.queryProvenance(
                query =
                    me.moiclathy.dism.schedule.adaptation.provenance
                        .DynamicScheduleDecisionProvenanceQuery(
                            itemId = itemId,
                            includeNonDecisive = false,
                            includeDiagnostics = false,
                        ),
                generatedAt = now,
            )

        assertTrue(onlyDecisive.found)

        assertTrue(
            onlyDecisive.nodes.all { it.decisive },
        )

        assertTrue(
            onlyDecisive.nodes.size <
                completeProvenance.nodeCount,
        )

        /*
         * provenance 仍然是完整图，只是 nodes 被投影了。
         */
        assertEquals(
            completeProvenance.nodeCount,
            onlyDecisive.provenance?.nodeCount,
        )

        assertTrue(onlyDecisive.diagnostics.isEmpty())

        /*
         * 指向不存在的 snapshot 时是"没找到"，不是异常。
         */
        val missing =
            session.queryProvenance(
                query =
                    me.moiclathy.dism.schedule.adaptation.provenance
                        .DynamicScheduleDecisionProvenanceQuery(
                            snapshotId =
                                me.moiclathy.dism.schedule.snapshot
                                    .DynamicScheduleDecisionSnapshotId(
                                        kotlin.uuid.Uuid.parse(
                                            "00000000-0000-0000-0000-0000000000ff",
                                        ),
                                    ),
                        ),
                generatedAt = now,
            )

        assertTrue(!missing.found)

        assertTrue(missing.nodes.isEmpty())

        /*
         * 8.9) 14.17：生命周期审计
         */
        val activeAudit =
            assertNotNull(
                session.audit(
                    itemId = itemId,
                    generatedAt = now,
                ),
            )

        assertEquals(
            me.moiclathy.dism.schedule.adaptation.audit
                .DynamicScheduleDecisionAuditStatus.ACTIVE,
            activeAudit.status,
        )

        assertTrue(
            activeAudit.events.any {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.audit
                        .DynamicScheduleDecisionAuditEventType
                        .CREATED
            },
        )

        assertTrue(
            activeAudit.events.any {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.audit
                        .DynamicScheduleDecisionAuditEventType
                        .TRACE_PERSISTED
            },
        )

        assertTrue(
            activeAudit.events.any {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.audit
                        .DynamicScheduleDecisionAuditEventType
                        .ADAPTATION_PERSISTED
            },
        )

        /*
         * 再决策一次（更晚的时间），于是第一次决策就被取代了。
         *
         * 注意：取代**不是**删除。第一次的 CREATED / SNAPSHOT
         * 必须原样留在审计里，否则"为什么昨天这么排"就答不出来。
         */
        val laterNow = now.plus(1.days)

        session.evaluate(
            model = model,
            now = laterNow,
            activeContextKeys = setOf("DORM"),
            contextBindings = contextBindings,
        )

        val supersededAudit =
            assertNotNull(
                session.audit(
                    snapshotId = snapshotId,
                    generatedAt = laterNow,
                ),
            )

        assertEquals(
            me.moiclathy.dism.schedule.adaptation.audit
                .DynamicScheduleDecisionAuditStatus.SUPERSEDED,
            supersededAudit.status,
        )

        assertTrue(
            supersededAudit.events.any {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.audit
                        .DynamicScheduleDecisionAuditEventType
                        .SUPERSEDED
            },
        )

        assertTrue(
            supersededAudit.events.any {
                it.type ==
                    me.moiclathy.dism.schedule.adaptation.audit
                        .DynamicScheduleDecisionAuditEventType
                        .CREATED
            },
            "superseding must not erase the original decision",
        )

        /*
         * 最新的那一次仍然是 ACTIVE。
         */
        assertEquals(
            me.moiclathy.dism.schedule.adaptation.audit
                .DynamicScheduleDecisionAuditStatus.ACTIVE,
            session.audit(
                itemId = itemId,
                generatedAt = laterNow,
            )?.status,
        )

        /*
         * 9) “为什么现在提醒？”——产品化真正需要的输出
         */
        val why =
            session.explain(
                result = decision,
                itemId = itemId,
            )

        assertTrue(
            why.contains("attentionRequired=true"),
        )

        assertTrue(
            why.contains("summary="),
        )

        println("=== VERTICAL SLICE: why is the user reminded now? ===")
        println(why)
    }

    /**
     * 14.4 缝合验证：
     *
     * ```text
     * History → Feature → Interpretation → Behavioral Model → Weight Delta
     * ```
     *
     * 这条链**没有历史时也必须能跑**，而且要明确告诉调用方
     * "现在还没有足够证据"——而不是悄悄返回一个看起来像
     * 真实结论的 0.0。
     */
    @Test
    fun weightAdaptationIsSafeWithoutHistory() = runTest {

        val itemId =
            DynamicScheduleItemId("course-001")

        val session =
            DynamicScheduleIntegrationAssembly()
                .build(
                    identityByItem =
                        mapOf(
                            itemId to "course-001",
                        ),
                )

        val interpretation =
            session.decisionInterpretation(
                itemId = itemId,
                now = now,
            )

        val model =
            session.behavioralAdaptation(
                itemId = itemId,
                now = now,
            )

        assertEquals(
            false,
            model.hasEvidence,
        )

        val adaptation =
            session.weightAdaptation(
                itemId = itemId,
                now = now,
            )

        /*
         * 没有历史 → 不允许适配。
         *
         * 关键点不是 enabled = false 本身，
         * 而是它**不能**返回一个"部分生效"的权重偏移。
         */
        assertEquals(
            false,
            adaptation.enabled,
        )

        assertTrue(
            adaptation.isNeutral,
            "no history must not move any weight",
        )

        assertEquals(
            0,
            adaptation.sampleCount,
        )

        val batch =
            session.weightAdaptations(
                itemIds = listOf(itemId),
                now = now,
            )

        assertEquals(1, batch.size)

        assertEquals(
            false,
            batch.find(itemId)?.enabled,
        )

        assertTrue(
            batch.enabled().isEmpty(),
        )

        /*
         * 14.5：继续往下翻译成 Attention 影响量。
         *
         * 没有历史 → 影响量必须为 0 且 enabled = false，
         * 而不是"给一个很小的偏移"。
         */
        val attention =
            session.attentionAdaptation(
                itemId = itemId,
                now = now,
            )

        assertEquals(false, attention.enabled)

        assertEquals(
            0.0,
            attention.attentionDelta,
        )

        val influence =
            session.attentionInfluence(
                itemId = itemId,
                now = now,
            )

        assertEquals(false, influence.enabled)

        assertEquals(0.0, influence.delta)

        val attentionBatch =
            session.attentionAdaptations(
                itemIds = listOf(itemId),
                now = now,
            )

        assertEquals(1, attentionBatch.size)

        assertEquals(
            itemId,
            attentionBatch.first().itemId,
        )

        /*
         * 14.6：再把同一个行为模型翻译成 Priority 影响量。
         *
         * 注意这条链**不**接 14.7 的 DynamicScheduleContextSensitivePriority：
         * 那条链消费的是 12.x 的 DynamicSchedulePriorityAdaptation。
         */
        val priority =
            session.behavioralPriorityAdaptation(
                itemId = itemId,
                now = now,
            )

        assertEquals(false, priority.enabled)

        assertEquals(0.0, priority.priorityDelta)

        val priorityInfluence =
            session.behavioralPriorityInfluence(
                itemId = itemId,
                now = now,
            )

        assertEquals(false, priorityInfluence.enabled)

        assertEquals(0.0, priorityInfluence.delta)

        val priorityBatch =
            session.behavioralPriorityAdaptations(
                itemIds = listOf(itemId),
                now = now,
            )

        assertEquals(1, priorityBatch.size)

        println(
            "=== 14.4/14.5/14.6: interpretation signals=" +
                "${interpretation.size}, " +
                "weight deltas=${adaptation.deltas}, " +
                "attention delta=${attention.attentionDelta}, " +
                "priority delta=${priority.priorityDelta} ===",
        )
    }

    /**
     * 14.11 缝合验证：
     *
     * ```text
     * 适配结果 + 用户覆盖 → 落盘 → 读回
     * ```
     *
     * 重点不是"能存"，而是**读回来之后仍然分得清**
     * 哪些是自动算的、哪些是用户设的。
     */
    @Test
    fun adaptationPersistsAndRestoresHumanOverride() = runTest {

        val itemId =
            DynamicScheduleItemId("course-001")

        val session =
            DynamicScheduleIntegrationAssembly()
                .build(
                    identityByItem =
                        mapOf(itemId to "course-001"),
                )

        assertTrue(
            session.restoreAdaptation(itemId) == null,
            "nothing should be stored before the first persist",
        )

        val adaptation =
            me.moiclathy.dism.schedule.adaptation
                .DynamicSchedulePriorityAdaptation(
                    itemId = itemId,
                    basePriority = 0.5,
                    adaptationFactor = 1.3,
                    effectivePriority = 0.65,
                    reasons = listOf("Context matched after_class"),
                )

        val override =
            me.moiclathy.dism.schedule.adaptation
                .DynamicScheduleHumanOverride(
                    itemId = itemId,
                    priorityOverride = 0.90,
                    createdAt = now,
                    reason = "User explicitly marked important",
                )

        session.persistAdaptation(
            adaptation = adaptation,
            humanOverride = override,
        )

        val restored =
            assertNotNull(
                session.restoreAdaptation(itemId),
            )

        assertEquals(adaptation, restored.adaptation)

        assertEquals(override, restored.humanOverride)

        /*
         * persistedAt 是"什么时候写进去的"，用的是真实时钟，
         * 与 schedule 的评估时间 `now` 是两条时间线。
         *
         * 这条断言刻意**不**写成 `assertEquals(now, ...)`：
         * 那会要求持久化层谎报时间。
         */
        assertTrue(
            restored.persistedAt > now,
            "persistedAt must come from the persistence clock, " +
                "not from the schedule evaluation time; " +
                "persistedAt=${restored.persistedAt}",
        )

        /*
         * 自动结果与用户结果并存，而不是互相覆盖。
         */
        assertEquals(
            0.65,
            restored.adaptation.effectivePriority,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            0.90,
            restored.humanOverride?.priorityOverride
                ?: 0.0,
            absoluteTolerance = 0.000001,
        )

        println(
            "=== 14.11: restored adaptation=" +
                "${restored.adaptation.effectivePriority}, " +
                "humanOverride=" +
                "${restored.humanOverride?.priorityOverride} ===",
        )
    }

    /**
     * 14.12 缝合验证：
     *
     * ```text
     * 存输入（Model + Attention History + Policy）
     *   + 存结果
     *   → 重放
     *   → 无差异
     * ```
     *
     * 然后用**改过的 policy** 再存一次，重放必须报差异——
     * 否则"参数被改"和"算法退化"永远分不清。
     */
    @Test
    fun adaptationReplayReproducesHistoricalResult() = runTest {

        val itemId =
            DynamicScheduleItemId("course-001")

        val session =
            DynamicScheduleIntegrationAssembly()
                .build(
                    identityByItem =
                        mapOf(itemId to "course-001"),
                )

        val model =
            me.moiclathy.dism.schedule.replay
                .DynamicScheduleReplayTestData.model()

        val histories =
            mapOf(
                me.moiclathy.dism.schedule.replay
                    .DynamicScheduleReplayTestData.itemId.value to
                    me.moiclathy.dism.schedule.adaptation
                        .DynamicScheduleAttentionHistory(
                            attentionKey =
                                me.moiclathy.dism.schedule.replay
                                    .DynamicScheduleReplayTestData
                                    .itemId.value,
                            shownCount = 10,
                            engagedCount = 3,
                            ignoredCount = 2,
                        ),
            )

        val historicalInput =
            me.moiclathy.dism.schedule.adaptation.replay
                .DynamicScheduleAdaptationReplayInput(
                    model = model,
                    histories = histories,
                    policy =
                        me.moiclathy.dism.schedule.adaptation
                            .DynamicSchedulePriorityAdaptationPolicy(),
                )

        /*
         * 用同一份输入真正跑一次，作为"历史结果"。
         */
        val historical =
            me.moiclathy.dism.schedule.adaptation
                .DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    input =
                        me.moiclathy.dism.schedule.adaptation
                            .DynamicSchedulePriorityAdaptationInput(
                                model = model,
                                histories = histories,
                            ),
                    policy = historicalInput.policy,
                )
                .adaptations

        val replayItemId =
            me.moiclathy.dism.schedule.replay
                .DynamicScheduleReplayTestData.itemId

        session.persistAdaptation(
            adaptation = historical.single(),
        )

        session.saveAdaptationReplayInput(
            itemId = replayItemId,
            input = historicalInput,
        )

        /*
         * 读回来必须逐字段相等。
         */
        assertEquals(
            historicalInput,
            session.restoreAdaptationReplayInput(replayItemId),
        )

        val result =
            session.replayAdaptation(replayItemId)

        assertTrue(
            result.equivalent,
            "replaying the stored input must reproduce the stored " +
                "result exactly; differences=${result.differences}",
        )

        assertEquals(
            1,
            result.historicalSize,
        )

        /*
         * 14.13：这次重放到底意味着什么？
         */
        val report =
            session.verifyAdaptation(
                itemId = replayItemId,

                historicalEngineVersion =
                    me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData.ENGINE_VERSION,

                replayEngineVersion =
                    me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData.ENGINE_VERSION,
            )

        assertEquals(
            me.moiclathy.dism.schedule.adaptation.verification
                .DynamicScheduleAdaptationVerificationStatus
                .EXACT_MATCH,
            report.status,
            buildString {
                append("verification must pass when the stored input ")
                append("reproduces the stored result; ")
                append("differences=")
                append(
                    report.differences.joinToString {
                        "${it.factor}(${it.historicalValue} -> ${it.replayedValue})"
                    },
                )
                append(" diagnostics=")
                append(report.diagnostics.joinToString())
            },
        )

        assertTrue(report.verified)

        /*
         * 传入一个被改过的 policy 之后，必须报告 POLICY_CHANGED，
         * 而不是 BEHAVIOR_CHANGED。
         */
        val drifted =
            session.verifyAdaptation(
                itemId = replayItemId,

                historicalEngineVersion =
                    me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData.ENGINE_VERSION,

                replayEngineVersion =
                    me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData.ENGINE_VERSION,

                currentPolicy =
                    me.moiclathy.dism.schedule.adaptation
                        .DynamicSchedulePriorityAdaptationPolicy(
                            engagedBonus = 0.90,
                        ),
            )

        assertEquals(
            me.moiclathy.dism.schedule.adaptation.verification
                .DynamicScheduleAdaptationVerificationStatus
                .POLICY_CHANGED,
            drifted.status,
            "a policy edit must not be reported as algorithm drift",
        )

        assertTrue(!drifted.verified)

        println(
            "=== 14.12/14.13: replay equivalent=" +
                "${result.equivalent}, " +
                "verify=${report.status}, " +
                "policyDrift=${drifted.status} ===",
        )
    }
}
