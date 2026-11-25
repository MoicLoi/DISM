package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry

/**
 * 确定性 External Schedule Normalizer。
 *
 * 所有规则都是显式、可测试、可重复的。
 */
class DeterministicExternalScheduleNormalizer(
    private val policy:
        ExternalScheduleNormalizationPolicy =
        ExternalScheduleNormalizationPolicy(),
) : ExternalScheduleNormalizer {

    override fun normalize(
        schedule: ExternalSchedule,
        normalizedAt: Instant,
    ): ExternalScheduleNormalizationResult {

        return try {

            val warnings =
                mutableListOf<String>()

            val normalizedEntries =
                schedule.entries.map { entry ->

                    normalizeEntry(
                        entry = entry,
                        warnings = warnings,
                    )
                }

            val normalizedSchedule =
                NormalizedExternalSchedule(
                    id =
                        schedule.id,

                    source =
                        schedule.source,

                    name =
                        normalizeText(
                            schedule.name,
                        ) ?: schedule.name,

                    normalizedAt =
                        normalizedAt,

                    entries =
                        normalizedEntries,
                )

            ExternalScheduleNormalizationResult.success(
                schedule =
                    normalizedSchedule,

                warnings =
                    warnings,
            )

        } catch (
            exception: IllegalArgumentException
        ) {

            ExternalScheduleNormalizationResult.failure(
                error =
                    exception.message
                        ?: "External schedule normalization failed.",
            )
        }
    }

    private fun normalizeEntry(
        entry: ExternalScheduleEntry,
        warnings: MutableList<String>,
    ): NormalizedExternalScheduleEntry {

        val originalTitle =
            entry.title

        val originalLocation =
            entry.location

        val originalInstructor =
            entry.instructor

        val originalCourseCode =
            entry.courseCode

        val originalDescription =
            entry.description

        val title =
            normalizeText(
                entry.title
            )
                ?: throw IllegalArgumentException(
                    "Entry title became empty after normalization: ${entry.id}"
                )

        val location =
            normalizeLocation(
                entry.location
            )

        val instructor =
            normalizeText(
                entry.instructor
            )

        val courseCode =
            normalizeCourseCode(
                entry.courseCode
            )

        val description =
            normalizeText(
                entry.description
            )

        if (
            originalTitle != title
        ) {
            warnings +=
                "Normalized title for entry ${entry.id}."
        }

        if (
            originalLocation != location
        ) {
            warnings +=
                "Normalized location for entry ${entry.id}."
        }

        if (
            originalCourseCode != courseCode
        ) {
            warnings +=
                "Normalized course code for entry ${entry.id}."
        }

        val metadata: Map<String, String> =
            if (policy.preserveOriginalMetadata) {
                entry.metadata.values
            } else {
                emptyMap()
            }

        return NormalizedExternalScheduleEntry(
            id =
                entry.id,

            title =
                title,

            startAt =
                entry.startAt,

            endAt =
                entry.endAt,

            location =
                location,

            instructor =
                instructor,

            courseCode =
                courseCode,

            description =
                description,

            metadata =
                metadata,

            originalTitle =
                originalTitle
                    .takeIf {
                        it != title
                    },

            originalLocation =
                originalLocation
                    ?.takeIf {
                        it != location
                    },

            originalInstructor =
                originalInstructor
                    ?.takeIf {
                        it != instructor
                    },

            originalCourseCode =
                originalCourseCode
                    ?.takeIf {
                        it != courseCode
                    },

            originalDescription =
                originalDescription
                    ?.takeIf {
                        it != description
                    },
        )
    }

    private fun normalizeText(
        value: String?,
    ): String? {

        if (value == null) {
            return null
        }

        var normalized =
            value

        if (policy.trimText) {
            normalized =
                normalized.trim()
        }

        if (policy.collapseWhitespace) {
            normalized =
                collapseWhitespace(
                    normalized
                )
        }

        if (
            policy.emptyTextToNull &&
            normalized.isBlank()
        ) {
            return null
        }

        return normalized
    }

    private fun normalizeLocation(
        value: String?,
    ): String? {

        val normalized =
            normalizeText(
                value
            )

        if (
            normalized == null ||
            !policy.normalizeLocation
        ) {
            return normalized
        }

        return normalized
            .replace(
                " ",
                "",
            )
    }

    private fun normalizeCourseCode(
        value: String?,
    ): String? {

        val normalized =
            normalizeText(
                value
            )

        if (
            normalized == null ||
            !policy.normalizeCourseCode
        ) {
            return normalized
        }

        return normalized
            .replace(
                " ",
                "",
            )
            .uppercase()
    }

    private fun collapseWhitespace(
        value: String,
    ): String {

        val builder =
            StringBuilder()

        var previousWasWhitespace =
            false

        value.forEach { character ->

            if (character.isWhitespace()) {

                if (!previousWasWhitespace) {
                    builder.append(' ')
                }

                previousWasWhitespace = true

            } else {

                builder.append(
                    character
                )

                previousWasWhitespace = false
            }
        }

        return builder
            .toString()
            .trim()
    }
}
