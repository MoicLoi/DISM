package me.moiclathy.dism.provider

import me.moiclathy.dism.api.DismPublicApi

/**
 * Base contract for an external DISM provider.
 *
 * A provider is an adapter around an external source or service.
 *
 * Providers must not become the owner of DISM domain state:
 *
 * ```text
 * Internal Domain
 *       ↑
 * External Adapter   ← 你在这里
 *       ↑
 * Provider（喜鹊儿 / 课程表 / ICS / 第三方日历）
 * ```
 *
 * ### 为什么需要 `version`
 *
 * 只记录 `provider=quequeer` 是不够的。线上出现"某个 provider
 * 行为变了"的时候，必须能回答：
 *
 * ```text
 * provider=quequeer
 * providerVersion=1.2.0     ← 关键
 * ```
 *
 * 否则只能靠猜：是适配器改了，还是源站改了。
 */
@DismPublicApi
interface DismProvider {

    /**
     * Stable provider identifier.
     */
    val id: String

    /**
     * Human-readable provider name.
     */
    val name: String

    /**
     * Provider implementation version.
     */
    val version: String

    /**
     * Provider capabilities.
     */
    val capabilities: Set<DismProviderCapability>
}
