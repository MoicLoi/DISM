package me.moiclathy.dism.calendar

/**
 * 让用户交出 ICS 文本。
 *
 * ### 为什么是 `fun interface` 而不是平台 expect/actual
 *
 * 三个平台的取文件方式完全不同：
 *
 * ```text
 * 桌面      AWT FileDialog
 * Android   SAF（ActivityResultContracts.OpenDocument）
 * Web       <input type="file">
 * ```
 *
 * 而且 Android 与 Web 的取文件是**异步 + 回调**式的，
 * 没法用一个同步的 expect 函数表达。
 *
 * 所以共享层只声明"我需要一段 ICS 文本"，
 * 由平台入口注入具体实现。
 *
 * ### 返回 null 表示用户取消
 *
 * 取消是正常操作，不是错误：不该抛异常，也不该显示错误提示。
 */
fun interface CalendarPicker {

    /**
     * @return ICS 文本；用户取消或未选择时返回 null
     */
    suspend fun pickIcs(): String?
}

/**
 * 导入结果给用户的反馈。
 *
 * 与 `DismScheduleImportResult` 的区别：
 * 那个是**解析层**的结论，这个是**界面层**要显示的东西。
 */
data class CalendarImportFeedback(
    val importedCount: Int,

    val errors: List<String> = emptyList(),

    val warnings: List<String> = emptyList(),
) {

    /**
     * 部分成功：有条目导入，但也有条目被跳过。
     *
     * 这是最需要明确告知用户的状态：
     * 只说"导入成功"会让用户以为日历是完整的。
     */
    val isPartial: Boolean
        get() = importedCount > 0 && errors.isNotEmpty()
}
