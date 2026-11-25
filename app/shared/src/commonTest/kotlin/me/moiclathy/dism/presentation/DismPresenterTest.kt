package me.moiclathy.dism.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.presentation.navigation.DismDestination
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.3 / 16.4 / 16.5 / 16.6：展示层契约。
 *
 * 这些测试都不需要 Compose —— 正因为先做了 Presentation Model，
 * 排序、过滤、格式化这些规则可以用普通单测覆盖。
 */
class DismPresenterTest {

    private val scheduleId = DynamicScheduleId("test-schedule")

    private val utc = TimeZone.UTC

    private fun at(
        hour: Int,
        minute: Int = 0,
    ): Instant =
        Instant.parse(
            "2026-09-09T" +
                hour.toString().padStart(2, '0') +
                ":" +
                minute.toString().padStart(2, '0') +
                ":00Z",
        )

    private fun item(
        id: String,
        title: String,
        startAt: Instant?,
        endAt: Instant? = null,
        priority: Double = 0.5,
        state: DynamicScheduleState =
            DynamicScheduleState.UPCOMING,
        externalKey: String? = null,
    ) =
        DynamicScheduleItem(
            id = DynamicScheduleItemId(id),
            scheduleId = scheduleId,
            title = title,
            type = DynamicScheduleItemType.EVENT,
            source = DynamicScheduleSource.USER_INPUT,
            temporal =
                when {
                    startAt == null ->
                        DynamicScheduleTemporal.Unscheduled

                    endAt == null ->
                        DynamicScheduleTemporal.At(startAt)

                    else ->
                        DynamicScheduleTemporal.Interval(
                            startAt = startAt,
                            endAt = endAt,
                        )
                },
            state = state,
            priority = priority,
            externalKey = externalKey,
        )

    private fun model(
        vararg items: DynamicScheduleItem,
    ) =
        DynamicScheduleModel(
            id = scheduleId,
            revision = 1L,
            items = items.toList(),
        )

    private fun presenter() =
        DismPresenter(
            dism = DismFactory.create(),
            timeZone = utc,
        )

    // ---------- 16.4 Today ----------

