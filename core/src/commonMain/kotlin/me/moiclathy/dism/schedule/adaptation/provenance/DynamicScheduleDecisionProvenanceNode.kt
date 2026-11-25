package me.moiclathy.dism.schedule.adaptation.provenance

data class DynamicScheduleDecisionProvenanceNode(
    val id: String,

    val type:
        DynamicScheduleDecisionProvenanceNodeType,

    /**
     * 指向上游 Provenance 节点。
     */
    val parentIds:
        List<String> = emptyList(),

    /**
     * 是否直接参与最终决策。
     */
    val decisive:
        Boolean = false,

    /**
     * 人类可读的简短说明。
     */
    val message:
        String,

    /**
     * 结构化诊断信息。
     *
     * 注意：
     *
     * 这里保存的是 provenance metadata，
     * 而不是完整领域对象。
     */
    val metadata:
        Map<String, String> = emptyMap(),
) {

    init {
        require(id.isNotBlank())
        require(message.isNotBlank())
        require(parentIds.none { it.isBlank() })
    }
}
