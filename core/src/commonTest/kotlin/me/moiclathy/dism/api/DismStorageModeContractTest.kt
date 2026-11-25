package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode

/**
 * 15.5
 *
 * `DismStorageMode.LOCAL` 在没有平台绑定时必须**明确失败**。
 *
 * ### 为什么这条比"能用"更重要
 *
 * 规格这一版把 commonMain 的 LOCAL 实现写成了
 * "返回内存 Persistence，以后由平台绑定"。
 *
 * 那会产生一个不会报错的错误：
 *
 * ```text
 * 用户配置 LOCAL
 *     ↓
 * 系统默默给内存存储
 *     ↓
 * 重启后数据全没了
 * ```
 *
 * 一个什么都不做的开关比没有这个开关更糟——
 * 用户会以为自己已经开启了持久化。
 *
 * 所以这里锁住的是**报错**，而不是"能跑"。
 */
class DismStorageModeContractTest {

    @Test
    fun inMemoryModeWorksWithoutPlatformBinding() {

        val dism =
            DismFactory.create(
                DismConfiguration(
                    storage =
                        DismStorageConfiguration(
                            mode = DismStorageMode.IN_MEMORY,
                        ),
                ),
            )

        assertTrue(dism.capabilities.persistence)

        assertTrue(
            dism.runtime.snapshot().entities.isEmpty(),
        )
    }

    @Test
    fun localModeFailsLoudlyWithoutPlatformProvider() {

        val failure =
            assertFailsWith<DismException> {
                DismFactory.create(
                    DismConfiguration(
                        storage =
                            DismStorageConfiguration(
                                mode = DismStorageMode.LOCAL,
                                databaseName = "dism",
                            ),
                    ),
                )
            }

        /*
         * 15.6：失败必须以结构化形式暴露，
         * App 才能按 code 分支，而不是解析文本。
         */
        assertEquals(
            DismFailureCode.UNSUPPORTED_CAPABILITY,
            failure.failure.code,
        )

        assertEquals(
            "createPersistence",
            failure.failure.operation,
        )

        assertEquals(
            "LOCAL",
            failure.failure.details["storageMode"],
        )

        assertEquals(
            "dism",
            failure.failure.details["databaseName"],
        )

        /*
         * 错误信息必须告诉集成者**下一步该做什么**，
         * 而不只是"失败了"。
         *
         * 注意这里**不**要求 message 回显 databaseName：
         * 那属于结构化字段（上面已经断言 details["databaseName"]），
         * 而 message 是给人看的、将来可能要本地化。
         */
        val message =
            failure.failure.message

        assertTrue(
            message.contains("DismPersistenceProvider"),
            "message must name the missing seam; got=$message",
        )

        assertTrue(
            message.contains("DismBootstrapper"),
            "message must point at the binding entry point; " +
                "got=$message",
        )

        assertTrue(
            message.contains("IN_MEMORY"),
            "message must offer the working alternative; " +
                "got=$message",
        )
    }

    @Test
    fun defaultStorageModeIsInMemory() {

        /*
         * 默认必须是内存：这样 `DismFactory.create()`
         * 在任何平台都可用，也不会假装已经落盘。
         */
        assertTrue(
            DismConfiguration().storage.mode ==
                DismStorageMode.IN_MEMORY,
        )
    }
}
