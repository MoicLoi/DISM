package me.moiclathy.dism.diagnostics

import me.moiclathy.dism.api.DismPublicApi

/**
 * Public diagnostics configuration.
 */
@DismPublicApi
data class DismDiagnosticsConfiguration(
    val enabled: Boolean = true,
    val sink: DismDiagnosticSink =
        DismDiagnosticSinks.none,
)
