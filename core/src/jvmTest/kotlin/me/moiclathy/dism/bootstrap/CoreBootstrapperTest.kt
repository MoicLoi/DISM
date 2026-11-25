package me.moiclathy.dism.bootstrap

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 完整语义状态恢复测试：
 * Entity / Evidence / RelationState / TransitionLog
 * 全部写入 SQLite 后，经 DismPersistence + CoreBootstrapper 完整恢复。
 */
class CoreBootstrapperTest {

    @Test
    fun coreStateShouldSurviveDatabaseReopen() {

        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val database =
            DismDatabase(driver)

        val entities =
            SqlDelightEntityRepository(database)

        val evidence =
            SqlDelightEvidenceRepository(database)

        val relationStates =
            SqlDelightRelationStateRepository(database)

        val transitionLog =
            SqlDelightTransitionLogRepository(database)

        val powerBank =
            Entity(
                type = EntityType.ITEM,
                name = "充电宝",
            )

        val dormitory =
            Entity(
                type = EntityType.LOCATION,
                name = "宿舍",
            )

        entities.save(powerBank)
        entities.save(dormitory)

        val observation =
            Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                confidence = 0.95,
            )

        evidence.save(observation)

        val state =
            RelationState(
                key =
                    RelationKey(
                        subjectId = powerBank.id,
                        predicate = Predicates.LOCATED_AT,
                        objectId = dormitory.id,
                    ),
                confidence = 0.95,
                supports =
                    setOf(
                        RelationSupport(
                            evidenceId = observation.id,
                            semanticConfidence = 0.95,
                        )
                    ),
                validFrom =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
            )

        relationStates.replaceAll(
            listOf(state),
        )

        val record =
            StateTransitionRecord(
                effectiveAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                transition =
                    StateTransition.AssertRelation(
                        key =
                            RelationKey(
                                subjectId = powerBank.id,
                                predicate = Predicates.LOCATED_AT,
                                objectId = dormitory.id,
                            ),
                        semanticConfidence = 0.95,
                        effectiveAt =
                            Instant.parse(
                                "2026-09-10T03:00:00Z"
                            ),
                        evidenceIds =
                            setOf(observation.id),
                    ),
            )

        transitionLog.append(record)

        // 通过容器 + Bootstrapper 恢复整个长期状态
        val restored =
            CoreBootstrapper(
                DismPersistence(
                    entities = entities,
                    evidence = evidence,
                    relationStates = relationStates,
                    transitionLog = transitionLog,
                )
            ).restore()

        assertEquals(
            listOf(powerBank, dormitory),
            restored.entities,
        )

        assertEquals(
            listOf(observation),
            restored.evidence,
        )

        assertEquals(
            listOf(state),
            restored.relationStates,
        )

        assertTrue(
            restored.relationStates
                .single()
                .evidenceIds
                .contains(observation.id),
        )

        assertEquals(
            listOf(record),
            restored.transitions,
        )

        driver.close()
    }
}
