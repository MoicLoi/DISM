package me.moiclathy.dism.persistence.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.persistence.db.DismDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightEntityRepositoryTest {

    @Test
    fun entity_shouldSurviveDatabaseRoundTrip() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(
            driver
        )

        val database =
            DismDatabase(
                driver
            )

        val repository =
            SqlDelightEntityRepository(
                database
            )

        val original =
            Entity(
                type = EntityType.ITEM,
                name = "充电宝",
            )

        repository.save(
            original
        )

        val loaded =
            repository.find(
                original.id
            )

        assertEquals(
            original,
            loaded,
        )

        driver.close()
    }

    @Test
    fun saveAll_shouldPersistEveryEntity() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(
            driver
        )

        val repository =
            SqlDelightEntityRepository(
                DismDatabase(driver)
            )

        val powerBank =
            Entity(
                type = EntityType.ITEM,
                name = "充电宝",
            )

        val dorm =
            Entity(
                type = EntityType.LOCATION,
                name = "宿舍",
            )

        repository.saveAll(
            listOf(
                powerBank,
                dorm,
            )
        )

        assertEquals(
            setOf(powerBank.id, dorm.id),
            repository.all()
                .map { it.id }
                .toSet(),
        )

        driver.close()
    }

    @Test
    fun missingEntity_shouldReturnNull() {
        val driver =
            JdbcSqliteDriver(
                JdbcSqliteDriver.IN_MEMORY
            )

        DismDatabase.Schema.create(
            driver
        )

        val repository =
            SqlDelightEntityRepository(
                DismDatabase(driver)
            )

        assertNull(
            repository.find(
                kotlin.uuid.Uuid.random()
            )
        )

        driver.close()
    }
}
