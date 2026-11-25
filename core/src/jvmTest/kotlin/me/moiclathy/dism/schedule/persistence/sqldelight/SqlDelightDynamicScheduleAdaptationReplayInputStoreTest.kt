package me.moiclathy.dism.schedule.persistence.sqldelight

import java.io.File

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.replay.DynamicScheduleAdaptationReplayInput
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData

/**
 * 14.12
 *
 * Replay Input 的真实落盘。
 *
 * 这条链比适配结果更长：
 *
 * ```text
 * Model + Attention History + Policy
 *     → JSON
 *     → SQLite TEXT
 *     → JSON
 *     → Model + History + Policy
 * ```
 *
 * 任何一段丢字段都会让 14.12 的重放产生假差异，
 * 所以这里验证的是**完整对象相等**。
 */
class SqlDelightDynamicScheduleAdaptationReplayInputStoreTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private fun input() =
        DynamicScheduleAdaptationReplayInput(
            model = DynamicScheduleReplayTestData.model(),

            histories =
                mapOf(
                    itemId.value to
                        DynamicScheduleAttentionHistory(
                            attentionKey = itemId.value,
                            shownCount = 12,
                            engagedCount = 4,
                            ignoredCount = 3,
                            dismissedCount = 1,
                            requestedCount = 2,
                            feedbackSignal = -0.5,
                        ),
                ),

            policy =
                DynamicSchedulePriorityAdaptationPolicy(
                    ignoredPenalty = 0.07,
                    dismissedPenalty = 0.13,
                    engagedBonus = 0.19,
                    requestedBonus = 0.23,
                    feedbackWeight = 0.29,
                ),
        )

    @Test
    fun replayInputSurvivesReopeningTheDatabase() = runTest {

        val file =
            File.createTempFile("dism-adaptation-replay-", ".db")

        file.delete()

        try {

            val firstDriver =
                JvmDatabaseDriverFactory(file).createDriver()

            SqlDelightDynamicScheduleAdaptationReplayInputStore(
                DismDatabase(firstDriver),
            ).save(itemId, input())

            firstDriver.close()

            val secondDriver =
                JvmDatabaseDriverFactory(file).createDriver()

            val restored =
                assertNotNull(
                    SqlDelightDynamicScheduleAdaptationReplayInputStore(
                        DismDatabase(secondDriver),
                    ).get(itemId),
                )

            assertEquals(input(), restored)

            assertEquals(
                input().policy,
                restored.policy,
            )

            secondDriver.close()

        } finally {

            file.delete()
        }
    }

    @Test
    fun deleteAndClearWork() = runTest {

        val file =
            File.createTempFile("dism-adaptation-replay-", ".db")

        file.delete()

        val driver =
            JvmDatabaseDriverFactory(file).createDriver()

        try {

            val store =
                SqlDelightDynamicScheduleAdaptationReplayInputStore(
                    DismDatabase(driver),
                )

            store.save(itemId, input())

            assertNotNull(store.get(itemId))

            store.delete(itemId)

            assertNull(store.get(itemId))

            store.save(itemId, input())

            store.clear()

            assertNull(store.get(itemId))

        } finally {

            driver.close()

            file.delete()
        }
    }
}
