package me.moiclathy.dism.nl.query.e2e

/**
 * Orchestration seam for the query path. The supplied stages remain responsible
 * for compilation, resolution, execution and rendering; this class only records
 * their outcomes and never mutates semantic state.
 */
class SemanticQueryE2ERunner<C, R, E, O>(
    private val compile: (String) -> C?,
    private val resolve: (C) -> R?,
    private val executable: (R) -> Boolean,
    private val execute: (R) -> E?,
    private val render: (E) -> O?,
) {
    fun run(input: String): SemanticQueryE2ETrace {
        val compiled = compile(input) ?: return SemanticQueryE2ETrace(input, false, false, false, false)
        val resolved = resolve(compiled) ?: return SemanticQueryE2ETrace(input, true, false, false, false)
        if (!executable(resolved)) return SemanticQueryE2ETrace(input, true, true, false, false)
        val executed = execute(resolved) ?: return SemanticQueryE2ETrace(input, true, true, true, false)
        val rendered = render(executed)
        return SemanticQueryE2ETrace(input, true, true, true, rendered != null, rendered?.toString())
    }
}
