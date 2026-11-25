package me.moiclathy.dism.external.schedule

import kotlin.time.Instant

/**
 * External Schedule 工厂。
 *
 * 只负责创建经过基础校验的模型。
 */
object ExternalScheduleFactory {

    fun createEntry(
        id: String,
        title: String,
        source: ExternalScheduleOrigin,
        startAt: Instant? = null,
        endAt: Instant? = null,
        location: String? = null,
        instructor: String? = null,
        courseCode: String? = null,
        description: String? = null,
        metadata: ExternalScheduleMetadata =
            ExternalScheduleMetadata.EMPTY,
    ): ExternalScheduleEntry {

        return ExternalScheduleEntry(
            id =
                ExternalScheduleId(id),

            title =
                title,

            source =
                source,

            startAt =
                startAt,

            endAt =
                endAt,

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
        )
    }

    fun createSchedule(
        id: String,
        source: ExternalScheduleOrigin,
        name: String,
        importedAt: Instant,
        entries: List<ExternalScheduleEntry>,
        metadata: ExternalScheduleMetadata =
            ExternalScheduleMetadata.EMPTY,
    ): ExternalSchedule {

        return ExternalSchedule(
            id =
                ExternalScheduleId(id),

            source =
                source,

            name =
                name,

            importedAt =
                importedAt,

            entries =
                entries,

            metadata =
                metadata,
        )
    }
}
