package me.moiclathy.dism.persistence

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.DatabaseDriverFactory

/**
 * 统一数据库入口。
 *
 * 故意非常薄：以后 SQLDelight 2.x → 3.x、
 * SQLite → 其他 local DB，都不会影响 Semantic Core。
 */
class DismDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
) {

    fun create(): DismDatabase {
        return DismDatabase(
            driverFactory.createDriver(),
        )
    }
}
