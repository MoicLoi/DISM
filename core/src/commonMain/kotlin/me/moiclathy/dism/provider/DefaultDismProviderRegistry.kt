package me.moiclathy.dism.provider

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.diagnostics.DismDiagnostics

/**
 * Default in-memory provider registry.
 *
 * ### 为什么保留插入顺序
 *
 * `all()` 返回的是**注册顺序**，而不是哈希顺序。
 * 同一组 provider 在任何进程里都必须得到同样的列表——
 * 否则诊断输出、契约测试、UI 列表都会随运行而变化。
 *
 * ### 15.8.10：诊断是真的发出去了
 *
 * 规格列了一串 `DISM.PROVIDER.*` code，但没给出实现。
 * 这里让 registry 接受一个可选的 [DismDiagnostics]，
 * 注册/注销时真的发射事件。
 *
 * 默认是 no-op，所以 `DefaultDismProviderRegistry()` 这个
 * 既有调用形态继续有效。
 */
@DismInternalApi
class DefaultDismProviderRegistry(
    private val diagnostics: DismDiagnostics =
        DismDiagnostics(),
) : DismProviderRegistry {

    private val providers =
        linkedMapOf<String, DismProvider>()

    override fun register(
        provider: DismProvider,
    ) {
        require(provider.id.isNotBlank()) {
            "provider.id must not be blank"
        }

        val replaced =
            providers.put(provider.id, provider) != null

        diagnostics.info(
            message =
                if (replaced) {
                    "Provider re-registered: ${provider.id}"
                } else {
                    "Provider registered: ${provider.id}"
                },
            component = "provider",
            operation = "register",
            code = DismProviderDiagnosticCodes.REGISTERED,
            attributes =
                mapOf(
                    "providerId" to provider.id,
                    "providerVersion" to provider.version,
                    "replaced" to replaced.toString(),
                ),
        )
    }

    override fun unregister(
        providerId: String,
    ) {
        val removed =
            providers.remove(providerId) != null

        diagnostics.info(
            message =
                if (removed) {
                    "Provider unregistered: $providerId"
                } else {
                    "Provider was not registered: $providerId"
                },
            component = "provider",
            operation = "unregister",
            code = DismProviderDiagnosticCodes.UNREGISTERED,
            attributes =
                mapOf(
                    "providerId" to providerId,
                    "removed" to removed.toString(),
                ),
        )
    }

    override fun find(
        providerId: String,
    ): DismProvider? =
        providers[providerId]

    override fun all():
        List<DismProvider> =
        providers.values.toList()
}

/**
 * 15.8.10：Provider 操作的诊断 code。
 *
 * 规范形态与 15.7 一致：
 *
 * ```text
 * DISM.PROVIDER.<EVENT>
 * ```
 */
@DismInternalApi
object DismProviderDiagnosticCodes {

    const val REGISTERED: String =
        "DISM.PROVIDER.REGISTERED"

    const val UNREGISTERED: String =
        "DISM.PROVIDER.UNREGISTERED"

    const val REQUEST_STARTED: String =
        "DISM.PROVIDER.REQUEST.STARTED"

    const val REQUEST_COMPLETED: String =
        "DISM.PROVIDER.REQUEST.COMPLETED"

    const val REQUEST_FAILED: String =
        "DISM.PROVIDER.REQUEST.FAILED"

    const val SYNC_STARTED: String =
        "DISM.PROVIDER.SYNC.STARTED"

    const val SYNC_COMPLETED: String =
        "DISM.PROVIDER.SYNC.COMPLETED"

    const val SYNC_FAILED: String =
        "DISM.PROVIDER.SYNC.FAILED"
}
