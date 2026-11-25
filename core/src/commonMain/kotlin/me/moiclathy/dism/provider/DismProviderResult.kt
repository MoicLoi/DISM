package me.moiclathy.dism.provider

import me.moiclathy.dism.api.DismFailure
import me.moiclathy.dism.api.DismPublicApi

/**
 * Result returned by an external provider operation.
 */
@DismPublicApi
sealed interface DismProviderResult<out T> {

    /**
     * Provider operation completed successfully.
     */
    data class Success<T>(
        val value: T,
    ) : DismProviderResult<T>

    /**
     * Provider operation failed in a structured way.
     */
    data class Failure(
        val error: DismFailure,
    ) : DismProviderResult<Nothing>
}
