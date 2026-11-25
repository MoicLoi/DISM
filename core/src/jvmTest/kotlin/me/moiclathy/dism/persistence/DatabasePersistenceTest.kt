package me.moiclathy.dism.persistence

import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * 真文件跨生命周期测试：
 * 写入 SQLite → 关闭 driver（模拟进程退出）→ 重新打开同一文件 → Evidence 仍在。
 */
class DatabasePersistenceTest {

    @Test
    fun evidenceShouldSurviveDatabaseReopen() {

        val file =
            File.createTempFile(
                "dism-test-",
                ".db",
            )

        // createTempFile 已创建空文件，
        // 而 Factory 用 exists() 判断是否建 Schema → 先删掉
        file.delete()

        try {

            val original =
                Evidence(
                    type =
                        EvidenceType.EXPLICIT_STATEMENT,
                    source = "user",
                    observedAt =
                        Instant.parse(
                            "2026-09-10T03:00:00Z",
                        ),
                    confidence = 0.95,
                )

            // 第一次"运行"
            val firstDriver =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val firstDatabase =
                DismDatabase(firstDriver)

            val firstRepository =
                SqlDelightEvidenceRepository(
                    firstDatabase,
                )

            firstRepository.save(original)

            firstDriver.close()

            // 模拟重新启动应用
            val secondDriver =
                JvmDatabaseDriverFactory(file)
                    .createDriver()

            val secondDatabase =
                DismDatabase(secondDriver)

            val secondRepository =
                SqlDelightEvidenceRepository(
                    secondDatabase,
                )

            assertEquals(
                original,
                secondRepository.find(
                    original.id,
                ),
            )

            secondDriver.close()

        } finally {
            file.delete()
        }
    }
}
