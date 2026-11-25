package me.moiclathy.dism.presentation.home

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 16.1：把 [Instant] 变成展示用字符串。
 *
 * ### 为什么不用平台格式化 API
 *
 * 共享 UI 跑在 JVM / Android / JS / Wasm / iOS 上。
 * `java.time.format.DateTimeFormatter` 只在 JVM / Android 有，
 * JS 与 Native 上没有对应物。
 *
 * 所以这里在 common 里自己格式化。
 *
 * ### 缩写从枚举名推导，而不是写死表
 *
 * ```text
 * MONDAY -> mon -> Mon
 * SEPTEMBER -> sep -> Sep
 * ```
 *
 * 写死 `listOf("Mon", "Tue", ...)` 的版本看起来更直观，
 * 但它和 `DayOfWeek` 的顺序必须永远一致；
 * 一旦有人按 ISO 序（Monday=1）与 Java 序（Sunday=0）混淆，
 * 展示出来的日期会**静默错一天**。从 `.name` 推导没有这个风险。
 *
 * ### 已知取舍
 *
 * 语言固定为英文缩写。真正的本地化需要引入平台日期格式桥接，
 * 留给 16.x 后续。先保证三端一致且可单测。
 */
object TimeFormatting {

    /**
     * 形如 `Wed, Sep 9`。
     */
    fun dateLabel(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val dateTime = instant.toLocalDateTime(timeZone)

        return abbreviate(dateTime.dayOfWeek.name) +
            ", " +
            abbreviate(dateTime.month.name) +
            " " +
            dateTime.dayOfMonth
    }

    /**
     * 形如 `09:00`。
     *
     * 始终两位数——列表里 `9:00` 与 `11:30` 混排会看起来参差不齐。
     */
    fun timeLabel(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val dateTime = instant.toLocalDateTime(timeZone)

        return twoDigits(dateTime.hour) +
            ":" +
            twoDigits(dateTime.minute)
    }

    /**
     * 形如 `09:00 – 09:40`；两端相同时退化成单点时间（返回 null）。
     */
    fun rangeLabel(
        start: Instant,
        end: Instant?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String? {
        if (end == null || end == start) {
            return null
        }

        return timeLabel(start, timeZone) +
            " – " +
            timeLabel(end, timeZone)
    }

    /**
     * `MONDAY` → `Mon`，`SEPTEMBER` → `Sep`。
     *
     * 对短于三个字母的名字（理论上不会有）做保护，避免越界。
     */
    private fun abbreviate(
        enumName: String,
    ): String =
        enumName
            .lowercase()
            .replaceFirstChar { it.uppercase() }
            .take(3)
            .let {
                if (it.length == 3) {
                    it
                } else {
                    enumName
                }
            }

    private fun twoDigits(
        value: Int,
    ): String =
        if (value < 10) "0$value" else value.toString()
}
