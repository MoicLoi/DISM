package me.moiclathy.dism.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.repository.SqlDelightEntityRepository
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import me.moiclathy.dism.persistence.repository.SqlDelightRelationStateRepository
import me.moiclathy.dism.persistence.repository.SqlDelightStateCommitRepository
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.runtime.DismRuntime
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

/**
 * 端到端：经单实例 DismRuntime（DismRuntimeApi）写入语义状态，
 * 再构造一个新的 Runtime 实例模拟"重新启动 App"——
 * 新实例从 persistence 恢复后能读到旧状态，并能继续 observe。
 */
class SemanticStatePersistenceIntegrationTest {

    @Test
    fun runtimeShouldRestoreStateAndContinueObserving() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DismDatabase.Schema.create(driver)
        val database = DismDatabase(driver)

        val persistence = DismPersistence(
            entities = SqlDelightEntityRepository(database),
            evidence = SqlDelightEvidenceRepository(database),
            relationStates = SqlDelightRelationStateRepository(database),
            transitionLog = SqlDelightTransitionLogRepository(database),
            stateCommit = SqlDelightStateCommitRepository(database),
        )

        val powerBank = Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
        val backpack = Entity(type = EntityType.LOCATION, name = "背包")
        persistence.entities.saveAll(listOf(powerBank, dorm, backpack))

        val firstTime = Instant.parse("2026-09-10T08:00:00Z")
        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 1.0,
        )
        persistence.evidence.save(firstEvidence)

        // App instance A：单实例 Runtime，直接 observe（无需 restore 入口）
        val apiA: DismRuntimeApi = DismRuntime(persistence)
        val firstResult = apiA.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention("充电宝", EntityType.ITEM),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention("宿舍", EntityType.LOCATION),
                confidence = 1.0,
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )
        assertIs<ObserveResult.Applied>(firstResult)

        assertEquals(1, persistence.relationStates.loadAll().size)
        assertEquals(1, persistence.transitionLog.all().size)

        // App instance B：构造新的单实例 Runtime，构造时即从 persistence 恢复
        val apiB: DismRuntimeApi = DismRuntime(persistence)
        assertEquals(
            dorm.id,
            apiB.queryAt(
                instant = firstTime,
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            ).single().key.objectId,
        )

        val secondTime = Instant.parse("2026-09-10T09:00:00Z")
        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = secondTime,
            confidence = 1.0,
        )
        persistence.evidence.save(secondEvidence)

        val secondResult = apiB.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention("充电宝", EntityType.ITEM),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention("背包", EntityType.LOCATION),
                confidence = 1.0,
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )
        assertIs<ObserveResult.Applied>(secondResult)

        assertEquals(
            backpack.id,
            apiB.queryAt(
                instant = secondTime,
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            ).single().key.objectId,
        )
        assertEquals(2, persistence.transitionLog.all().size)

        driver.close()
    }
}
