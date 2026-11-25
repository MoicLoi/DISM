package me.moiclathy.dism.domain.entity

/**
 * Entity 的基础分类。
 *
 * 这里只定义稳定、粗粒度的实体类型。
 * 不要把具体业务语义全部塞进 EntityType。
 */
enum class EntityType {
    PERSON,
    ITEM,
    LOCATION,
    EVENT,
    COURSE,
    ORGANIZATION,
    PLACE,
    OTHER,
}