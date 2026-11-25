package me.moiclathy.dism.calendar

import kotlin.time.Instant

import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.24：导入一次日历的完整流程。
 *
 * ```text
 * 让用户选文件          平台（CalendarPicker）
 *      ↓
 * 解析                  共享
 *      ↓
 * 缓存原文              应用（回调）
 *      ↓
 * 给用户的反馈          共享
 * ```
 *
 * ### 为什么从 App.kt 里抽出来
 *
 * 这段逻辑原本内联在 `App` 的 Composable 里，
 * 于是**测不到**——而它包含两个容易出错的判断：
 *
 * ```text
 * 1. 解析失败时不该缓存（否则下次启动必然失败）
 * 2. 缓存失败必须让用户知道（否则"导入成功"是假的）
 * ```
 *
 * 抽出来之后，两端平台适配器只剩十几行胶水，
 * 而这段真正有判断的逻辑进了单元测试。
 */
class CalendarImporter(
    private val picker: CalendarPicker,

    /**
     * 缓存用户导入的原文。
     *
     * 抛异常表示缓存失败——见 [ImportOutcome.Imported] 的说明。
     */
    private val onImported: (String) -> Unit,
) {

    /**
     * @param now 解析基准时间（时钟纪律：由调用方提供）
     */
    suspend fun pickAndImport(
        now: Instant,
        sourceId: String = DEFAULT_SOURCE_ID,
    ): ImportOutcome {

        val icsText = picker.pickIcs()

        /*
         * null = 用户取消。不是错误，不产生任何反馈。
         */
            ?: return ImportOutcome.Cancelled

        val result =
            DismScheduleImport.fromIcs(
                icsText = icsText,
                now = now,
                sourceId = sourceId,
            )

        val imported = result.model

        /*
         * 解析不出模型：把文件原样报给用户，**不缓存**。
         *
         * 缓存一份解析不了的文件没有意义，
         * 而且会让下次启动必然走到同一个失败。
         */
            ?: return ImportOutcome.Rejected(
                feedback =
                    CalendarImportFeedback(
                        importedCount = 0,
                        errors = result.errors,
                        warnings = result.warnings,
                    ),
            )

        /*
         * 解析成功 → 缓存。
         *
         * 缓存失败**不吞**：用户必须知道"这次的导入不会留到下次"。
         * 吞掉的话界面会显示"导入成功"，而下次启动它就不见了——
         * 那与 ImportedCalendarStore.save 不吞异常的理由是同一个。
         *
         * 区别在于这里不抛给调用方，而是转成一条反馈：
         * 导入本身是成功的（模型已经在内存里可用），
         * 失败的只是"下次还在不在"。
         */
        val cacheError =
            try {
                onImported(icsText)
                null
            } catch (error: Exception) {
                error.message
                    ?: error::class.simpleName
                    ?: "unknown error"
            }

        return ImportOutcome.Imported(
            model = imported,

            feedback =
                CalendarImportFeedback(
                    importedCount = imported.items.size,

                    errors =
                        result.errors +
                            listOfNotNull(
                                cacheError?.let {
                                    "Imported, but could not be " +
                                        "saved for next time: $it"
                                },
                            ),

                    warnings = result.warnings,
                ),
        )
    }

    private companion object {
        const val DEFAULT_SOURCE_ID = "user-import"
    }
}

/**
 * 一次导入的结果。
 *
 * 三态而不是"成功/失败"，因为这三件事对界面意味着完全不同的反应：
 *
 * ```text
 * Cancelled   什么都不做（用户主动取消）
 * Imported    更新模型 + 显示反馈（可能带"没存下来"的警告）
 * Rejected    不更新模型 + 显示错误
 * ```
 */
sealed interface ImportOutcome {

    data object Cancelled : ImportOutcome

    data class Imported(
        val model: DynamicScheduleModel,
        val feedback: CalendarImportFeedback,
    ) : ImportOutcome

    data class Rejected(
        val feedback: CalendarImportFeedback,
    ) : ImportOutcome
}
