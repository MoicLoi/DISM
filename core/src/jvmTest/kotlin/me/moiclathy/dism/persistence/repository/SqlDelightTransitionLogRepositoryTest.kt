package me.moiclathy.dism.persistence.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.uuid.Uuid
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SqlDelightTransitionLogRepositoryTest {

    private class TestDatabase(
        val driver: JdbcSqliteDriver,
        val transitionRepository: SqlDelightTransitionLogRepository,
        val evidenceRepository: SqlDelightEvidenceRepository,
    ) {
        fun close() {
            driver.close()
        }
    }

    private fun createDatabase(): TestDatabase {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val database = DismDatabase(driver)

        return TestDatabase(
            driver = driver,
            transitionRepository =
                SqlDelightTransitionLogRepository(database),
            evidenceRepository =
                SqlDelightEvidenceRepository(database),
        )
    }

    @Test
    fun assertRelation_shouldRoundTripWithEvidence() {
        val db = createDatabase()

        val first = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:50:00Z"),
            confidence = 0.8,
        )

        val second = Evidence(
            type = EvidenceType.CHECKLIST_COMPLETION,
            source = "checklist",
            observedAt = Instant.parse("2026-09-10T08:00:00Z"),
            confidence = 0.7,
        )

        db.evidenceRepository.saveAll(
            listOf(first, second)
        )

        val original =
            StateTransitionRecord(
                effectiveAt =
                    Instant.parse(
                        "2026-09-10T08:00:00Z"
                    ),
                transition =
                    StateTransition.AssertRelation(
                        key = RelationKey(
                            subjectId = Uuid.random(),
                            predicate = Predicates.LOCATED_AT,
                            objectId = Uuid.random(),
                        ),
                        semanticConfidence = 0.9,
                        effectiveAt =
                            Instant.parse(
                                "2026-09-10T08:00:00Z"
                            ),
                        evidenceIds =
                            setOf(first.id, second.id),
                    ),
            )

        db.transitionRepository.append(original)

        val loaded =
            db.transitionRepository.all().single()

        assertEquals(original, loaded)

        db.close()
    }

    @Test
    fun supersedeRelation_shouldPreserveBothKeys() {
        val db = createDatabase()

        val evidence = Evidence(
            type = EvidenceType.USER_ACTION,
            source = "user",
            observedAt = Instant.parse("2026-09-10T08:05:00Z"),
            confidence = 1.0,
        )

        db.evidenceRepository.save(evidence)

        val original =
            StateTransitionRecord(
                effectiveAt =
                    Instant.parse(
                        "2026-09-10T08:05:00Z"
                    ),
                transition =
                    StateTransition.SupersedeRelation(
                        oldKey = RelationKey(
                            subjectId = Uuid.random(),
                            predicate = Predicates.LOCATED_AT,
                            objectId = Uuid.random(),
                        ),
                        newKey = RelationKey(
                            subjectId = Uuid.random(),
                            predicate = Predicates.INSIDE,
                            objectId = Uuid.random(),
                        ),
                        semanticConfidence = 0.85,
                        effectiveAt =
                            Instant.parse(
                                "2026-09-10T08:05:00Z"
                            ),
                        evidenceIds =
                            setOf(evidence.id),
                    ),
            )

        db.transitionRepository.append(original)

        val loaded =
            db.transitionRepository.all().single()

        assertEquals(original, loaded)

        db.close()
    }

    @Test
    fun at_shouldReturnOnlyRecordsForExactInstant() {
        val db = createDatabase()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T08:00:00Z"),
            confidence = 1.0,
        )

        db.evidenceRepository.save(evidence)

        val early =
            StateTransitionRecord(
                effectiveAt =
                    Instant.parse(
                        "2026-09-10T08:00:00Z"
                    ),
                transition =
                    StateTransition.InvalidateRelation(
                        key = RelationKey(
                            subjectId = Uuid.random(),
                            predicate = Predicates.LOCATED_AT,
                            objectId = Uuid.random(),
                        ),
                        effectiveAt =
                            Instant.parse(
                                "2026-09-10T08:00:00Z"
                            ),
                        evidenceIds =
                            setOf(evidence.id),
                    ),
            )

        val later =
            StateTransitionRecord(
                effectiveAt =
                    Instant.parse(
                        "2026-09-10T09:00:00Z"
                    ),
                transition =
                    StateTransition.AssertRelation(
                        key = RelationKey(
                            subjectId = Uuid.random(),
                            predicate = Predicates.CARRIED_BY,
                            objectId = Uuid.random(),
                        ),
                        semanticConfidence = 1.0,
                        effectiveAt =
                            Instant.parse(
                                "2026-09-10T09:00:00Z"
                            ),
                        evidenceIds =
                            setOf(evidence.id),
                    ),
            )

        db.transitionRepository.append(early)
        db.transitionRepository.append(later)

        val atEight =
            db.transitionRepository.at(
                Instant.parse(
                    "2026-09-10T08:00:00Z"
                )
            )

        assertEquals(1, atEight.size)
        assertEquals(early, atEight.single())

        assertEquals(
            2,
            db.transitionRepository.all().size,
        )

        db.close()
    }
}
