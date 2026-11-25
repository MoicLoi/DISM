package me.moiclathy.dism.persistence.driver

import app.cash.sqldelight.db.SqlDriver

/**
 * 平台数据库驱动工厂。
 *
 * Core 只认识 "给我一个 SqlDriver"，
 * 完全不知道数据库实际存在 Android 沙盒、Windows 文件夹还是 iOS 沙盒里。
 */
interface DatabaseDriverFactory {

    fun createDriver(): SqlDriver
}
