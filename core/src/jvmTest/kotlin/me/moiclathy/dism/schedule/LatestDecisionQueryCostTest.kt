package me.moiclathy.dism.schedule

import java.io.File
import java.nio.file.Files

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.runBlocking

import me.moiclathy.dism.platform.JvmDismFactory
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.32：N+1 查询的**量化诊断**。
 *
 * ### 这不是断言测试，是诊断
 *
 * 它打印数字，用来回答一个决策问题：
 *
 * ```text
 * DecisionRecorder 每个条目一次 latestDecision(itemId)
 *   → 值得为它加一个批量 API（跨 6 层的公共 ABI 变更）吗？
 * ```
 *
 * 要回答这个问题需要知道它**比什么贵**：
 *
 * ```text
 * 对照   一次 preview()      —— 完整决策链，本来就每次加载都要跑
 * 被测   N 次 latestDecision —— 带索引的单行查询
 * ```
 *
 * 所以这里测的是**比值**，不是绝对值。比值比绝对时间稳得多：
 * 机器快慢会同时影响两边。
 *
 * ### 为什么用真实的 SQLDelight 而不是内存
 *
 * 内存实现下"查询"是 map 查找，会把 N+1 测得几乎免费——
 * 而真实代价恰恰来自 SQL 往返。用错实现会得到错的结论。
 */
class LatestDecisionQueryCostTest {

    private val dir: File =
        Files.createTempDirectory("dism-nq-cost")
            .toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private fun model(
        itemCount: Int,
    ): DynamicScheduleModel {

        val scheduleId = DynamicScheduleId("cost")

        return DynamicScheduleModel(
            id = scheduleId,
            revision = 1L,
            items =
                (1..itemCount).map { index ->
                    DynamicScheduleItem(
                        id =
                            DynamicScheduleItemId("item-$index"),
                        scheduleId = scheduleId,
                        title = "Item $index",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.USER_INPUT,
                        temporal =
                            DynamicScheduleTemporal.At(
                                Instant.parse(
                                    "2026-09-09T09:00:00Z",
                                ),
                            ),
                        state = DynamicScheduleState.UPCOMING,
                    )
                },
        )
    }

    /**
     * 计时辅助。
     *
     * 是 `suspend` 的：被测的两边都是挂起函数，
     * 用普通函数包不住它们。
     */
    private suspend fun millis(
        block: suspend () -> Unit,
    ): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }

    @Test
    fun compareTheCostOfOneEvaluationAgainstNLatestLookups() =
        runBlocking {

            val itemCount = 200

            val dism =
                JvmDismFactory.create(
                    databaseFile = File(dir, "cost.db"),
                )

            val schedule = dism.runtime.dynamicSchedule()

            val scheduleModel = model(itemCount)

            /*
             * 先评估一次，把 N 条快照写进去——
             * 否则下面的 latestDecision 查的是空表。
             */
            schedule.evaluate(model = scheduleModel, now = now)

            /*
             * 预热：JIT 与页面缓存。
             */
            repeat(3) {
                schedule.preview(
                    model = scheduleModel,
                    now = now,
                )

                scheduleModel.items.forEach {
                    schedule.latestDecision(it.id)
                }
            }

            val previewMillis =
                millis {
                    repeat(5) {
                        schedule.preview(
                            model = scheduleModel,
                            now = now,
                        )
                    }
                } / 5

            val lookupsMillis =
                millis {
                    repeat(5) {
                        scheduleModel.items.forEach {
                            schedule.latestDecision(it.id)
                        }
                    }
                } / 5

            /*
             * 16.32：批量版本。这是修复后的路径。
             */
            val batchMillis =
                millis {
                    repeat(5) {
                        schedule.latestDecisions(
                            scheduleModel.items.map { it.id },
                        )
                    }
                } / 5

            println(
                "[N+1 COST] items=$itemCount " +
                    "preview=${previewMillis}ms " +
                    "n-lookups=${lookupsMillis}ms " +
                    "batch=${batchMillis}ms",
            )

            /*
             * 断言两边**结果一致**——这是修复的正确性前提。
             *
             * 性能改进只有在语义不变时才算改进；
             * 如果批量版本选错了行，快也没有意义。
             */
            val batch = schedule.latestDecisions(
                scheduleModel.items.map { it.id },
            )

            assertEquals(
                itemCount,
                batch.size,
                "every item has a recorded decision here",
            )

            scheduleModel.items.forEach { item ->
                val single = schedule.latestDecision(item.id)

                /*
                 * 整份 view 相等，而不是只比某个字段。
                 *
                 * 契约是"每个条目对应的值与 latestDecision
                 * 返回的完全一致"——只比一个字段的话，
                 * 将来给 view 加字段时这条断言会静默失去覆盖。
                 */
                assertEquals(
                    single,
                    batch[item.id],
                    "the batch entry for ${item.id.value} must " +
                        "equal the single lookup",
                )
            }

            /*
             * 不断言时间。数字留在日志里供人工判断。
             */
            assertTrue(previewMillis >= 0)
            assertTrue(lookupsMillis >= 0)
            assertTrue(batchMillis >= 0)
        }
}
