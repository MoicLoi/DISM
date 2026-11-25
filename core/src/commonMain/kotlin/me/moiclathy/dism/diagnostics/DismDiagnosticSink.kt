package me.moiclathy.dism.diagnostics

import me.moiclathy.dism.api.DismPublicApi

/**
 * Receives structured DISM diagnostic events.
 *
 * DISM does not prescribe a logging implementation.
 *
 * Applications may connect this to:
 * - console logging
 * - file logging
 * - telemetry
 * - IDE diagnostics
 * - test collectors
 */
@DismPublicApi
fun interface DismDiagnosticSink {

    fun emit(
        event: DismDiagnosticEvent,
    )
}
