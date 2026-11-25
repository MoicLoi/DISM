package me.moiclathy.dism.persistence

import me.moiclathy.dism.bootstrap.CoreBootstrapper
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.persistence.repository.SqlDelightEntityRepository
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import me.moiclathy.dism.persistence.repository.SqlDelightRelationStateRepository
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 真正模拟"关闭 DISM 再打开"：
 * Instance A 写入完整语义状态 → close → 同文件 Instance B 恢复全部。
 */
class DatabaseReopenIntegrationTest {

    @Test
    fun semanticStateShouldSurviveApplicationRestart() {

        val file =
            File.createTempFile(
                "dism-integration-",
                ".db",
            )

        // createTempFile 已创建空文件，Factory 用 exists() 判 Schema → 删掉
        file.delete()

        try {

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

            // ---- Application instance A ----
            run {
                val driver =
                    JvmDatabaseDriverFactory(file)
                        .createDriver()

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

                entities.saveAll(
                    listOf(powerBank, dormitory)
                )
                evidence.save(observation)
                relationStates.replaceAll(listOf(state))
                transitionLog.append(record)

                driver.close()
            }

            // ---- Application instance B（重启，走 DismPersistenceFactory 组装）----
            val restored =
                CoreBootstrapper(
                    DismPersistenceFactory(
                        JvmDatabaseDriverFactory(file)
                    ).create()
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

        } finally {
            file.delete()
        }
    }
}
