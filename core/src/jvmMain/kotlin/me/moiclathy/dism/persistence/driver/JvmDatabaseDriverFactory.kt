package me.moiclathy.dism.persistence.driver

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.persistence.db.DismDatabase
import java.io.File

class JvmDatabaseDriverFactory(
    private val databaseFile: File,
) : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        databaseFile.parentFile?.mkdirs()

        val databaseAlreadyExists =
            databaseFile.exists()

        val driver =
            JdbcSqliteDriver(
                url = sqliteUrl(databaseFile),
            )

        if (!databaseAlreadyExists) {
            DismDatabase.Schema.create(driver)
        }

        return driver
    }

    private fun sqliteUrl(
        file: File,
    ): String {
        // Windows 路径含反斜杠，xerial sqlite-jdbc 对正斜杠更稳
        return "jdbc:sqlite:${file.absolutePath.replace('\\', '/')}"
    }
}
