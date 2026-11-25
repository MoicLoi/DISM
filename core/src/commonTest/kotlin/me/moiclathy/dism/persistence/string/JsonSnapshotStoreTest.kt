package me.moiclathy.dism.persistence.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore

/**
 * 16.34：JSON 存储后端的语义。
 *
 * ### 这组测试的意义不在于"覆盖一个实现"
 *
 * `DynamicScheduleDecisionSnapshotStore` 有三个实现：
 *
 * ```text
 * InMemory      只用于测试与 Demo
 * SQLDelight    桌面 / Android / Server
 * Json + String 浏览器（以及任何只有键值存储的地方）
 * ```
 *
 * 三者必须**语义一致**——否则同一个应用在三端行为不同，
 * 而那种差异只在特定平台、特定操作顺序下才出现。
 *
 * 所以这里断言的是**端口契约**，不是"JSON 能不能读写"：
 * 最新一条是哪一条、并列时怎么取舍、query 的排序与截断、
 * 从未记录过的条目怎么表示。
 */
class JsonSnapshotStoreTest {

    private val itemA = DynamicScheduleItemId("item-a")
    private val itemB = DynamicScheduleItemId("item-b")

    private fun store(
        storage: StringStorage = InMemoryStringStorage(),
    ): DynamicScheduleDecisionSnapshotStore =
        JsonDynamicScheduleDecisionSnapshotStore(storage)

    /**
     * 造一条属于指定条目的快照。
     *
     * ### 一个被这次测试揭示出来的不变量
     *
     * 快照里有**两个** itemId：
     *
     * ```text
     * snapshot.itemId        外层
     * snapshot.item.itemId   内层（条目快照）
     * ```
     *
     * 而扁平列表示里**只有一列** `item_id`——内层那个是由外层
     * 派生的。所以：
     *
     * ```text
     * itemId == item.itemId   是这个表示方式隐含的不变量
     * ```
     *
     * 第一版夹具只改了外层（内层留成 `item-1`），
     * 于是"存进去再读出来"不再相等——测试当场失败，
     * 而失败信息指向的是内层那个字段。
     *
     * 这不是存储的 bug：任何基于那套扁平列的实现
     * （包括 SQLDelight 那个）都有同样的性质。
     * 夹具必须构造**满足不变量**的数据，否则测的是
     * 一种现实中不存在的输入。
     */
    private fun snapshotFor(
        itemId: DynamicScheduleItemId,
        capturedAt: Instant,
    ): DynamicScheduleDecisionSnapshot {

        val base = DynamicScheduleReplayTestData.decisionSnapshot()

        return base.copy(
            itemId = itemId,

            /*
             * 内外一起改，保持上面那个不变量。
             */
            item = base.item.copy(itemId = itemId),

            capturedAt = capturedAt,
        )
    }

    // ── 基本读写 ────────────────────────────────────────

    @Test
    fun savedSnapshotRoundTrips() = runTest {

        val store = store()

        val snapshot =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            )

        store.save(snapshot)

        val loaded = store.get(snapshot.id)

