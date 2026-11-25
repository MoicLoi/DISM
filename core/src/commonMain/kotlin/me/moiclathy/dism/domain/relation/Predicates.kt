package me.moiclathy.dism.domain.relation

/**
 * DISM 内建标准 predicate 词汇表。
 *
 * 这是开放词汇 —— 未来可以在此继续添加新的标准 predicate，
 * 同时不阻止外部代码使用自定义 Predicate 值。
 */
object Predicates {

    /** 实体当前位于某处。 */
    val LOCATED_AT = Predicate("located_at")

    /** 实体在某容器内部。 */
    val INSIDE = Predicate("inside")

    /** 实体被某人随身携带。 */
    val CARRIED_BY = Predicate("carried_by")

    /** 事件需要某实体。 */
    val REQUIRES = Predicate("requires")
}
