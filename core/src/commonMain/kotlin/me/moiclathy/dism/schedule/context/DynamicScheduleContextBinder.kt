package me.moiclathy.dism.schedule.context

fun interface DynamicScheduleContextBinder {

    /**
     * Projects current context onto a dynamic schedule model.
     *
     * The binder itself does not mutate persistent state.
     */
    fun bind(
        input: DynamicScheduleContextBindingInput,
    ): DynamicScheduleContextBindingResult
}
