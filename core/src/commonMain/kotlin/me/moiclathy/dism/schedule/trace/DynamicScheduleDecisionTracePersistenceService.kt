package me.moiclathy.dism.schedule.trace

class DynamicScheduleDecisionTracePersistenceService(
    private val store:
        DynamicScheduleDecisionTraceStore,
) {

    suspend fun persist(
        record: DynamicScheduleDecisionTraceRecord,
    ) {
        store.save(record)
    }

    suspend fun find(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord? {
        return store.get(traceId)
    }

    suspend fun delete(
        traceId: DynamicScheduleDecisionTraceId,
    ) {
        store.delete(traceId)
    }

    suspend fun clear() {
        store.clear()
    }

    suspend fun query(
        query: DynamicScheduleDecisionTraceQuery,
    ): DynamicScheduleDecisionTraceQueryResult {
        return store.query(query)
    }
}
