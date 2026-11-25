package me.moiclathy.dism.runtime

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.repository.SqlDelightEntityRepository
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import me.moiclathy.dism.persistence.repository.SqlDelightRelationStateRepository
import me.moiclathy.dism.persistence.repository.SqlDelightStateCommitRepository
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 验证 App 只经 DismRuntimeApi 收口即可完成
 * observe / 查询 / snapshot，且无需接触任何内部组件。
 */
class DismRuntimeApiTest {

    private fun newPersistence(
        database: DismDatabase,
    ): DismPersistence =
        DismPersistence(
            entities =
                SqlDelightEntityRepository(database),
            evidence =
                SqlDelightEvidenceRepository(database),
            relationStates =
                SqlDelightRelationStateRepository(database),
            transitionLog =
                SqlDelightTransitionLogRepository(database),
            stateCommit =
                SqlDelightStateCommitRepository(database),
        )

    @Test
    fun appShouldUseOnlyRuntimeApiToObserveQueryAndSnapshot() {

        val driver =
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        DismDatabase.Schema.create(driver)

        val database = DismDatabase(driver)

        val persistence = newPersistence(database)

        val powerBank =
            Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm =
            Entity(type = EntityType.LOCATION, name = "宿舍")

        persistence.entities.saveAll(listOf(powerBank, dorm))

        val now =
            Instant.parse("2026-09-10T08:00:00Z")

        val evidence =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = now,
                confidence = 1.0,
            )
        persistence.evidence.save(evidence)

        // 以接口类型持有 —— App 只认识 DismRuntimeApi
        val api: DismRuntimeApi =
            DismRuntime(persistence)

        val result =
            api.observe(
                draft =
                    SemanticObservationDraft(
                        subjectMention =
                            EntityMention("充电宝", EntityType.ITEM),
                        predicate = Predicates.LOCATED_AT,
                        objectMention =
                            EntityMention("宿舍", EntityType.LOCATION),
                        confidence = 1.0,
                        evidenceIds = setOf(evidence.id),
                        observedAt = now,
                    ),
                context = Context(now = now),
            )

        assertIs<ObserveResult.Applied>(result)

        // 当前状态查询（经 Facade）
        assertEquals(
            dorm.id,
            api.queryAt(
                instant = now,
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            )
                .single()
                .key.objectId,
        )

        assertEquals(
            1,
            api.currentStates().size,
        )

        // 时间点查询
        assertEquals(
            dorm.id,
            api.queryAt(
                instant = Instant.parse("2026-09-10T09:00:00Z"),
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            ).single().key.objectId,
        )

        // 快照
        val snapshot = api.snapshot()
        assertEquals(2, snapshot.entities.size)
        assertEquals(1, snapshot.evidence.size)
        assertEquals(1, snapshot.relationStates.size)
        assertEquals(1, snapshot.transitions.size)

        driver.close()
    }

    @Test
    fun evaluateShouldBeReachableThroughFacade() {

        val driver =
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        DismDatabase.Schema.create(driver)

        val database = DismDatabase(driver)
        val persistence = newPersistence(database)

        val powerBank =
            Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm =
            Entity(type = EntityType.LOCATION, name = "宿舍")
        persistence.entities.saveAll(listOf(powerBank, dorm))

        val now =
            Instant.parse("2026-09-10T08:00:00Z")
        val evidence =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = now,
                confidence = 1.0,
            )
        persistence.evidence.save(evidence)

        val api: DismRuntimeApi =
            DismRuntime(persistence)

        val userId = Uuid.random()

        val evaluation =
            api.observeAndEvaluate(
                draft =
                    SemanticObservationDraft(
                        subjectMention =
                            EntityMention("充电宝", EntityType.ITEM),
                        predicate = Predicates.LOCATED_AT,
                        objectMention =
                            EntityMention("宿舍", EntityType.LOCATION),
                        confidence = 1.0,
                        evidenceIds = setOf(evidence.id),
                        observedAt = now,
                    ),
                context = Context(now = now),
                userId = userId,
                attentionPolicy = AttentionPolicy(),
            )

        assertIs<ObserveResult.Applied>(evaluation.observeResult)
        // 无 upcoming event → 不产生风险/建议，但链路经 Facade 完整可达
        assertTrue(evaluation.recommendations.isEmpty())

        driver.close()
    }

    @Test
    fun runtimeShouldBeASingleInstanceAccumulatingHistory() {

        val driver =
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        DismDatabase.Schema.create(driver)
        val database = DismDatabase(driver)

        val persistence = newPersistence(database)

        val powerBank =
            Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm =
            Entity(type = EntityType.LOCATION, name = "宿舍")
        val backpack =
            Entity(type = EntityType.LOCATION, name = "背包")
        persistence.entities.saveAll(listOf(powerBank, dorm, backpack))

        // 单实例：构造一次，连续 observe 复用同一个内部 Engine
        val api: DismRuntimeApi =
            DismRuntime(persistence)

        val t1 = Instant.parse("2026-09-10T08:00:00Z")
        val evidence1 =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = t1,
                confidence = 1.0,
            )
        persistence.evidence.save(evidence1)

        assertIs<ObserveResult.Applied>(
            api.observe(
                draft =
                    SemanticObservationDraft(
                        subjectMention = EntityMention("充电宝", EntityType.ITEM),
                        predicate = Predicates.LOCATED_AT,
                        objectMention = EntityMention("宿舍", EntityType.LOCATION),
                        confidence = 1.0,
                        evidenceIds = setOf(evidence1.id),
                        observedAt = t1,
                    ),
                context = Context(now = t1),
            )
        )

        val t2 = Instant.parse("2026-09-10T09:00:00Z")
        val evidence2 =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = t2,
                confidence = 1.0,
            )
        persistence.evidence.save(evidence2)

        assertIs<ObserveResult.Applied>(
            api.observe(
                draft =
                    SemanticObservationDraft(
                        subjectMention = EntityMention("充电宝", EntityType.ITEM),
                        predicate = Predicates.LOCATED_AT,
                        objectMention = EntityMention("背包", EntityType.LOCATION),
                        confidence = 1.0,
                        evidenceIds = setOf(evidence2.id),
                        observedAt = t2,
                    ),
                context = Context(now = t2),
            )
        )

        // 当前位置已是背包
        assertEquals(
            backpack.id,
            api.queryAt(
                instant = t2,
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            ).single().key.objectId,
        )

        // 关键：批量提交不应覆盖 append-only 历史——两次 transition 都在
        assertEquals(2, api.snapshot().transitions.size)

        driver.close()
    }
}
