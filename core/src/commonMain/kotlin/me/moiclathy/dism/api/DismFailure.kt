package me.moiclathy.dism.api

/**
 * Structured failure information exposed by the DISM Public API.
 *
 * A failure is not necessarily a programming error.
 *
 * It represents a condition that the application may reasonably
 * want to report, retry, downgrade, or otherwise handle.
 *
 * Internal implementation exceptions must not be exposed directly
 * as part of the public API contract.
 */
@DismPublicApi
data class DismFailure(
    val code: DismFailureCode,
    val message: String,
    val operation: String? = null,
    val details: Map<String, String> = emptyMap(),
)

/**
 * Stable machine-readable failure categories.
 *
 * Do not use the message text for program logic.
 * Application code should branch on this enum instead.
 */
@DismPublicApi
enum class DismFailureCode {

    /**
     * Public configuration is invalid or internally inconsistent.
     */
    INVALID_CONFIGURATION,

    /**
     * A requested capability is not available in the current runtime.
     */
    UNSUPPORTED_CAPABILITY,

    /**
     * Local storage could not be opened or initialized.
     */
    STORAGE_UNAVAILABLE,

    /**
     * A storage operation failed.
     */
    STORAGE_FAILURE,

    /**
     * The requested operation conflicts with the current runtime state.
     */
    INVALID_STATE,

    /**
     * Input cannot be safely accepted by the requested operation.
     */
    INVALID_INPUT,

    /**
     * An expected domain operation could not be completed.
     */
    OPERATION_REJECTED,

    /**
     * A replay or historical operation cannot be performed with the
     * currently available information/version.
     */
    REPLAY_UNAVAILABLE,

    /**
     * An internal failure escaped the normal operation boundary.
     *
     * Applications should normally report this rather than attempting
     * to infer the internal implementation details.
     */
    INTERNAL_FAILURE,
}
