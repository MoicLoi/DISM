package me.moiclathy.dism.api

/**
 * Public exception boundary for DISM operations that cannot return
 * a structured result directly.
 *
 * This exception carries a stable [failure] object.
 *
 * The underlying implementation exception is intentionally not part
 * of the public API contract.
 */
@DismPublicApi
class DismException(
    val failure: DismFailure,
) : IllegalStateException(
    failure.message,
)