        /*
         * 整份对象相等，而不是只比几个字段。
         *
         * 后端换了存储介质时最容易丢的是"不常用"的字段
         * （策略参数、依赖缺失列表）——它们只在特定场景下
         * 被读到，所以只比几个显眼字段的话，
         * 丢字段这件事会在很久以后才暴露。
         */
        assertEquals(snapshot, loaded)
    }

    @Test
    fun savingTheSameSnapshotTwiceDoesNotDuplicate() =
        runTest {

            val store = store()

            val snapshot =
                snapshotFor(
                    itemA,
                    Instant.parse("2026-09-09T09:00:00Z"),
                )

            store.save(snapshot)
            store.save(snapshot)

            val result =
                store.query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemA,
                    ),
                )

            /*
             * `save` 是 upsert，与 SQLDelight 的
             * `INSERT OR REPLACE` 一致。
             *
             * 追加的话，同一条快照被重复持久化
             * （重试、重放）会在历史里留下重复项。
             */
            assertEquals(1, result.totalMatched)
        }

    @Test
    fun unknownIdIsNull() = runTest {

        val store = store()

        /*
         * 用随机 Uuid 而不是构造一个特定值：
         * `DynamicScheduleDecisionSnapshotId` 收的是 `Uuid`，
         * 而且"一个从未存在过的 id"用随机值表达最贴切。
         */
        assertNull(
            store.get(
                me.moiclathy.dism.schedule.snapshot
                    .DynamicScheduleDecisionSnapshotId(
                        kotlin.uuid.Uuid.random(),
                    ),
            ),
        )
    }

    // ── "最近一次"的语义 ────────────────────────────────

    @Test
    fun latestIsTheNewestByTimestamp() = runTest {

        val store = store()

        val older =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            )

        val newer =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T11:00:00Z"),
            )

        /*
         * 刻意先存新的再存旧的：
         *
         * 如果实现是"取最后写入的那条"，这条测试会失败。
         * 语义必须是"按时间戳最新"，而不是"最近一次写入"——
         * 重放 / 恢复会以任意顺序写回历史。
         */
        store.save(newer)
        store.save(older)

        assertEquals(
            newer.id,
            assertNotNull(store.findLatestByItemId(itemA)).id,
        )
    }

    @Test
    fun batchLatestMatchesTheSingleItemLookup() = runTest {

        val store = store()

        listOf(
            snapshotFor(itemA, Instant.parse("2026-09-09T09:00:00Z")),
            snapshotFor(itemA, Instant.parse("2026-09-09T11:00:00Z")),
            snapshotFor(itemB, Instant.parse("2026-09-09T10:00:00Z")),
        ).forEach { store.save(it) }

        val batch =
            store.findLatestByItemIds(listOf(itemA, itemB))

        assertEquals(2, batch.size)

        /*
         * 批量与单条必须选出**同一条**。
         *
         * 这是 16.32 引入批量入口时的契约，也是
         * "用批量替换逐条"这个优化的正确性前提。
         */
        listOf(itemA, itemB).forEach { item ->
            assertEquals(
                store.findLatestByItemId(item),
                batch.first { it.itemId == item },
                "batch and single lookup disagree for $item",
            )
        }
    }

    @Test
    fun batchSkipsItemsWithoutHistory() = runTest {

        val store = store()

        store.save(
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            ),
        )

        val batch =
            store.findLatestByItemIds(listOf(itemA, itemB))

        /*
         * 从未记录过的条目**不出现**在结果里。
         *
         * 调用方（DecisionRecorder）据此判断"应当记录一次"。
         * 如果这里返回一条空壳或 null 占位，那个判断就会失效。
         */
        assertEquals(1, batch.size)

        assertEquals(itemA, batch.single().itemId)
    }

    @Test
    fun anEmptyBatchIsFine() = runTest {

        /*
         * `IN ()` 在 SQL 里是语法错误，而空集合是完全正常的
         * 一次调用（模型里一个条目都没有）。
         * JSON 实现同样要接受它。
         */
        assertTrue(store().findLatestByItemIds(emptyList()).isEmpty())
    }

    // ── 删除与清空 ──────────────────────────────────────

    @Test
    fun deleteRemovesOnlyThatSnapshot() = runTest {

        val store = store()

        val keep =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            )

        val drop =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T11:00:00Z"),
            )

        store.save(keep)
        store.save(drop)

        store.delete(drop.id)

        assertNull(store.get(drop.id))

        assertEquals(
            keep.id,
            assertNotNull(store.findLatestByItemId(itemA)).id,
        )
    }

    @Test
    fun clearRemovesEverything() = runTest {

        val store = store()

        store.save(
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            ),
        )

        store.clear()

        assertNull(store.findLatestByItemId(itemA))
    }

    // ── query ──────────────────────────────────────────

    @Test
    fun queryReturnsNewestFirstAndRespectsTheLimit() =
        runTest {

            val store = store()

            val times =
                listOf(
                    "2026-09-09T09:00:00Z",
                    "2026-09-09T11:00:00Z",
                    "2026-09-09T10:00:00Z",
                ).map(Instant::parse)

            times.forEach {
                store.save(snapshotFor(itemA, it))
            }

            val all =
                store.query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemA,
                    ),
                )

            assertEquals(3, all.totalMatched)

            assertEquals(
                times.sortedDescending(),
                all.snapshots.map { it.capturedAt },
                "history must read newest first",
            )

            val limited =
                store.query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemA,
                        limit = 2,
                    ),
                )

            /*
             * `limit` 只截断结果，**不**影响 totalMatched。
             *
             * 界面靠这个差值说"显示最近 2 条，共 3 条"。
             */
            assertEquals(2, limited.snapshots.size)

            assertEquals(3, limited.totalMatched)
        }

    @Test
    fun queryFiltersByItem() = runTest {

        val store = store()

        store.save(
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            ),
        )

        store.save(
            snapshotFor(
                itemB,
                Instant.parse("2026-09-09T09:00:00Z"),
            ),
        )

        val onlyB =
            store.query(
                DynamicScheduleDecisionSnapshotQuery(
                    itemId = itemB,
                ),
            )

        assertEquals(1, onlyB.totalMatched)

        assertEquals(itemB, onlyB.snapshots.single().itemId)
    }

    // ── 损坏数据 ────────────────────────────────────────

    @Test
    fun corruptStorageReadsAsEmptyInsteadOfThrowing() =
        runTest {

            val storage =
                InMemoryStringStorage(
                    mapOf(
                        "dism.schedule.decision.snapshots" to
                            "{ not valid json at all",
                    ),
                )

            val store = store(storage)

            /*
             * 浏览器里可能留着旧版本写的数据。
             *
             * 读取发生在**每次加载**时，让它抛会把整个应用带崩。
             * 历史丢了可恢复，应用起不来不可恢复。
             */
            assertNull(store.findLatestByItemId(itemA))

            assertTrue(
                store.query(
                    DynamicScheduleDecisionSnapshotQuery(),
                ).snapshots.isEmpty(),
            )
        }

    @Test
    fun overwritingCorruptStorageRecovers() = runTest {

        val storage =
            InMemoryStringStorage(
                mapOf(
                    "dism.schedule.decision.snapshots" to "broken",
                ),
            )

        val store = store(storage)

        val snapshot =
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            )

        /*
         * 损坏之后仍然应该能用——写入会覆盖掉坏数据。
         *
         * 如果实现是"读失败就拒绝写"，应用会永久卡在
         * "历史不可用"的状态，而用户没有任何办法恢复。
         */
        store.save(snapshot)

        assertEquals(
            snapshot,
            store.get(snapshot.id),
        )
    }

    // ── 键的隔离 ────────────────────────────────────────

    @Test
    fun differentKeysDoNotShareHistory() = runTest {

        val storage = InMemoryStringStorage()

        val first =
            JsonDynamicScheduleDecisionSnapshotStore(
                storage = storage,
                key = "history-a",
            )

        val second =
            JsonDynamicScheduleDecisionSnapshotStore(
                storage = storage,
                key = "history-b",
            )

        first.save(
            snapshotFor(
                itemA,
                Instant.parse("2026-09-09T09:00:00Z"),
            ),
        )

        /*
         * 同一个底层存储，不同的键 → 互不可见。
         *
         * 这一条的重要性在于"多账号 / 多工作区"的场景：
         * 如果实现忽略了 key、总是写死一个键，
         * 两个实例就会互相覆盖，而表面上一切正常。
         */
        assertNull(second.findLatestByItemId(itemA))

        /*
         * 用 `assertEquals` 而非 `assertNotNull` 收尾：
         * 后者返回非 Unit，会让测试方法在 JVM 上不是 `void`，
         * JUnit 会以 `InvalidTestClassError` 拒绝整个类
         * （错误信息只说"should be void"，不指向具体原因）。
         */
        assertEquals(
            itemA,
            first.findLatestByItemId(itemA)?.itemId,
        )
    }
}
