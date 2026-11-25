package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.api.DynamicScheduleDecisionView
import me.moiclathy.dism.schedule.api.DynamicScheduleEvaluation
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryEntry
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryFacade
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryQuery
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryResult
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleId

/**
 * 15.3：Schedule Facade Boundary 契约。
 *
 * ### 这些断言真正在验证什么
 *
 * 契约测试最容易写成"断言一些永远为真的东西"。
 * 这里刻意避开那条路：
 *
 * - **签名**由编译期保证（下面的 lambda 如果签名不符，
 *   这个文件编译不过），所以不需要运行时 `is` 断言
 * - **语义**才需要运行时断言，所以下面断言的是
 *   `attentionRequiredCount` / `latest` / `isEmpty` 这些
 *   真的会算错的东西
 *
 * 另外还要证明一件事：这个 Facade 上**没有**出现内部类型。
 * 那一点同样由编译期保证——如果 `DynamicScheduleDecisionView`
 * 里嵌了 Snapshot，下面的构造不可能写得出来。
 */
class DynamicScheduleFacadeContractTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val now =
        Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun facadeExposesStableCapabilities() {

        /*
         * 编译期契约：以下三个函数值只有在 Facade 的签名
         * 完全符合 15.3 时才可能通过编译。
         */
        val evaluate:
            suspend (
                DynamicScheduleFacade,
                DynamicScheduleModel,
                Instant,
            ) -> DynamicScheduleEvaluation =
            { facade, model, at ->
                facade.evaluate(
                    model = model,
                    now = at,
                )
            }

        val latest:
            suspend (
                DynamicScheduleFacade,
                DynamicScheduleItemId,
            ) -> DynamicScheduleDecisionView? =
            { facade, id ->
                facade.latestDecision(id)
            }

        /*
         * history 是**属性**而不是方法，这一点也要锁住。
         */
        val historyAccessor:
            (DynamicScheduleFacade) ->
                DynamicScheduleHistoryFacade =
            { facade -> facade.history }

        assertNotNull(evaluate)
        assertNotNull(latest)
        assertNotNull(historyAccessor)
    }

    @Test
    fun evaluationCountsAttentionRequired() {

        val evaluation =
            DynamicScheduleEvaluation(
                evaluatedAt = now,
                decisions =
                    listOf(
                        view(attentionRequired = true),
                        view(attentionRequired = false),
                        view(attentionRequired = null),
                    ),
            )

        /*
         * `null` 表示"这次快照没有给出新评估"，
         * 它不是 true，也不是 false。
         */
        assertEquals(
            1,
            evaluation.attentionRequiredCount,
        )

        assertEquals(3, evaluation.decisions.size)
    }

    @Test
    fun historyResultExposesLatestAndEmptiness() {

        val empty =
            DynamicScheduleHistoryResult(
                entries = emptyList(),
            )

        assertTrue(empty.isEmpty)
        assertNull(empty.latest)
        assertEquals(0, empty.totalMatched)

        val entry =
            DynamicScheduleHistoryEntry(
                itemId = itemId,
                capturedAt = now,
                scheduleRevision = 3L,
                engineVersion = "test",
                hasTrace = true,
            )

        val result =
            DynamicScheduleHistoryResult(
                entries = listOf(entry),
                totalMatched = 5,
            )

        assertTrue(!result.isEmpty)

        /*
         * latest 是"第一条"，不是"最后一条"——
         * 历史查询按时间倒序返回。
         */
        assertEquals(entry, result.latest)

        /*
         * totalMatched 与 entries.size 可以不同：
         * 后者可能被 limit 截断。
         */
        assertEquals(1, result.entries.size)
        assertEquals(5, result.totalMatched)
    }

    @Test
    fun historyQueryRejectsNonPositiveLimit() {

        assertEquals(20, DynamicScheduleHistoryQuery().limit)

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DynamicScheduleHistoryQuery(limit = 0)
        }
    }

    /**
     * 构造一个只含公共字段的决策视图。
     *
     * 这里能用 `DynamicScheduleItemId` / `Boolean?` / `String?`
     * 直接构造，本身就证明了公共边界上没有内部类型。
     */
    private fun view(
        attentionRequired: Boolean?,
    ) =
        DynamicScheduleDecisionView(
            itemId = itemId,
            title = "实验动物学",
            priority = 0.6,
            attentionRequired = attentionRequired,
            explanation = null,
        )

    @Suppress("unused")
    private val unusedModelId =
        DynamicScheduleId("schedule-1")
}
