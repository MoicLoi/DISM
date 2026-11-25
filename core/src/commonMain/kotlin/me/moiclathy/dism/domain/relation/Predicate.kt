package me.moiclathy.dism.domain.relation

/**
 * 关系谓词的开放值类型。
 *
 * Predicate 不是封闭枚举 —— DISM 未来会不断出现新语义，
 * 甚至 Provider / 插件可能提供扩展 predicate。
 * 内建 Predicate 只是标准词汇表（见 [Predicates]）。
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class Predicate(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "Predicate value cannot be blank"
        }
    }

    override fun toString(): String = value
}
