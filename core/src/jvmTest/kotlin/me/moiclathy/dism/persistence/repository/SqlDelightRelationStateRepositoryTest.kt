package me.moiclathy.dism.persistence.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SqlDelightRelationStateRepositoryTest {

    private fun createRepository(): Triple<
        SqlDelightRelationStateRepository,
        SqlDelightEvidenceRepository,
        JdbcSqliteDriver
        > {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val database = DismDatabase(driver)

        return Triple(
            SqlDelightRelationStateRepository(database),
            SqlDelightEvidenceRepository(database),
            driver,
        )
    }

    @Test
    fun activeStateWithEvidence_shouldRoundTripCompletely() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

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

        evidenceRepository.saveAll(
            listOf(first, second)
        )

        val original =
            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 0.94,
                supports = setOf(
                    RelationSupport(
                        evidenceId = first.id,
                        semanticConfidence = 1.0,
                    ),
                    RelationSupport(
                        evidenceId = second.id,
                        semanticConfidence = 1.0,
                    ),
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
            )

        repository.replaceAll(
            listOf(original)
        )

        val loaded =
            repository.loadAll().single()

        // 状态完全一致
        assertEquals(original, loaded)

        // evidenceIds 由 relation_support 恢复，不是 JSON
        assertEquals(
            setOf(first.id, second.id),
            loaded.evidenceIds,
        )

        assertEquals(
            setOf(
                RelationSupport(
                    evidenceId = first.id,
                    semanticConfidence = 1.0,
                ),
                RelationSupport(
                    evidenceId = second.id,
                    semanticConfidence = 1.0,
                ),
            ),
            loaded.supports,
        )

        driver.close()
    }

    @Test
    fun supersededState_shouldPreserveStatusAndBoundaries() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:00:00Z"),
            confidence = 1.0,
        )

        evidenceRepository.save(evidence)

        val original =
            RelationState(
                key = RelationKey(
                    subjectId = itemId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = evidence.id,
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
                validUntil = Instant.parse("2026-09-10T10:00:00Z"),
                status = RelationStatus.SUPERSEDED,
            )

        repository.replaceAll(
            listOf(original)
        )

        val loaded =
            repository.loadAll().single()

        assertEquals(
            RelationStatus.SUPERSEDED,
            loaded.status,
        )

        assertEquals(
            Instant.parse("2026-09-10T10:00:00Z"),
            loaded.validUntil,
        )

        assertEquals(
            false,
            loaded.isActive,
        )

        driver.close()
    }

    @Test
    fun queryActiveAt_shouldRespectHalfOpenInterval() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:00:00Z"),
            confidence = 1.0,
        )

        evidenceRepository.save(evidence)

        val state =
            RelationState(
                key = RelationKey(
                    subjectId = itemId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = evidence.id,
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
                validUntil = Instant.parse("2026-09-10T10:00:00Z"),
                status = RelationStatus.ACTIVE,
            )

        repository.replaceAll(
            listOf(state)
        )

        // 区间内（09:00）：应该存在
        assertEquals(
            1,
            repository.queryActiveAt(
                Instant.parse("2026-09-10T09:00:00Z")
            ).size,
        )

        // 区间右边界（10:00）：[validFrom, validUntil) → 不存在
        assertEquals(
            0,
            repository.queryActiveAt(
                Instant.parse("2026-09-10T10:00:00Z")
            ).size,
        )

        driver.close()
    }

    @Test
    fun queryActiveAt_shouldReturnAllActiveRelationsAndFilterInactive() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val userId = Uuid.random()
        val backpackId = Uuid.random()

        val time = Instant.parse("2026-09-10T03:00:00Z")

        // 不同 relation 可以同时存在（主键 = subject + predicate + object + validFrom 各不相同）
        val locatedAt =
            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
                status = RelationStatus.ACTIVE,
            )

        val carriedBy =
            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.CARRIED_BY,
                    objectId = userId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
                status = RelationStatus.ACTIVE,
            )

        // 同 validFrom 但已被替代的关系：status 过滤掉
        val supersededInside =
            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.INSIDE,
                    objectId = backpackId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
                validUntil = Instant.parse("2026-09-10T04:00:00Z"),
                status = RelationStatus.SUPERSEDED,
            )

        repository.replaceAll(
            listOf(locatedAt, carriedBy, supersededInside)
        )

        val result =
            repository.queryActiveAt(
                Instant.parse("2026-09-10T04:30:00Z")
            )

        // 只返回 2 条 ACTIVE，SUPERSEDED 被过滤
        assertEquals(2, result.size)

        assertEquals(
            setOf(Predicates.LOCATED_AT, Predicates.CARRIED_BY),
            result.map { it.key.predicate }.toSet(),
        )

        assertTrue(
            result.all { it.status == RelationStatus.ACTIVE }
        )

        driver.close()
    }

    @Test
    fun supersededState_shouldNotAppearInQueryActiveAt() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:00:00Z"),
            confidence = 1.0,
        )

        evidenceRepository.save(evidence)

        val superseded =
            RelationState(
                key = RelationKey(
                    subjectId = itemId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = evidence.id,
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
                validUntil = Instant.parse("2026-09-10T10:00:00Z"),
                status = RelationStatus.SUPERSEDED,
            )

        repository.replaceAll(
            listOf(superseded)
        )

        // 时间区间覆盖 09:00，但 status 已是 SUPERSEDED → 不应返回
        assertEquals(
            0,
            repository.queryActiveAt(
                Instant.parse("2026-09-10T09:00:00Z")
            ).size,
        )

        // loadAll 仍保留历史：SUPERSEDED 状态必须能恢复
        assertEquals(
            RelationStatus.SUPERSEDED,
            repository.loadAll().single().status,
        )

        driver.close()
    }

    @Test
    fun invalidatedState_shouldPreserveStatusAndBeExcludedFromQueryActiveAt() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:00:00Z"),
            confidence = 1.0,
        )

        evidenceRepository.save(evidence)

        val invalidated =
            RelationState(
                key = RelationKey(
                    subjectId = itemId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = evidence.id,
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
                validUntil = Instant.parse("2026-09-10T10:00:00Z"),
                status = RelationStatus.INVALIDATED,
            )

        repository.replaceAll(
            listOf(invalidated)
        )

        val loaded =
            repository.loadAll().single()

        assertEquals(
            RelationStatus.INVALIDATED,
            loaded.status,
        )

        assertEquals(
            Instant.parse("2026-09-10T10:00:00Z"),
            loaded.validUntil,
        )

        assertEquals(
            false,
            loaded.isActive,
        )

        // 时间区间覆盖 09:00，但 status 是 INVALIDATED → queryActiveAt 不返回
        assertEquals(
            0,
            repository.queryActiveAt(
                Instant.parse("2026-09-10T09:00:00Z")
            ).size,
        )

        driver.close()
    }

    @Test
    fun replaceAll_shouldReplacePreviousWorld() {
        val (repository, evidenceRepository, driver) =
            createRepository()

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = Instant.parse("2026-09-10T07:00:00Z"),
            confidence = 1.0,
        )

        evidenceRepository.save(evidence)

        val oldState =
            RelationState(
                key = RelationKey(
                    subjectId = Uuid.random(),
                    predicate = Predicates.LOCATED_AT,
                    objectId = Uuid.random(),
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = evidence.id,
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = Instant.parse("2026-09-10T08:00:00Z"),
            )

        repository.replaceAll(
            listOf(oldState)
        )

        // 第二次 replaceAll 只保留新世界
        val newState =
            RelationState(
                key = RelationKey(
                    subjectId = Uuid.random(),
                    predicate = Predicate("requires"),
                    objectId = Uuid.random(),
                ),
                confidence = 0.9,
                supports = emptySet(),
                validFrom = Instant.parse("2026-09-10T09:00:00Z"),
            )

        repository.replaceAll(
            listOf(newState)
        )

        val loaded =
            repository.loadAll()

        assertEquals(1, loaded.size)
        assertEquals(newState, loaded.single())

        driver.close()
    }
}
