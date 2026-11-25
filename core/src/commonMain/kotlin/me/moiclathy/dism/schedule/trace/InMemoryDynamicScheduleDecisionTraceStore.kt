package me.moiclathy.dism.schedule.trace

class InMemoryDynamicScheduleDecisionTraceStore :
    DynamicScheduleDecisionTraceStore {

    private val records =
        LinkedHashMap<
            DynamicScheduleDecisionTraceId,
            DynamicScheduleDecisionTraceRecord,
        >()

    override suspend fun save(
        record: DynamicScheduleDecisionTraceRecord,
    ) {
        records[
            record.traceId
        ] = record
    }

    override suspend fun get(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord? {
        return records[traceId]
    }

    override suspend fun delete(
        traceId: DynamicScheduleDecisionTraceId,
    ) {
        records.remove(traceId)
    }

    override suspend fun clear() {
        records.clear()
    }

    override suspend fun query(
        query: DynamicScheduleDecisionTraceQuery,
    ): DynamicScheduleDecisionTraceQueryResult {

        val matched =
            records.values
                .asSequence()
                .filter { record ->

                    query.itemId == null ||
                        record.itemId == query.itemId
                }
                .filter { record ->

                    query.from == null ||
                        record.createdAt >= query.from
                }
                .filter { record ->

                    query.until == null ||
                        record.createdAt <= query.until
                }
                .filter { record ->

                    query.stage == null ||
                        record.trace.entries.any {
                            it.stage == query.stage
                        }
                }
                .filter { record ->

                    !query.decisiveOnly ||
                        record.trace.decisiveEntries.isNotEmpty()
                }
                .sortedByDescending {
                    it.createdAt
                }
                .toList()

        return DynamicScheduleDecisionTraceQueryResult(
            records =
                matched.take(query.limit),

            totalMatched =
                matched.size,
        )
    }
}
