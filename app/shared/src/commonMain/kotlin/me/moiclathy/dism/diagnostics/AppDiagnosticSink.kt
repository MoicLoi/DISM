package me.moiclathy.dism.diagnostics

/**
 * 16.23：应用侧的诊断出口。
 *
 * ```text
 * 打印到控制台   →  开发时能立刻看到
 * 保留在内存里   →  测试能断言"确实发了"
 * ```
 *
 * ### 为什么两者都要
 *
 * 只用打印：测试里断言不了（要抓 stdout，脆）。
 * 只用内存：开发时看不到，等于没有。
 *
 * ### 为什么不需要 Core 改动
 *
 * `DismDiagnosticSink` 是公共的 `fun interface`。
 * 应用**自己创建**这个 sink 并传给
 * `DismConfiguration.diagnostics.sink`，于是天然持有引用，
 * 可以直接往它写。
 *
 * ```text
 * Dism 不暴露 diagnostics 访问器（只有 apiVersion / capabilities / runtime）
 * 但应用不需要它暴露——sink 本来就是我建的
 * ```
 */
class AppDiagnosticSink(
    private val capacity: Int = 256,

    /**
     * 额外转发目标。
     *
     * 例如把同一条事件同时送去 telemetry 或文件。
     */
    private val delegate: DismDiagnosticSink? = null,

    /**
     * 是否打印。
     *
     * 测试里关掉，避免污染测试输出。
     */
    private val print: Boolean = true,
) : DismDiagnosticSink {

    private val memory = InMemoryDismDiagnosticSink(capacity)

    override fun emit(event: DismDiagnosticEvent) {

        memory.emit(event)

        delegate?.emit(event)

        if (print) {
            println(format(event))
        }
    }

    /**
     * 已记录的事件（最旧在前）。
     */
    fun events(): List<DismDiagnosticEvent> = memory.snapshot()

    fun clear() {
        memory.clear()
    }

    /**
     * 形如：
     *
     * ```text
     * [DISM WARN] presentation/recordDecision RECORDING_FAILED:
     *   Failed to record the decision: disk full
     * ```
     *
     * 单行、字段固定，方便直接 grep。
     */
    private fun format(event: DismDiagnosticEvent): String =
        buildString {
            append("[DISM ")
            append(event.level)
            append("] ")

            val where =
                listOfNotNull(
                    event.component,
                    event.operation,
                ).joinToString("/")

            if (where.isNotEmpty()) {
                append(where)
                append(' ')
            }

            event.code?.let {
                append(it)
                append(": ")
            }

            append(event.message)
        }
}

/**
 * 16.23：记录决策失败。
 *
 * ### 这条事件存在的意义
 *
 * 16.22 的 `DecisionRecorder` 在失败时会被有意吞掉
 * （宁可 History 少一条，也不能让 Today 变成错误页）。
 *
 * 但"吞掉"不等于"不告诉任何人"：
 *
 * ```text
 * 吞掉且不报告  →  历史悄悄不长了，查不出来
 * 吞掉并报告    →  页面照常可用，但诊断里有据可查
 * ```
 *
 * 这正是"有意降级"与"静默失败"的区别。
 */
internal fun decisionRecordFailedEvent(
    error: Exception,
): DismDiagnosticEvent =
    DismDiagnosticEvent(
        level = DismDiagnosticLevel.WARN,

        message =
            "Failed to record the decision: " +
                (error.message ?: error::class.simpleName),

        component = "presentation",

        operation = "recordDecision",

        code = DECISION_RECORD_FAILED_CODE,

        /*
         * 带上异常类型：不同的失败原因（磁盘满 / 权限 /
         * 数据库被锁）在排查时是完全不同的问题，
         * 而 message 里往往看不出类型。
         */
        attributes =
            mapOf(
                "errorType" to error::class.simpleName.orEmpty(),
            ),
    )

/**
 * 稳定的代码，测试与 grep 都依赖它。
 *
 * 不要用 message 做断言——那句话是给人看的，随时会改。
 */
internal const val DECISION_RECORD_FAILED_CODE =
    "DECISION_RECORD_FAILED"
