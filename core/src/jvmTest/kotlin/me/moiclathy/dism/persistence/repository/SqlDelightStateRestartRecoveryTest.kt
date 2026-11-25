package me.moiclathy.dism.persistence.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 跨进程生命周期保持个人语义状态：
 * 写入 SQLite → 关闭 driver（模拟进程退出）→ 重新打开同一文件 → 世界状态完整恢复。
 */
class SqlDelightStateRestartRecoveryTest {

    @Test
    fun world_shouldSurviveAcrossProcessRestart() {
        val dbFile =
            File.createTempFile(
                "dism-restart-",
                ".db",
            )

        try {

            // ── 第一次"运行"：写入世界 ──
            val driver1 =
                fileDriver(dbFile)
            DismDatabase.Schema.create(driver1)

            val database1 = DismDatabase(driver1)
            val evidenceRepository1 =
                SqlDelightEvidenceRepository(database1)
            val stateRepository1 =
                SqlDelightRelationStateRepository(database1)

            val powerBankId = Uuid.random()
            val dormId = Uuid.random()
            val userId = Uuid.random()

            val locationEvidence = Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = Instant.parse("2026-09-10T07:00:00Z"),
                confidence = 0.8,
            )

            val carriedEvidence = Evidence(
                type = EvidenceType.USER_ACTION,
                source = "user",
                observedAt = Instant.parse("2026-09-10T08:00:00Z"),
                confidence = 1.0,
            )

            evidenceRepository1.saveAll(
                listOf(locationEvidence, carriedEvidence)
            )

            val oldLocation =
                RelationState(
                    key = RelationKey(
                        subjectId = powerBankId,
                        predicate = Predicates.LOCATED_AT,
                        objectId = dormId,
                    ),
                    confidence = 0.8,
                    supports = setOf(
                        RelationSupport(
                            evidenceId = locationEvidence.id,
                            semanticConfidence = 1.0,
                        )
                    ),
                    validFrom = Instant.parse("2026-09-10T07:00:00Z"),
                    validUntil = Instant.parse("2026-09-10T08:00:00Z"),
                    status = RelationStatus.SUPERSEDED,
                )

            val carried =
                RelationState(
                    key = RelationKey(
                        subjectId = powerBankId,
                        predicate = Predicates.CARRIED_BY,
                        objectId = userId,
                    ),
                    confidence = 1.0,
                    supports = setOf(
                        RelationSupport(
                            evidenceId = carriedEvidence.id,
                            semanticConfidence = 1.0,
                        )
                    ),
                    validFrom = Instant.parse("2026-09-10T08:00:00Z"),
                )

            stateRepository1.replaceAll(
                listOf(oldLocation, carried)
            )

            driver1.close()

            // ── 第二次"启动"：同一文件重开 ──
            val driver2 =
                fileDriver(dbFile)

            val database2 = DismDatabase(driver2)
            val evidenceRepository2 =
                SqlDelightEvidenceRepository(database2)
            val stateRepository2 =
                SqlDelightRelationStateRepository(database2)

            // 证据链恢复
            assertEquals(
                locationEvidence,
                evidenceRepository2.find(locationEvidence.id),
            )

            assertEquals(
                carriedEvidence,
                evidenceRepository2.find(carriedEvidence.id),
            )

            // 世界状态恢复
            val restored =
                stateRepository2.loadAll()

            assertEquals(2, restored.size)

            val restoredLocation =
                restored.first {
                    it.key.predicate == Predicates.LOCATED_AT
                }

            assertEquals(
                RelationStatus.SUPERSEDED,
                restoredLocation.status,
            )

            assertEquals(
                Instant.parse("2026-09-10T08:00:00Z"),
                restoredLocation.validUntil,
            )

            assertEquals(
                setOf(locationEvidence.id),
                restoredLocation.evidenceIds,
            )

            val restoredCarried =
                restored.first {
                    it.key.predicate == Predicates.CARRIED_BY
                }

            assertTrue(restoredCarried.isActive)

            assertEquals(
                setOf(carriedEvidence.id),
                restoredCarried.evidenceIds,
            )

            // 重启后 queryActiveAt 只返回当前 ACTIVE 状态
            val activeNow =
                stateRepository2.queryActiveAt(
                    Instant.parse("2026-09-10T09:00:00Z")
                )

            assertEquals(1, activeNow.size)
            assertEquals(
                Predicates.CARRIED_BY,
                activeNow.single().key.predicate,
            )

            driver2.close()

        } finally {
            dbFile.delete()
        }
    }

    private fun fileDriver(
        file: File,
    ): JdbcSqliteDriver {
        return JdbcSqliteDriver(
            "jdbc:sqlite:${file.absolutePath.replace('\\', '/')}"
        )
    }
}
