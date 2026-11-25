package me.moiclathy.dism.persistence.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SqlDelightEvidenceRepositoryTest {

    @Test
    fun evidence_shouldSurviveDatabaseRoundTrip() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val database =
            DismDatabase(driver)

        val repository =
            SqlDelightEvidenceRepository(
                database
            )

        val original =
            Evidence(
                type =
                    EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                confidence = 0.95,
            )

        repository.save(original)

        val loaded =
            repository.find(original.id)

        assertEquals(
            original,
            loaded,
        )

        driver.close()
    }

    @Test
    fun saveShouldReplaceExistingEvidence() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val database =
            DismDatabase(driver)

        val repository =
            SqlDelightEvidenceRepository(
                database
            )

        val original =
            Evidence(
                type =
                    EvidenceType.EXPLICIT_STATEMENT,
                source = "old-source",
                observedAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                confidence = 0.5,
            )

        repository.save(original)

        val updated =
            original.copy(
                source = "new-source",
                confidence = 0.95,
            )

        repository.save(updated)

        assertEquals(
            updated,
            repository.find(original.id),
        )

        // INSERT OR REPLACE：相同 UUID 不会产生两条 Evidence
        assertEquals(
            1,
            repository.all().size,
        )

        driver.close()
    }

    @Test
    fun saveAll_shouldPersistEveryEvidence() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(driver)

        val repository =
            SqlDelightEvidenceRepository(
                DismDatabase(driver)
            )

        val first =
            Evidence(
                type = EvidenceType.USER_ACTION,
                source = "app",
                observedAt =
                    Instant.parse(
                        "2026-09-10T03:00:00Z"
                    ),
                confidence = 1.0,
            )

        val second =
            Evidence(
                type = EvidenceType.SCHEDULE_IMPORT,
                source = "xiaoqier",
                observedAt =
                    Instant.parse(
                        "2026-09-10T04:00:00Z"
                    ),
                confidence = 0.8,
            )

        repository.saveAll(
            listOf(first, second)
        )

        assertEquals(
            setOf(first.id, second.id),
            repository.all()
                .map { it.id }
                .toSet(),
        )

        driver.close()
    }
}
