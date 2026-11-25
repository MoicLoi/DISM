package me.moiclathy.dism.scenario

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.persistence.repository.SqlDelightEntityRepository
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import me.moiclathy.dism.persistence.repository.SqlDelightRelationStateRepository
import me.moiclathy.dism.persistence.repository.SqlDelightStateCommitRepository
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.runtime.DismRuntime
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * 持久化场景：DISM 的现实模型不是一次运行期间的临时变量。
 *
 * Runtime A：建立 充电宝 located_at 宿舍、实验课 requires 充电宝，落盘，关闭。
 * Runtime B：从同一 SQLite 文件构造（模拟重启）→
 *   仍知道充电宝在宿舍、仍能发现 RequirementRisk；
 *   再说"我带上了"→ 风险消失。
 */
class ItemRequirementPersistenceScenarioTest {

    private fun sqlPersistence(
        database: DismDatabase,
    ): DismPersistence =
        DismPersistence(
            entities = SqlDelightEntityRepository(database),
            evidence = SqlDelightEvidenceRepository(database),
            relationStates = SqlDelightRelationStateRepository(database),
            transitionLog = SqlDelightTransitionLogRepository(database),
            stateCommit = SqlDelightStateCommitRepository(database),
        )

    private fun draft(
        subject: String,
        subjectType: EntityType,
        predicate: me.moiclathy.dism.domain.relation.Predicate,
        objectText: String,
        objectType: EntityType,
        evidenceId: kotlin.uuid.Uuid,
        at: Instant,
    ) = SemanticObservationDraft(
        subjectMention = EntityMention(subject, subjectType),
        predicate = predicate,
        objectMention = EntityMention(objectText, objectType),
        confidence = 1.0,
        evidenceIds = setOf(evidenceId),
        observedAt = at,
    )

    private fun evidence(
        at: Instant,
    ) = Evidence(
        type = EvidenceType.EXPLICIT_STATEMENT,
        source = "user",
        observedAt = at,
        confidence = 1.0,
    )

    @Test
    fun restoredRuntimeShouldRememberStateAndKeepRiskUntilItemCarried() {

        val file =
            File.createTempFile("dism-scenario-", ".db")
        file.delete() // Factory 用 exists() 判 Schema

        try {

            val user = Entity(type = EntityType.PERSON, name = "我")
            val powerBank = Entity(type = EntityType.ITEM, name = "充电宝")
            val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
            val experiment = Entity(type = EntityType.EVENT, name = "实验课")

            val evening =
                Instant.parse("2026-09-10T20:00:00Z")

            // ── Runtime A：建立世界状态并落盘 ──
            run {
                val database =
                    DismDatabase(
                        JvmDatabaseDriverFactory(file).createDriver()
                    )
                val persistence = sqlPersistence(database)
                persistence.entities.saveAll(
                    listOf(user, powerBank, dorm, experiment)
                )

                val api: DismRuntimeApi = DismRuntime(persistence)

                val evRequire = evidence(evening)
                persistence.evidence.save(evRequire)
                assertIs<ObserveResult.Applied>(
                    api.observe(
                        draft = draft(
                            "实验课", EntityType.EVENT, Predicates.REQUIRES,
                            "充电宝", EntityType.ITEM, evRequire.id, evening,
                        ),
                        context = Context(now = evening),
                    )
                )

                val evLocated = evidence(evening)
                persistence.evidence.save(evLocated)
                assertIs<ObserveResult.Applied>(
                    api.observe(
                        draft = draft(
                            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
                            "宿舍", EntityType.LOCATION, evLocated.id, evening,
                        ),
                        context = Context(now = evening),
                    )
                )
                // Runtime A 随作用域结束（driver 在工厂内部，进程内文件库仍落盘）
            }

            // ── Runtime B：模拟重启，从同一文件恢复 ──
            val databaseB =
                DismDatabase(
                    JvmDatabaseDriverFactory(file).createDriver()
                )
            val persistenceB = sqlPersistence(databaseB)
            val apiB: DismRuntimeApi = DismRuntime(persistenceB)

            // 仍知道充电宝在宿舍
            val morning =
                Instant.parse("2026-09-11T07:30:00Z")

            val stillInDorm =
                apiB.queryAt(
                    instant = morning,
                    subjectId = powerBank.id,
                    predicate = Predicates.LOCATED_AT,
                )

            assertEquals(
                dorm.id,
                stillInDorm.single().key.objectId,
                "重启后仍应知道充电宝在宿舍",
            )

            // 仍能发现 RequirementRisk → 可见提醒
            val firstEval =
                apiB.observeAndEvaluate(
                    // 一条不改变风险事实的中性重放：再次陈述 requires，
                    // 触发对 requires 槽的重算；更直接的是用"位置再确认"。
                    draft =
                        draft(
                            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
                            "宿舍", EntityType.LOCATION,
                            run {
                                val e = evidence(morning)
                                persistenceB.evidence.save(e)
                                e.id
                            },
                            morning,
                        ),
                    context =
                        Context(
                            now = morning,
                            upcomingEvents =
                                listOf(
                                    ContextEvent(
                                        eventId = experiment.id,
                                        startAt = morning + 1.hours,
                                    )
                                ),
                        ),
                    userId = user.id,
                    attentionPolicy = AttentionPolicy(),
                )

            assertIs<ObserveResult.Applied>(firstEval.observeResult)

            assertTrue(
                firstEval.propagationResults
                    .flatMap { it.requirementRisks }
                    .isNotEmpty(),
                "重启后面对即将到来的实验课，仍应发现充电宝风险",
            )
            assertTrue(
                firstEval.visibleRecommendations
                    .any { it.type == RecommendationType.PREPARE_ITEM },
                "重启后仍应显示 PREPARE_ITEM 提醒",
            )

            // ── 现实变化："我把充电宝带上了" → 风险消失 ──
            val carryTime =
                Instant.parse("2026-09-11T07:40:00Z")
            val evCarry = evidence(carryTime)
            persistenceB.evidence.save(evCarry)

            val secondEval =
                apiB.observeAndEvaluate(
                    draft =
                        draft(
                            "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
                            "我", EntityType.PERSON, evCarry.id, carryTime,
                        ),
                    context =
                        Context(
                            now = carryTime,
                            upcomingEvents =
                                listOf(
                                    ContextEvent(
                                        eventId = experiment.id,
                                        startAt = carryTime + 1.hours,
                                    )
                                ),
                        ),
                    userId = user.id,
                    attentionPolicy = AttentionPolicy(),
                )

            assertIs<ObserveResult.Applied>(secondEval.observeResult)

            assertTrue(
                secondEval.visibleRecommendations
                    .none { it.type == RecommendationType.PREPARE_ITEM },
                "带上充电宝后 PREPARE_ITEM 提醒应消失",
            )

            assertTrue(
                apiB.currentStates().any {
                    it.key.predicate == Predicates.CARRIED_BY &&
                        it.key.subjectId == powerBank.id &&
                        it.key.objectId == user.id
                },
                "恢复后的 Runtime 应继续演化出 carried_by 我 的活动状态",
            )

        } finally {
            file.delete()
        }
    }
}
