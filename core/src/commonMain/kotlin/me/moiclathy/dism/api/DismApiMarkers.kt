package me.moiclathy.dism.api

import kotlin.RequiresOptIn
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.EXPRESSION
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPEALIAS

/**
 * Marks a type or member that is part of the supported DISM Public API.
 *
 * This is a source-level boundary marker.
 *
 * 注意：
 *
 * @DismPublicApi 并不意味着该类型依赖的所有类型
 * 都自动成为 Public API。
 *
 * Public API 的传递闭包仍然由 15.x 架构审计确定。
 *
 * ### 关于 `EXPRESSION`
 *
 * 规格原文的 @Target 里带了 `EXPRESSION`，但 Kotlin 明确禁止：
 *
 * - 带 BINARY/`RUNTIME` retention 的注解不能标注表达式
 * - `@RequiresOptIn` 标记也不能用于表达式
 *
 * 保留它是编译错误，所以这里去掉。
 */
@Retention(BINARY)
@Target(
    ANNOTATION_CLASS,
    CLASS,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY,
    TYPEALIAS,
)
annotation class DismPublicApi

/**
 * Marks an implementation-facing DISM API that is intentionally
 * outside the supported product API surface.
 *
 * 15.x 当前使用 WARNING 级别，而不是直接使用 internal，
 * 原因是当前工程仍存在大量内部测试、迁移代码和旧装配路径。
 *
 * ```text
 * 15.2  标记
 *  ↓
 * 15.3  收口
 *  ↓
 * 15.4  真正 internal 化
 * ```
 *
 * 而不是一步把整个工程砍断。
 */
@RequiresOptIn(
    message =
        "This is an internal DISM API and is not covered by " +
            "Public API compatibility guarantees.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(BINARY)
@Target(
    ANNOTATION_CLASS,
    CLASS,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY,
    TYPEALIAS,
)
annotation class DismInternalApi
