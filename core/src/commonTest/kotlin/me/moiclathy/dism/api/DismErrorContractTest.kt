package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DismErrorContractTest {

    @Test
    fun failureCodeIsMachineReadable() {

        val failure =
            DismFailure(
                code =
                    DismFailureCode.STORAGE_UNAVAILABLE,

                message =
                    "Storage is unavailable.",
            )

        assertEquals(
            DismFailureCode.STORAGE_UNAVAILABLE,
            failure.code,
        )
    }

    @Test
    fun operationResultHasExplicitFailureVariant() {

        val result:
            DismOperationResult<String> =
            DismOperationResult.Failure(
                error =
                    DismFailure(
                        code =
                            DismFailureCode.INVALID_INPUT,

                        message =
                            "Invalid input.",
                    ),
            )

        assertIs<DismOperationResult.Failure>(
            result,
        )
    }

    @Test
    fun exceptionPreservesStructuredFailure() {

        val failure =
            DismFailure(
                code =
                    DismFailureCode.INTERNAL_FAILURE,

                message =
                    "Unexpected failure.",
            )

        val exception =
            DismException(
                failure,
            )

        assertEquals(
            DismFailureCode.INTERNAL_FAILURE,
            exception.failure.code,
        )
    }
}
