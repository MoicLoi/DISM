package me.moiclathy.dism.persistence.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.moiclathy.dism.persistence.db.DismDatabase

class IosDatabaseDriverFactory :
    DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = DismDatabase.Schema,
            name = "dism.db",
        )
    }
}
