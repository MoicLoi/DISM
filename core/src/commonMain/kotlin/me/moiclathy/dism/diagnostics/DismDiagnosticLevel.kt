package me.moiclathy.dism.diagnostics

import me.moiclathy.dism.api.DismPublicApi

/**
 * Severity of a DISM diagnostic event.
 */
@DismPublicApi
enum class DismDiagnosticLevel {

    /**
     * Detailed information useful during normal diagnostics.
     */
    DEBUG,

    /**
     * Normal runtime information.
     */
    INFO,

    /**
     * Something unusual happened, but the operation may continue.
     */
    WARN,

    /**
     * An operation failed or an important runtime problem occurred.
     */
    ERROR,
}
