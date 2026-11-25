package me.moiclathy.dism.persistence.driver

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.moiclathy.dism.persistence.db.DismDatabase

/**
 * 16.14：Android 的 SQLite driver。
 *
 * ### 库名现在是参数，不再硬编码
 *
 * 之前这里写死 `"dism.db"`，于是：
 *
 * ```kotlin
 * DismConfiguration(
 *     storage = DismStorageConfiguration(databaseName = "work"),
 * )
 * ```
 *
 * 在 Android 上会**静默**落到 `dism.db`——用户以为数据分开存，
 * 其实全在一个库里。
 *
 * 桌面端没这个问题，因为 `JvmDatabaseDriverFactory` 拿的是完整
 * 文件路径，名字天然由路径决定。两边语义不一致正是 bug 的来源。
 *
 * 默认值保留 `"dism.db"`，所以既有调用方不受影响。
 */
class AndroidDatabaseDriverFactory(
    context: Context,
    private val databaseName: String = DEFAULT_DATABASE_NAME,
) : DatabaseDriverFactory {

    private val appContext =
        context.applicationContext

    init {
        require(databaseName.isNotBlank()) {
            "databaseName must not be blank"
        }
    }

    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = DismDatabase.Schema,
            context = appContext,
            name = databaseName,
        )
    }

    private companion object {
        const val DEFAULT_DATABASE_NAME =
            "dism.db"
    }
}
