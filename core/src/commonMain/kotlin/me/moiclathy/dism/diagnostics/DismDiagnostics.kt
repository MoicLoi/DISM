package me.moiclathy.dism.diagnostics

import kotlin.time.Clock
import me.moiclathy.dism.api.DismInternalApi

/**
 * Internal diagnostic dispatcher.
 *
 * All internal components should emit diagnostics through this
 * abstraction instead of directly depending on a logging framework.
 */
@DismInternalApi
class DismDiagnostics(
    private val sink: DismDiagnosticSink =
        DismDiagnosticSinks.none,
) {

    fun debug(
        message: String,
        component: String? = null,
        operation: String? = null,
        code: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ) {
        emit(
            level = DismDiagnosticLevel.DEBUG,
            message = message,
            component = component,
            operation = operation,
            code = code,
            attributes = attributes,
        )
    }

    fun info(
        message: String,
        component: String? = null,
        operation: String? = null,
        code: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ) {
        emit(
            level = DismDiagnosticLevel.INFO,
            message = message,
            component = component,
            operation = operation,
            code = code,
            attributes = attributes,
        )
    }

    fun warn(
        message: String,
        component: String? = null,
        operation: String? = null,
        code: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ) {
        emit(
            level = DismDiagnosticLevel.WARN,
            message = message,
            component = component,
            operation = operation,
            code = code,
            attributes = attributes,
        )
    }

    fun error(
        message: String,
        component: String? = null,
        operation: String? = null,
        code: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ) {
        emit(
            level = DismDiagnosticLevel.ERROR,
            message = message,
            component = component,
            operation = operation,
            code = code,
            attributes = attributes,
        )
    }

    private fun emit(
        level: DismDiagnosticLevel,
        message: String,
        component: String?,
        operation: String?,
        code: String?,
        attributes: Map<String, String>,
    ) {
        sink.emit(
            DismDiagnosticEvent(
                level = level,
                message = message,
                timestamp = Clock.System.now(),
                component = component,
                operation = operation,
                code = code,
                attributes = attributes,
            ),
        )
    }
}
