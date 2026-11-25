package me.moiclathy.dism.platform

import me.moiclathy.dism.api.DismPublicApi

/**
 * Describes the current runtime platform.
 *
 * ### 为什么要有 `capabilities`（15.9.11）
 *
 * `DismPlatformInfo.platform` 只说明"这是 JVM"，
 * 但真正影响产品行为的是"这台机器能不能后台执行 / 能不能写文件"。
 *
 * ```text
 * JVM
 *  ├── LOCAL_STORAGE
 *  ├── FILE_SYSTEM
 *  ├── NETWORK
 *  └── BACKGROUND_EXECUTION
 *
 * JS
 *  ├── LOCAL_STORAGE
 *  └── NETWORK
 * ```
 *
 * 注意**不要假定每个平台一定具备全部能力**：
 * 默认是空集合，平台实现必须显式声明。
 */
@DismPublicApi
data class DismPlatformInfo(
    val platform: DismPlatform,
    val runtimeName: String,
    val runtimeVersion: String? = null,
    val capabilities:
        Set<DismPlatformCapability> = emptySet(),
) {

    init {
        require(runtimeName.isNotBlank()) {
            "runtimeName must not be blank"
        }
    }

    fun supports(
        capability: DismPlatformCapability,
    ): Boolean =
        capability in capabilities
}
