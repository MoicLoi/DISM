package me.moiclathy.dism.schedule.persistence.sqldelight

import java.io.File

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleHumanOverride
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation.persistence.DynamicScheduleAdaptationPersistenceRecord
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * 真实的 SQLite 落盘。
 *
 * 与内存版测试的区别在于这里验证的是：
 *
 * ```text
 * 关掉连接 → 重新打开 → 记录还在，而且逐字段相等
 * ```
 *
 * 也就是说 JSON 编解码 + 表结构 + schema 校验这条链路都真的通了。
 */
class SqlDelightDynamicScheduleAdaptationPersistenceStoreTest {

    private val itemId =
        DynamicScheduleItemId("course-001")

    private val persistedAt =
        kotlin.time.Instant.parse("2026-09-24T07:20:00Z")

    private fun record(
        override: DynamicScheduleHumanOverride? = null,
    ) =
        DynamicScheduleAdaptationPersistenceRecord(
            itemId = itemId,
            adaptation =
                DynamicSchedulePriorityAdaptation(
                    itemId = itemId,
                    basePriority = 0.5,
                    adaptationFactor = 1.3,
                    effectivePriority = 0.65,
                    reasons = listOf("Context matched after_class"),
                ),
            humanOverride = override,
            persistedAt = persistedAt,
        )

    @Test
    fun recordSurvivesReopeningTheDatabase() = runTest {

        val file =
            File.createTempFile("dism-adaptation-", ".db")

        file.delete()

        try {

            /*
             * 第一次打开：写入。
             */
            val firstDriver =
                JvmDatabaseDriverFactory(file).createDriver()

            val firstStore =
                SqlDelightDynamicScheduleAdaptationPersistenceStore(
                    DismDatabase(firstDriver),
                )

            firstStore.save(record())

            firstDriver.close()

            /*
             * 第二次打开：全新 driver + 全新 database。
             */
            val secondDriver =
                JvmDatabaseDriverFactory(file).createDriver()

            val secondStore =
                SqlDelightDynamicScheduleAdaptationPersistenceStore(
                    DismDatabase(secondDriver),
                )

            val restored =
                assertNotNull(secondStore.get(itemId))

            assertEquals(
                record().adaptation,
                restored.adaptation,
            )

            assertEquals(
                persistedAt,
                restored.persistedAt,
            )

            assertEquals(1L, restored.schemaVersion)

            secondDriver.close()

        } finally {

            file.delete()
        }
    }

    @Test
    fun humanOverrideSurvivesTheRoundTrip() = runTest {

        withStore { store ->

            val override =
                DynamicScheduleHumanOverride(
                    itemId = itemId,
                    priorityOverride = 0.9,
                    createdAt = persistedAt,
                    reason = "User explicitly marked important",
                )

            store.save(
                record(override = override),
            )

            val restored =
                assertNotNull(store.get(itemId))

            assertEquals(
                override,
                restored.humanOverride,
            )

            /*
             * 自动结果没有被 override 覆盖掉。
             */
            assertEquals(
                0.65,
                restored.adaptation.effectivePriority,
                absoluteTolerance = 0.000001,
            )
        }
    }

    @Test
    fun deleteAndClearWork() = runTest {

        withStore { store ->

            store.save(record())

            assertNotNull(store.get(itemId))

            store.delete(itemId)

            kotlin.test.assertNull(store.get(itemId))

            store.save(record())

            store.clear()

            kotlin.test.assertNull(store.get(itemId))
        }
    }

    @Test
    fun schemaMismatchIsDetected() = runTest {

        withStore { store, queries ->

            store.save(record())

            /*
             * 模拟"表里的 schema 版本被改过"。
             */
            queries.insertOrReplace(
                item_id = itemId.value,
                payload_json =
                    DynamicScheduleAdaptationSqlDelightCodec
                        .encode(
                            me.moiclathy.dism.schedule.adaptation
                                .persistence
                                .DynamicScheduleAdaptationPersistencePayload(
                                    adaptation = record().adaptation,
                                    humanOverride = null,
                                    schemaVersion = 1L,
                                ),
                        ),
                persisted_at_epoch_ms =
                    persistedAt.toEpochMilliseconds(),
                schema_version = 2L,
            )

            assertFailsWith<IllegalArgumentException> {
                store.get(itemId)
            }
        }
    }

    private suspend fun withStore(
        body: suspend (
            SqlDelightDynamicScheduleAdaptationPersistenceStore,
        ) -> Unit,
    ) {
        val file =
            File.createTempFile("dism-adaptation-", ".db")

        file.delete()

        val driver =
            JvmDatabaseDriverFactory(file).createDriver()

        try {

            body(
                SqlDelightDynamicScheduleAdaptationPersistenceStore(
                    DismDatabase(driver),
                ),
            )

        } finally {

            driver.close()

            file.delete()
        }
    }

    private suspend fun withStore(
        body: suspend (
            SqlDelightDynamicScheduleAdaptationPersistenceStore,
            me.moiclathy.dism.persistence.db.ScheduleAdaptationQueries,
        ) -> Unit,
    ) {
        val file =
            File.createTempFile("dism-adaptation-", ".db")

        file.delete()

        val driver =
            JvmDatabaseDriverFactory(file).createDriver()

        try {

            val database = DismDatabase(driver)

            body(
                SqlDelightDynamicScheduleAdaptationPersistenceStore(
                    database,
                ),
                database.scheduleAdaptationQueries,
            )

        } finally {

            driver.close()

            file.delete()
        }
    }
}
