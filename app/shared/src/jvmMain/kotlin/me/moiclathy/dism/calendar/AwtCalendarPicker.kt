package me.moiclathy.dism.calendar

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * 用 AWT 的文件对话框选一个 `.ics`。
 *
 * ### 为什么用 AWT `FileDialog` 而不是 Swing `JFileChooser`
 *
 * ```text
 * FileDialog      操作系统原生对话框，观感与系统一致
 * JFileChooser    Swing 自绘，在 Windows 上明显"不像这个系统"
 * ```
 *
 * 对一个桌面应用来说，文件选择框是最容易被感知到"外行"的地方之一。
 *
 * ### 为什么不用 `withContext(Dispatchers.IO)`
 *
 * 模态对话框需要事件循环继续跑。把它丢到 IO 线程上、
 * 让 EDT 去做别的事，反而容易出现对话框不响应。
 *
 * Compose Desktop 的 `rememberCoroutineScope` 跑在 Swing EDT 上，
 * 而模态 `FileDialog` 自己会泵事件，所以在 EDT 上同步弹出是正确做法。
 *
 * ICS 文件很小（几百 KB 以内），同步读不会造成可感知的卡顿。
 */
class AwtCalendarPicker(
    private val owner: Frame? = null,

    private val title: String = "Import a calendar (.ics)",
) : CalendarPicker {

    override suspend fun pickIcs(): String? {

        val dialog =
            FileDialog(owner, title, FileDialog.LOAD)

        /*
         * 过滤器只是便利，不是保证：
         * 各平台对它的支持程度不同，Windows 上有效。
         * 所以下面仍然要处理"用户选了一个非 .ics 文件"的情况——
         * 解析器会给出可读的错误，而不是这里崩掉。
         */
        dialog.setFilenameFilter { _, name ->
            name.endsWith(".ics", ignoreCase = true)
        }

        dialog.isVisible = true

        val directory = dialog.directory

        val fileName = dialog.file

        if (directory == null || fileName == null) {
            /*
             * 用户取消。这是正常操作，不是错误。
             */
            return null
        }

        return File(directory, fileName).readText()
    }
}