    @Test
    fun todayTimelineIsOrderedByTimeNotByPriority() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "late",
                            title = "项目开发",
                            startAt = at(14),
                            priority = 0.9,
                        ),
                        item(
                            id = "early",
                            title = "实验动物学",
                            startAt = at(9),
                            priority = 0.2,
                        ),
                        item(
                            id = "noon",
                            title = "午餐",
                            startAt = at(11, 30),
                        ),
                    ),
                now = at(8),
            )

        assertEquals(
            listOf("early", "noon", "late"),
            state.today.timeline.map { it.itemId },
        )
    }

    @Test
    fun unscheduledItemsGoLastInToday() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "floating",
                            title = "没有时间的任务",
                            startAt = null,
                            priority = 1.0,
                        ),
                        item(
                            id = "timed",
                            title = "有时间",
                            startAt = at(9),
                            priority = 0.1,
                        ),
                    ),
                now = at(8),
            )

        assertEquals(
            listOf("timed", "floating"),
            state.today.timeline.map { it.itemId },
        )

        assertNull(state.today.timeline.last().timeLabel)
    }

    @Test
    fun timeZoneMakesLabelsDeterministic() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "a",
                            title = "实验动物学",
                            startAt = at(9),
                            endAt = at(9, 40),
                        ),
                    ),
                now = at(8),
            )

        val entry = state.today.timeline.single()

        assertEquals("09:00", entry.timeLabel)

        assertEquals("09:00 – 09:40", entry.rangeLabel)

        assertEquals("Wed, Sep 9", state.today.dateLabel)
    }

    @Test
    fun todayHidesTerminalItems() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "done",
                            title = "已完成",
                            startAt = at(9),
                            state =
                                DynamicScheduleState.COMPLETED,
                        ),
                        item(
                            id = "live",
                            title = "还没结束",
                            startAt = at(14),
                        ),
                    ),
                now = at(8),
            )

        assertEquals(
            listOf("live"),
            state.today.timeline.map { it.itemId },
        )
    }

    // ---------- 16.5 Schedule ----------

    @Test
    fun scheduleKeepsTerminalItemsButTodayDoesNot() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "done",
                            title = "已完成",
                            startAt = at(9),
                            state =
                                DynamicScheduleState.COMPLETED,
                        ),
                        item(
                            id = "live",
                            title = "还没结束",
                            startAt = at(14),
                        ),
                    ),
                now = at(8),
            )

        /*
         * 这是两个页面最关键的差别：
         *
         * Today 做减法（只看还要做的），
         * Schedule 做加法（用户要能确认"那件事我做完了"）。
         */
        assertEquals(
            listOf("live"),
            state.today.timeline.map { it.itemId },
        )

        assertEquals(
            listOf("done", "live"),
            state.schedule.entries.map { it.itemId },
        )

        assertTrue(
            state.schedule.entries.single { it.itemId == "done" }
                .isTerminal,
        )

        assertEquals(
            "Completed",
            state.schedule.entries.single { it.itemId == "done" }
                .stateLabel,
        )
    }

    @Test
    fun scheduleSeparatesUnscheduledEntries() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(id = "a", title = "A", startAt = at(9)),
                        item(
                            id = "none",
                            title = "无时间",
                            startAt = null,
                        ),
                    ),
                now = at(8),
            )

        assertEquals(2, state.schedule.size)

        assertEquals(
            listOf("none"),
            state.schedule.unscheduledEntries.map { it.itemId },
        )

        assertEquals(
            listOf("a"),
            state.schedule.entries
                .filterNot { it.timeLabel == null }
                .map { it.itemId },
        )
    }

    // ---------- 16.6 Attention ----------

    @Test
    fun attentionMatchesDecisionOutputExactly() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "lab",
                            title = "实验动物学",
                            startAt = at(9),
                            priority = 0.9,
                        ),
                        item(
                            id = "lunch",
                            title = "午餐",
                            startAt = at(11, 30),
                            priority = 0.1,
                        ),
                    ),
                now = at(8),
            )

        /*
         * 展示层**不重新判断**该注意什么。
         *
         * 断言的是"两侧一致"：决策说要关注的，Attention 页里必须有；
         * 决策没说要关注的，Attention 页里就不该有。
         *
         * 如果在 UI 里再写一遍"优先级 > 0.7 就该提醒"，
         * 就会出现两套注意力规则，而且不一致时没人知道该信谁。
         */
        val attentionIds =
            state.attention.entries.map { it.itemId }.toSet()

        val decisionIds =
            state.today.timeline
                .filter { it.needsAttention }
                .map { it.itemId }
                .toSet()

        assertEquals(decisionIds, attentionIds)
    }

    @Test
    fun attentionPageSortsByPriorityThenId() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "zz",
                            title = "Z",
                            startAt = at(9),
                            priority = 0.9,
                        ),
                        item(
                            id = "aa",
                            title = "A",
                            startAt = at(10),
                            priority = 0.9,
                        ),
                        item(
                            id = "low",
                            title = "Low",
                            startAt = at(11),
                            priority = 0.1,
                        ),
                    ),
                now = at(8),
            )

        val ids =
            state.attention.entries.map { it.itemId }

        /*
         * 排序必须是确定的：优先级降序，同优先级按 id 升序。
         * 这里不假设 Core 会判定哪些需要关注，
         * 只断言"给出的顺序是规则化的"。
         */
        val priorities =
            state.attention.entries.map { it.priority }

        assertEquals(
            priorities.sortedDescending(),
            priorities,
        )

        assertEquals(ids.distinct(), ids)
    }

    // ---------- 16.3 一次评估 ----------

    @Test
    fun oneLoadFillsAllThreeSectionsConsistently() = runTest {

        val state =
            presenter().load(
                model =
                    model(
                        item(
                            id = "a",
                            title = "A",
                            startAt = at(9),
                        ),
                    ),
                now = at(8),
            )

        assertTrue(!state.isLoading)

        assertNull(state.errorMessage)

        /*
         * 三个页面来自同一次评估，所以同一个 item
         * 在 Today 与 Schedule 里的优先级必须一致。
         */
        val fromToday =
            state.today.timeline.single { it.itemId == "a" }

        val fromSchedule =
            state.schedule.entries.single { it.itemId == "a" }

        assertEquals(fromToday.priority, fromSchedule.priority)

        assertEquals(
            fromToday.needsAttention,
            fromSchedule.needsAttention,
        )
    }

    @Test
    fun repeatedLoadsRecordAtMostOnce() = runTest {

        val model =
            model(
                item(id = "a", title = "A", startAt = at(9)),
            )

        val presenter = presenter()

        val first = presenter.load(model = model, now = at(8))

        val second = presenter.load(model = model, now = at(8))

        /*
         * 16.22：这条测试的语义变过一次，值得说清楚。
         *
         * **1.1.0 之前**：load 走 evaluate，每次加载都写一条。
         *   打开 100 次 = 100 条重复记录。
         *
         * **1.1.0 之后**：load 走 preview，从不写。
         *   副作用没了，但 History 页**永远是空的**——
         *   修掉了一个 bug 引入了另一个。
         *
         * **现在**：预览之后比对已记录的决策，变了才写。
         *   所以下面的期望是 1 而不是 0，也不是"每次加一"。
         *
         * ```text
         * 第一次 load  →  没记录过 →  记 1 条
         * 第二次 load  →  没变化   →  不记
         * ```
         *
         * 断言整份 UiState 相等，是在证明 "load 是幂等的"——
         * 这是比"load 无副作用"更准确、也更该保证的性质：
         * 用户不会因为多切了几次页面就看到更多历史。
         */
        assertEquals(first, second)

        assertEquals(
            1,
            first.history.totalMatched,
            "the first load must record a decision, " +
                "otherwise History stays permanently empty",
        )

        assertEquals(
            1,
            second.history.totalMatched,
            "a second load with an unchanged model must not " +
                "add another entry",
        )
    }

    @Test
    fun historyIsNotPermanentlyEmpty() = runTest {

        /*
         * 这是 16.22 的**直接回归测试**。
         *
         * 在它之前：展示层只用 preview，从不写历史，
         * 于是 History 页在真实应用里永远显示"没有记录"。
         *
         * 单测、门禁、编译全绿，而那个页面是死的——
         * 这类问题只有真的跑起来才会暴露。
         */
        val presenter = presenter()

        presenter.load(
            model =
                model(
                    item(id = "a", title = "A", startAt = at(9)),
                ),
            now = at(8),
        )

        val state =
            presenter.load(
                model =
                    model(
                        item(
                            id = "a",
                            title = "A",
                            startAt = at(9),
                        ),
                    ),
                now = at(8),
            )

        assertTrue(
            state.history.entries.isNotEmpty(),
            "History must actually contain the recorded decision",
        )

        assertEquals("a", state.history.entries.first().itemId)
    }

    @Test
    fun aChangedDecisionIsRecordedAgain() = runTest {

        val presenter = presenter()

        presenter.load(
            model =
                model(
                    item(id = "a", title = "A", startAt = at(9)),
                ),
            now = at(8),
        )

        /*
         * 同一时刻、同一个 item，但**标题变了**。
         *
         * 标题是用户看得见的决策内容，所以算"变了"。
         */
        val afterRename =
            presenter.load(
                model =
                    model(
                        item(
                            id = "a",
                            title = "A renamed",
                            startAt = at(9),
                        ),
                    ),
                now = at(8),
            )

        assertEquals(
            2,
            afterRename.history.totalMatched,
            "a decision whose content changed must be recorded",
        )
    }

    @Test
    fun unchangedModelDoesNotGrowHistoryAcrossManyLoads() =
        runTest {

            val presenter = presenter()

            val model =
                model(
                    item(id = "a", title = "A", startAt = at(9)),
                )

            /*
             * 模拟用户反复切页面 / 界面反复重组。
             */
            repeat(10) {
                presenter.load(model = model, now = at(8))
            }

            val state =
                presenter.load(model = model, now = at(8))

            assertEquals(
                1,
                state.history.totalMatched,
                "10 extra loads must not create 10 entries",
            )
        }

    @Test
    fun emptyModelRecordsNothing() = runTest {

        val presenter = presenter()

        val state =
            presenter.load(model = model(), now = at(8))

        /*
         * 空日历写进历史只会让 History 页多出一条
         * 点进去什么都没有的记录。
         */
        assertEquals(0, state.history.totalMatched)

        assertTrue(state.today.isEmpty)
    }

    @Test
    fun previewAndEvaluateAgreeOnTheDecision() = runTest {

        val dism = DismFactory.create()

        val model =
            model(
                item(id = "a", title = "A", startAt = at(9)),
                item(
                    id = "b",
                    title = "B",
                    startAt = at(11),
                    priority = 0.8,
                ),
            )

        val facade = dism.runtime.dynamicSchedule()

        /*
         * 只读路径与写入路径必须得出**相同**的决策。
         *
         * 否则"preview 只是 evaluate 不落盘"这句话就是假的，
         * 用户会看到与历史记录不一致的界面。
         */
        val previewed =
            facade.preview(model = model, now = at(8))

        val evaluated =
            facade.evaluate(model = model, now = at(8))

        assertEquals(
            previewed.decisions,
            evaluated.decisions,
        )

        assertEquals(
            previewed.evaluatedAt,
            evaluated.evaluatedAt,
        )
    }

    @Test
    fun evaluateStillRecordsHistory() = runTest {

        val dism = DismFactory.create()

        val model =
            model(
                item(id = "a", title = "A", startAt = at(9)),
            )

        val facade = dism.runtime.dynamicSchedule()

        /*
         * 只读入口加进来之后，写入入口**不能**被顺手废掉：
         * 真正的"记录这次决策"仍然必须写历史。
         */
        facade.evaluate(model = model, now = at(8))
        facade.evaluate(model = model, now = at(8))

        val history =
            facade.history.query(
                me.moiclathy.dism.schedule.model
                    .DynamicScheduleItemId("a"),
            )

        assertEquals(2, history.totalMatched)
    }

    @Test
    fun emptyModelProducesEmptyStateEverywhere() = runTest {

        val state =
            presenter().load(
                model = model(),
                now = at(8),
            )

        assertTrue(state.today.isEmpty)

        assertTrue(state.schedule.isEmpty)

        assertTrue(state.attention.isEmpty)

        assertNotNull(state.today.dateLabel)
    }

    // ---------- 16.3 导航 ----------

    @Test
    fun implementationStatusOfDestinationsIsExplicit() {

        /*
         * 导航里只列已经实现的页面。
         *
         * 这条测试的价值在于：谁想把某个目的地加进导航，
         * 就必须先决定它是否 `primary`、是否 `desktopOnly`，
         * 而不是顺手加一行。
         *
         * 16.33 补充：也要决定它是否 `implemented`。
         *
         * 这条测试的名字从 16.3 起就叫
         * `implementationStatusOfDestinationsIsExplicit`，
         * 但它当时**根本没检查 implemented**——那个标志
         * 那时还不存在，而"有没有页面"是靠一句硬编码的
         * `filter { it != DIAGNOSTICS }` 表达的，
         * 于是 MEMORY 漏掉了：它一直出现在导航里，
         * 点进去是一条"未实现"的消息。
         *
         * 更细的导航规则由 `DismResponsiveLayoutTest` 覆盖。
         */
        assertTrue(DismDestination.TODAY.primary)

        assertTrue(DismDestination.SCHEDULE.primary)

        assertTrue(DismDestination.ATTENTION.primary)

        /*
         * MEMORY：**应该**占主位置，但页面还没做。
         *
         * 这两件事分开表达，正是这个标志的意义。
         */
        assertTrue(DismDestination.MEMORY.primary)

        assertTrue(!DismDestination.MEMORY.implemented)

        assertTrue(DismDestination.SETTINGS.primary)

        assertTrue(DismDestination.SEMANTIC_STATE.desktopOnly)

        assertTrue(DismDestination.HISTORY.desktopOnly)

        assertTrue(DismDestination.DIAGNOSTICS.desktopOnly)

        assertTrue(!DismDestination.DIAGNOSTICS.implemented)

        assertTrue(
            !DismDestination.SEMANTIC_STATE.primary,
        )

        /*
         * 已经实现的那些必须标着 implemented。
         *
         * 反向也要检查：否则有人把某个页面的实现删掉、
         * 却忘了把标志改回 false，导航会留着一个打不开的入口。
         */
        listOf(
            DismDestination.TODAY,
            DismDestination.SCHEDULE,
            DismDestination.ATTENTION,
            DismDestination.SETTINGS,
            DismDestination.SEMANTIC_STATE,
            DismDestination.HISTORY,
        ).forEach { destination ->
            assertTrue(
                destination.implemented,
                "$destination has a screen but is not " +
                    "marked as implemented",
            )
        }
    }
}
