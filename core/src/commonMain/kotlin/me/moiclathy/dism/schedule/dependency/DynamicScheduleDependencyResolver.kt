package me.moiclathy.dism.schedule.dependency

fun interface DynamicScheduleDependencyResolver {
    fun resolve(input: DynamicScheduleDependencyInput): DynamicScheduleDependencyResolutionResult
}
