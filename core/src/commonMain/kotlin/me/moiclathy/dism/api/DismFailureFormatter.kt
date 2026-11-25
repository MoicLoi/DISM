package me.moiclathy.dism.api

/**
 * Converts a structured failure into a stable diagnostic string.
 *
 * This is intentionally simple. Human-facing localization belongs
 * to the application layer.
 *
 * ### 关于"稳定"
 *
 * `details` 是按 key **排序**输出的，而不是按 Map 的遍历顺序。
 *
 * 规格原文直接用了 `details.entries`，对 `mapOf(...)` 构造的
 * Map 确实会得到插入顺序——但那是实现细节，不是保证。
 * 一旦某个调用点换成 `mutableMapOf` 或 `buildMap`，
 * 同一份失败就会产生两种日志字符串，日志聚合直接失效。
 */
@DismPublicApi
object DismFailureFormatter {

    fun format(
        failure: DismFailure,
    ): String =
        buildString {

            append(failure.code.name)

            if (failure.operation != null) {
                append(" [")
                append(failure.operation)
                append(']')
            }

            append(": ")
            append(failure.message)

            if (failure.details.isNotEmpty()) {
                append(" (")

                failure.details.entries
                    .sortedBy { it.key }
                    .joinTo(this, separator = ", ") { (key, value) ->
                        "$key=$value"
                    }

                append(')')
            }
        }
}
