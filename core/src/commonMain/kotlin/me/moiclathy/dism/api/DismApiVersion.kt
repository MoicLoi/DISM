package me.moiclathy.dism.api

/**
 * DISM Public API version.
 *
 * 15.x 开始进入产品化阶段。
 *
 * 注意：
 *
 * ```text
 * 这里不是整个应用的版本号，
 * 而是 Public API compatibility version。
 * ```
 *
 * 两者的区别在于：应用可以每天发版，而 Public API
 * 只有在**破坏性变更**时才递增 MAJOR。
 *
 * ### 变更记录
 *
 * ```text
 * 1.0.0   15.x 冻结的公共面
 *
 * 1.1.0   additive：DynamicScheduleFacade.preview(model, now)
 *
 * 1.2.0   additive：JvmDismFactory / AndroidDismFactory（平台落盘）
 *
 * 1.3.0   additive：DismScheduleImport.fromIcs（ICS → 模型）
 *
 * 1.4.0   additive：DynamicScheduleFacade.latestDecisions（批量查询）
 *
 * 1.5.0   additive：WebDismFactory.create（浏览器键值存储）
 * ```
 *
 * 五次都是**新增**，没有改动任何已有签名，
 * 所以按 SemVer 规则都属于 MINOR。
 * 见 `docs/developer/compatibility.md`。
 */
@DismPublicApi
object DismApiVersion {

    const val MAJOR: Int = 1

    const val MINOR: Int = 5

    const val PATCH: Int = 0

    const val STRING: String =
        "$MAJOR.$MINOR.$PATCH"
}
