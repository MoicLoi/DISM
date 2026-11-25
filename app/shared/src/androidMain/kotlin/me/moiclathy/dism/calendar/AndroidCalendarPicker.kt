package me.moiclathy.dism.calendar

import android.content.ContentResolver
import android.net.Uri

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 16.24：Android 上用 SAF（Storage Access Framework）选 `.ics`。
 *
 * ### 为什么需要把回调桥成 suspend
 *
 * SAF 是回调式的：
 *
 * ```kotlin
 * registerForActivityResult(OpenDocument()) { uri -> ... }
 * ```
 *
 * 而 [CalendarPicker] 是挂起的。所以用一个
 * [suspendCancellableCoroutine] 把回调接回协程。
 *
 * Activity 负责两件事：
 *
 * ```kotlin
 * picker.requestOpenDocument = { types -> launcher.launch(types) }
 * // launcher 的回调里：picker.onDocumentPicked(uri)
 * ```
 *
 * ### ⚠ 只有编译验证
 *
 * 这个文件**没有在真机或模拟器上跑过**——本机没有 Android 环境。
 * 编译通过只能证明类型正确，不能证明：
 *
 * ```text
 * - SAF 对话框真的弹出
 * - 某些设备真的能选到 .ics（取决于 provider 声明的 MIME）
 * - 读出的内容编码正确
 * ```
 *
 * ### MIME 类型为什么给两个
 *
 * ```text
 * text/calendar    标准
 * text/*           有些 provider 把 .ics 标成通用文本
 * ```
 *
 * 只给 `text/calendar` 时，部分设备上文件会灰掉选不中——
 * 那是"功能看起来坏了"的典型表现。
 *
 * 给 `*/*` 可以最大化可选性，但会让用户能选中照片之类的文件。
 * 这里选择折中：给两个文本类型，选错文件由解析器拒绝
 * （`ImportOutcome.Rejected` 已经会给出可读提示）。
 */
class AndroidCalendarPicker(
    private val contentResolver: ContentResolver,
) : CalendarPicker {

    private var continuation: Continuation<Uri?>? = null

    /**
     * 由 Activity 绑定：真正启动 SAF 对话框。
     */
    var requestOpenDocument: (Array<String>) -> Unit = {}

    /**
     * 由 Activity 的 launcher 回调。
     *
     * 传 null 表示用户取消。
     */
    fun onDocumentPicked(uri: Uri?) {
        val pending = continuation

        continuation = null

        pending?.resume(uri)
    }

    override suspend fun pickIcs(): String? {

        val uri =
            suspendCancellableCoroutine<Uri?> { cont ->
                continuation = cont

                /*
                 * 协程被取消时把接续清掉，
                 * 否则会留下一个永远没人 resume 的引用。
                 */
                cont.invokeOnCancellation {
                    continuation = null
                }

                requestOpenDocument(MIME_TYPES)
            }

        /*
         * null = 用户取消。
         */
            ?: return null

        return contentResolver
            .openInputStream(uri)
            ?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
    }

    private companion object {
        val MIME_TYPES =
            arrayOf("text/calendar", "text/*")
    }
}
