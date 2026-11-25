package me.moiclathy.dism

import kotlin.time.Clock
import kotlin.time.Instant

import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.21：示例日历，**以 ICS 文本形式**提供。
 *
 * ### 它为什么从"手工编模型"变成了 ICS
 *
 * 之前这里直接构造 `DynamicScheduleModel`：
 *
 * ```kotlin
 * DynamicScheduleItem(id = ..., title = "实验动物学", ...)
 * ```
 *
 * 那意味着每个平台入口都在**绕过真实的导入链路**，
 * 于是 11.x 的解析/规范化/映射从来没有被应用真正跑过——
 * 只有单元测试跑过。
 *
 * 现在它给出一段 ICS，入口调用真正的公共 API：
 *
 * ```text
 * ICS 文本
 *    ↓ DismScheduleImport.fromIcs()      ← 真实链路
 * DynamicScheduleModel
 * ```
 *
 * ### 它仍然是示例数据，这一点没有变
 *
 * 这不是"假装已经实现了日历同步"。真实的日历来源是用户导入
 * 或设备日历，那需要一个文件选择 / 权限流程，属于后续工作。
 *
 * 它现在是**一段可以替换成用户真实 ICS 的文本**，
 * 而不再是一个绕过导入链路的手工对象。
 *
 * ### 时间锚定在"今天"
 *
 * ICS 需要具体日期，所以这里用"今天 00:00 UTC"生成，
 * 否则打开界面就是一堆已过期的条目。
 */
object SampleCalendar {

    /**
     * 三段式示例：一节实验课、一顿午餐、一段深度工作。
     */
    fun ics(
        now: Instant = Clock.System.now(),
    ): String {

        val day =
            Instant.fromEpochSeconds(
                (now.epochSeconds / SECONDS_PER_DAY) *
                    SECONDS_PER_DAY,
            )

        val date =
            day.toString()
                .substringBefore("T")
                .replace("-", "")

        return """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//DISM//Sample Calendar//EN
            BEGIN:VEVENT
            UID:sample-lab
            SUMMARY:实验动物学
            LOCATION:实验楼201
            DTSTART:${date}T090000Z
            DTEND:${date}T094000Z
            END:VEVENT
            BEGIN:VEVENT
            UID:sample-lunch
            SUMMARY:午餐
            DTSTART:${date}T113000Z
            DTEND:${date}T120000Z
            END:VEVENT
            BEGIN:VEVENT
            UID:sample-deepwork
            SUMMARY:项目开发
            LOCATION:DISM
            DTSTART:${date}T140000Z
            DTEND:${date}T170000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
    }

    /**
     * 走真实导入链路得到模型。
     *
     * 解析失败时返回一个空模型而不是抛异常：
     * 示例数据本身坏掉不该让应用起不来。
     */
    fun model(
        now: Instant = Clock.System.now(),
    ): DynamicScheduleModel =
        DismScheduleImport
            .fromIcs(
                icsText = ics(now),
                now = now,
                sourceId = "sample",
            )
            .model
            ?: emptyModel()

    private fun emptyModel(): DynamicScheduleModel =
        DismScheduleImport
            .fromIcs(
                icsText =
                    "BEGIN:VCALENDAR\n" +
                        "VERSION:2.0\n" +
                        "PRODID:-//DISM//Empty//EN\n" +
                        "END:VCALENDAR",
                now = Clock.System.now(),
                sourceId = "sample-empty",
            )
            .model
            ?: error(
                "an empty calendar must still produce a model",
            )

    private const val SECONDS_PER_DAY = 86_400L
}
