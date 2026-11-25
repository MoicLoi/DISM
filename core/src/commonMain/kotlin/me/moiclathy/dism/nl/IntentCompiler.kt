package me.moiclathy.dism.nl

/**
 * Deterministic compiler for the small set of high-confidence user intents
 * that can be recognized without an LLM. It produces candidates only.
 */
class IntentCompiler {
    fun compile(input: NaturalLanguageInput): SemanticParseResult {
        val normalized = DeterministicSemanticNormalizer.normalize(input)
        val text = normalized.text
        if (text.isBlank()) {
            return SemanticParseResult(normalized, emptyList(), warnings = listOf("empty input"))
        }

        val type = when {
            containsAny(text, "查一下", "查查", "查询", "现在什么", "在哪里", "在哪儿", "在哪") -> SemanticIntentType.QUERY_STATE
            containsAny(text, "取消", "不用了", "别提醒", "删除这个安排") -> SemanticIntentType.CANCEL
            containsAny(text, "完成了", "已经完成", "做完了", "搞定了", "拿到了", "已经拿了") -> SemanticIntentType.COMPLETE
            containsAny(text, "改成", "改到", "调整到", "改为", "换成") -> SemanticIntentType.UPDATE_EVENT
            containsAny(text, "提醒我", "记得提醒", "别忘了", "到时候提醒") -> SemanticIntentType.CREATE_EVENT
            containsAny(text, "明天", "后天", "今天", "周一", "周二", "周三", "周四", "周五", "周六", "周日") &&
                containsAny(text, "有课", "上课", "实验课", "考试", "会议", "预约", "要去") -> SemanticIntentType.CREATE_EVENT
            containsAny(text, "放在", "放到", "放回", "在宿舍", "在教室", "在实验室", "在桌上", "在书包") -> SemanticIntentType.SET_LOCATION
            containsAny(text, "需要带", "要带", "需要用", "需要拿", "别忘带") -> SemanticIntentType.ADD_REQUIREMENT
            containsAny(text, "我把", "我将", "我已经", "现在") && containsAny(text, "放", "拿", "带", "在") -> SemanticIntentType.UPDATE_FACT
            containsAny(text, "是", "有", "在") -> SemanticIntentType.ASSERT_FACT
            else -> SemanticIntentType.UNKNOWN
        }

        val mentions = extractMentions(text, type)
        val confidence = when (type) {
            SemanticIntentType.UNKNOWN -> 0.20
            SemanticIntentType.QUERY_STATE,
            SemanticIntentType.CANCEL,
            SemanticIntentType.COMPLETE,
            SemanticIntentType.SET_LOCATION,
            SemanticIntentType.ADD_REQUIREMENT -> 0.85
            SemanticIntentType.CREATE_EVENT,
            SemanticIntentType.UPDATE_EVENT,
            SemanticIntentType.UPDATE_FACT,
            SemanticIntentType.ASSERT_FACT -> 0.70
        }

        val warnings = if (type == SemanticIntentType.UNKNOWN) {
            listOf("no deterministic intent matched; use an LLM or another language frontend")
        } else emptyList()

        return SemanticParseResult(
            input = normalized,
            intents = listOf(SemanticIntent(type, confidence, mentions)),
            unresolvedText = if (mentions.isEmpty()) text else "",
            warnings = warnings,
        )
    }

    private fun extractMentions(text: String, type: SemanticIntentType): List<SemanticMention> {
        val candidates = linkedSetOf<String>()
        val patterns = when (type) {
            SemanticIntentType.ADD_REQUIREMENT -> listOf("需要带([^，。；;]+)", "要带([^，。；;]+)", "需要拿([^，。；;]+)")
            SemanticIntentType.SET_LOCATION -> listOf("([^，。；;]+)(?:放在|放到|放回)([^，。；;]+)")
            SemanticIntentType.COMPLETE -> listOf("已经([^，。；;]+)", "完成了([^，。；;]*)", "拿到了([^，。；;]+)")
            else -> emptyList()
        }
        for (pattern in patterns) {
            Regex(pattern).findAll(text).forEach { match ->
                match.groupValues.drop(1).filter { it.isNotBlank() }.forEach { candidates += it.trim() }
            }
        }
        if (candidates.isEmpty() && text.length <= 32 && type != SemanticIntentType.UNKNOWN) {
            candidates += text
        }
        return candidates.map { SemanticMention(it) }
    }

    private fun containsAny(text: String, vararg values: String): Boolean = values.any(text::contains)
}
