package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DismFailureContractTest {

    @Test
    fun failureUsesStableMachineReadableCode() {

        val failure =
            DismFailure(
                code =
                    DismFailureCode.STORAGE_UNAVAILABLE,

                message =
                    "Local storage could not be opened.",

                operation =
                    "initializePersistence",

                details =
                    mapOf(
                        "database" to "dism",
                    ),
            )

        assertEquals(
            DismFailureCode.STORAGE_UNAVAILABLE,
            failure.code,
        )

        assertEquals(
            "initializePersistence",
            failure.operation,
        )

        assertEquals(
            "dism",
            failure.details["database"],
        )
    }

    @Test
    fun operationResultSeparatesSuccessAndFailure() {

        val success:
            DismOperationResult<String> =
            DismOperationResult.Success(
                value = "ok",
            )

        val failure:
            DismOperationResult<String> =
            DismOperationResult.Failure(
                error =
                    DismFailure(
                        code =
                            DismFailureCode.REPLAY_UNAVAILABLE,

                        message =
                            "Historical replay is unavailable.",
                    ),
            )

        assertIs<DismOperationResult.Success<String>>(
            success,
        )

        assertIs<DismOperationResult.Failure>(
            failure,
        )
    }

    @Test
    fun exceptionCarriesStructuredFailure() {

        val failure =
            DismFailure(
                code =
                    DismFailureCode.INVALID_STATE,

                message =
                    "The runtime is not initialized.",
            )

        val exception =
            DismException(
                failure,
            )

        assertEquals(
            DismFailureCode.INVALID_STATE,
            exception.failure.code,
        )

        assertEquals(
            failure.message,
            exception.message,
        )
    }

    @Test
    fun formatterDoesNotRequireImplementationException() {

        val failure =
            DismFailure(
                code =
                    DismFailureCode.STORAGE_FAILURE,

                message =
                    "Storage operation failed.",

                operation =
                    "writeSnapshot",

                details =
                    mapOf(
                        "entity" to "schedule",
                    ),
            )

        val formatted =
            DismFailureFormatter.format(
                failure,
            )

        assertEquals(
            "STORAGE_FAILURE [writeSnapshot]: " +
                "Storage operation failed. " +
                "(entity=schedule)",
            formatted,
        )
    }
}
