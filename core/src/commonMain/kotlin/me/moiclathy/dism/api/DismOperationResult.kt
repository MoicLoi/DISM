package me.moiclathy.dism.api

/**
 * Structured result for operations where failure is an expected
 * part of normal control flow.
 *
 * This should be preferred over exceptions when callers are expected
 * to branch on success/failure as part of ordinary application logic.
 */
@DismPublicApi
sealed interface DismOperationResult<out T> {

    /**
     * Operation completed successfully.
     */
    @DismPublicApi
    data class Success<T>(
        val value: T,
    ) : DismOperationResult<T>

    /**
     * Operation completed with a structured product failure.
     */
    @DismPublicApi
    data class Failure(
        val error: DismFailure,
    ) : DismOperationResult<Nothing>
}
