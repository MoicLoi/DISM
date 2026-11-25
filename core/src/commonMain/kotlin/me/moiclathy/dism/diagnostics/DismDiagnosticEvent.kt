package me.moiclathy.dism.diagnostics

import kotlin.time.Clock
import kotlin.time.Instant
import me.moiclathy.dism.api.DismPublicApi

/**
 * A structured diagnostic event emitted by DISM.
 *
 * Diagnostic events are intentionally independent from any concrete
 * logging framework.
 *
 * ### 为什么 `timestamp` 允许有默认值
 *
 * 13.15 的时钟纪律是"时钟由调用方传入，引擎不自己读时间"。
 * 那条纪律针对的是**决策引擎**——读取时钟会让同一个输入
 * 产生不同结果，从而破坏重放与验证。
 *
 * 诊断事件不在那条线上：
 *
 * ```text
 * 它不参与决策，也不参与重放比对
 * ```
 *
 * 所以这里保留一个便利默认值。但需要确定性输出时
 * （例如快照测试），调用方仍然应该显式传 `timestamp`——
 * [DismDiagnostics] 就是这么做的：它在自己那一层读一次时钟，
 * 并把结果显式写进事件。
 */
@DismPublicApi
data class DismDiagnosticEvent(
    val level: DismDiagnosticLevel,
    val message: String,
    val timestamp: Instant = Clock.System.now(),
    val component: String? = null,
    val operation: String? = null,
    val code: String? = null,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(message.isNotBlank()) {
            "diagnostic message must not be blank"
        }
    }
}
