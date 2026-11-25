package me.moiclathy.dism.nl.temporal

/** Conservative temporal parser. It resolves only explicit, low-risk expressions. */
class DeterministicTemporalResolver : TemporalResolver {
    override fun resolve(text: String): TemporalResolution {
        val input = text.trim()
        if (input.isEmpty()) return TemporalResolution(emptyList(), 0.0, input)

        val dayOffset = when {
            input.contains("大后天") -> 3
            input.contains("后天") -> 2
            input.contains("明天") -> 1
            input.contains("今天") -> 0
            input.contains("昨天") -> -1
            else -> null
        }

        val meridiem = when {
            input.contains("凌晨") || input.contains("早上") || input.contains("上午") -> Meridiem.MORNING
            input.contains("下午") -> Meridiem.AFTERNOON
            input.contains("晚上") || input.contains("晚间") -> Meridiem.EVENING
            input.contains("夜里") || input.contains("夜间") -> Meridiem.NIGHT
            else -> null
        }

        val clockPatterns = listOf(
            Regex("(?<!\\d)([01]?\\d|2[0-3])[:：]([0-5]\\d)(?!\\d)"),
            Regex("(?<!\\d)([01]?\\d|2[0-3])(?:[:：]([0-5]\\d))?\\s*[点时]"),
        )
        val clock = clockPatterns.mapNotNull { it.find(input) }.minByOrNull { it.range.first }
        var hour = clock?.groupValues?.getOrNull(1)?.toIntOrNull()
        val minute = clock?.groupValues?.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: if (clock != null) 0 else null

        if (hour != null && meridiem != null) {
            hour = normalizeMeridiem(hour, meridiem)
        }

        val mentions = buildList {
            if (dayOffset != null || meridiem != null || clock != null) {
                add(
                    TemporalMention(
                        surface = input,
                        dayOffset = dayOffset,
                        hour = hour,
                        minute = minute,
                        meridiem = meridiem,
                        granularity = when {
                            clock != null -> TemporalGranularity.POINT
                            dayOffset != null -> TemporalGranularity.DAY
                            else -> TemporalGranularity.UNSPECIFIED
                        },
                        confidence = confidenceFor(dayOffset, clock, meridiem),
                    )
                )
            }
        }

        val confidence = mentions.maxOfOrNull { it.confidence } ?: 0.0
        return TemporalResolution(mentions, confidence, if (mentions.isEmpty()) input else null)
    }

    private fun normalizeMeridiem(hour: Int, meridiem: Meridiem): Int = when (meridiem) {
        Meridiem.AFTERNOON, Meridiem.EVENING, Meridiem.NIGHT -> if (hour in 1..11) hour + 12 else hour
        Meridiem.MORNING -> if (hour == 12) 0 else hour
    }

    private fun confidenceFor(dayOffset: Int?, clock: MatchResult?, meridiem: Meridiem?): Double = when {
        dayOffset != null && clock != null -> 0.98
        clock != null -> 0.95
        dayOffset != null -> 0.92
        meridiem != null -> 0.75
        else -> 0.0
    }
}
