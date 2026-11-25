package me.moiclathy.dism.schedule.api

import kotlin.time.Instant

import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.ics.IcsScheduleParser
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseInput
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId
import me.moiclathy.dism.schedule.integration.ExternalScheduleToDynamicScheduleMapper
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.21：把外部日历（ICS）变成 DISM 能评估的模型。
 *
 * ```kotlin
 * val result = DismScheduleImport.fromIcs(icsText, now)
 *
 * if (result.success) {
 *     schedule.preview(result.model!!, now)
 * }
 * result.warnings.forEach { println(it) }
 * ```
 *
 * ### 为什么需要它
 *
 * 在此之前，`DynamicScheduleModel` 只能由调用方**手工构造**
 * （各平台入口里的 `DemoSchedule` 就是这么来的）。
 *
 * 而 11.x 里其实早就有完整链路：
 *
 * ```text
 * ICS 文本
 *    ↓ IcsScheduleParser
 * ExternalSchedule
 *    ↓ ExternalScheduleToDynamicScheduleMapper
 * DynamicScheduleModel
 * ```
 *
 * 只是它整个住在"约定内部"的包里，没有被 Product API 收口。
 * 于是应用要么手工编数据，要么去碰 200 多个 `ExternalScheduleSync*`
 * 类型——都不对。
 *
 * ### 暴露边界：只收口这一条链
 *
 * 11.x 的绝大部分是同步机制（Journal / 幂等 / 恢复 / 对账 / 重放 / 事务）。
 * 那些**不**在这里暴露，它们是 Runtime 的内部事务，
 * 不是产品概念。一旦进入冻结的公共 ABI，就变成 1.x 的承诺。
 *
 * ### 错误处理：用户数据错误用结果表示，不用异常
 *
 * 这里的失败几乎都是**用户数据问题**（文件不是日历、
 * 某条 VEVENT 格式不对），不是调用方写错了代码。
 *
 * ```text
 * 调用方错误     →  抛异常（例如 sourceId 为空白，domain 里已 require）
 * 用户数据错误   →  返回结果（这个文件解析不了 / 有一条坏了）
 * ```
 *
 * 用异常表达后者会带来两个具体问题：
 *
 * 1. **丢掉 warnings**。一个含 3 条好 VEVENT、1 条坏 VEVENT 的文件
 *    应该成功导入 3 条**并告诉用户第 4 条坏了**；
 *    抛异常会把这些信息一起扔掉。
 * 2. **迫使调用方为常规情况写 try/catch**。
 *    "用户选错文件"是常规情况，不是异常情况。
 *
 * 这与 `DismBootstrap.createPersistence` 抛异常并不矛盾：
 * 那是「配置要求了平台做不到的事」，属于调用方错误。
 */
@DismPublicApi
object DismScheduleImport {

    /**
     * 解析 ICS 文本。
     *
     * @param icsText ICS 文件内容
     * @param now 映射时间戳（由调用方提供，见 13.15 时钟纪律）
     * @param sourceId 来源标识，用于区分多个日历
     */
    fun fromIcs(
        icsText: String,
        now: Instant,
        sourceId: String = DEFAULT_SOURCE_ID,
    ): DismScheduleImportResult {

        /*
         * sourceId 为空白是**调用方错误**，由 domain 的 require 抛出。
         * 这里刻意不 catch：它不该被降级成"导入失败"，
         * 那会让一个编程错误看起来像用户数据问题。
         */
        val parseResult =
            IcsScheduleParser().parse(
                ExternalScheduleParseInput(
                    sourceId =
                        ExternalScheduleSourceId(sourceId),
                    origin = ExternalScheduleOrigin.ICS,
                    rawContent = icsText,
                ),
            )

        val schedule = parseResult.schedule

        val errors =
            parseResult.errors.map { it.message }

        if (schedule == null) {
            return DismScheduleImportResult(
                model = null,
                errors =
                    errors.ifEmpty {
                        listOf(
                            "The file did not contain any " +
                                "calendar events.",
                        )
                    },
                warnings = parseResult.warnings,
            )
        }

        return DismScheduleImportResult(
            model =
                ExternalScheduleToDynamicScheduleMapper()
                    .map(
                        schedule = schedule,
                        mappedAt = now,
                    ),
            errors = errors,
            warnings = parseResult.warnings,
        )
    }

    private const val DEFAULT_SOURCE_ID = "ics-import"
}

/**
 * 导入结果。
 *
 * 模型与诊断信息**同时**返回：部分成功是常态
 * （3 条好 + 1 条坏），调用方需要两者才能给出正确的反馈。
 */
@DismPublicApi
data class DismScheduleImportResult(
    /**
     * 解析成功时为模型；完全没有可用条目时为 null。
     */
    val model: DynamicScheduleModel?,

    /**
     * 人类可读的错误。为空表示没有任何条目出问题。
     */
    val errors: List<String> = emptyList(),

    /**
     * 没有让条目失败、但值得提醒的问题。
     */
    val warnings: List<String> = emptyList(),
) {

    val success: Boolean
        get() = model != null

    /**
     * "部分成功"：有模型，但也有条目被跳过。
     *
     * 单独暴露是因为这是最需要 UI 明确告知用户的状态：
     * 直接说"导入成功"会让用户以为日历是完整的。
     */
    val hasPartialFailures: Boolean
        get() = model != null && errors.isNotEmpty()
}
