package me.moiclathy.dism.calendar

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine

import org.w3c.dom.HTMLInputElement
import org.w3c.files.File

/**
 * 16.24：浏览器上用 `<input type="file">` 选 `.ics`。
 *
 * ### 为什么必须动态创建一个 input
 *
 * 浏览器只允许"用户手势"直接触发文件选择。
 * 而 Compose 渲染成 canvas，没有可以放 `<input>` 的布局节点，
 * 所以：
 *
 * ```text
 * 建一个隐藏的 input
 * 挂到 body
 * 调 click()          ← 必须在用户点击的调用栈里
 * 等 change 事件
 * 读完把 input 移除
 * ```
 *
 * ### 对应到 js 与 wasmJs
 *
 * 依赖 `kotlinx-browser`，它同时提供 js 与 wasmJs 两种变体
 * （`kotlin-browser` wrappers 只有 js）。
 *
 * ### ⚠ 只有编译验证
 *
 * 没有在浏览器里点过。编译通过只证明类型正确，
 * 不能证明：
 *
 * ```text
 * - 隐藏 input 真的弹出选择框
 * - wasmJs 下 change 事件的类型桥接正确
 * - 读出的内容编码正确
 * ```
 */
class WebCalendarPicker : CalendarPicker {

    override suspend fun pickIcs(): String? {

        val input =
            document.createElement("input")
                as HTMLInputElement

        input.type = "file"

        /*
         * 同时给扩展名与 MIME：不同浏览器/系统认不同的那个。
         */
        input.accept = ".ics,text/calendar"

        input.style.display = "none"

        document.body?.appendChild(input)

        val file =
            try {
                suspendCancellableCoroutine<File?> { cont ->
                    input.onchange = {
                        /*
                         * 取消时 files 为空，交给下面统一处理。
                         */
                        cont.resume(input.files?.item(0))
                    }

                    cont.invokeOnCancellation {
                        input.onchange = null
                    }

                    input.click()
                }
            } finally {
                /*
                 * 无论成功、取消还是取消协程，都要把 input 摘掉，
                 * 否则每次点击都会往 body 里堆一个隐藏节点。
                 */
                document.body?.removeChild(input)
            }

        /*
         * null = 用户取消（关掉对话框）。
         */
            ?: return null

        return readText(file)
    }

    /**
     * 读文件内容。
     *
     * 用 `FileReader` 而不是 `File.text()`：
     * 后者返回 `Promise`，在 js 与 wasmJs 上的桥接方式不同；
     * 而 `FileReader` 的回调签名两边一致，包一次就够了。
     */
    private suspend fun readText(
        file: File,
    ): String? =
        suspendCoroutine { cont ->

            val reader =
                org.w3c.files.FileReader()

            reader.onload = {
                val result = reader.result

                cont.resume(
                    if (result is String) result else null,
                )
            }

            reader.onerror = {
                /*
                 * 读失败当作"没有内容"。
                 *
                 * 与取消同样的处理：不抛异常。
                 * 真正的错误信息交给上层——它拿不到内容时
                 * 会走 `ImportOutcome.Rejected` 并给出可读提示。
                 */
                cont.resume(null)
            }

            reader.readAsText(file)
        }
}
