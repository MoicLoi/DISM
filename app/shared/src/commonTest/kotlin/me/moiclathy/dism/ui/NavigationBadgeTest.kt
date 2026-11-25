package me.moiclathy.dism.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.moiclathy.dism.presentation.DismUiState
import me.moiclathy.dism.presentation.history.HistoryUiState
import me.moiclathy.dism.presentation.navigation.DismDestination

/**
 * 16.28：导航角标。
 *
 * "哪个目的地该出现角标"是一条产品规则。埋在 `when` 里
 * 靠肉眼维护的话，它会随着页面增加而悄悄失准。
 */
class NavigationBadgeTest {

    private fun stateWith(
        recordingError: String? = null,
    ) =
        DismUiState(
            history =
                HistoryUiState(
                    recordingError = recordingError,
                ),
        )

    @Test
    fun historyIsBadgedOnlyWhenRecordingIsFailing() {

        assertNull(
            badgeFor(
                DismDestination.HISTORY,
                stateWith(recordingError = null),
            ),
            "a healthy history must not be badged",
        )

        assertEquals(
            "!",
            badgeFor(
                DismDestination.HISTORY,
                stateWith(
                    recordingError = "SQLITE_READONLY",
                ),
            ),
        )
    }

    @Test
    fun otherDestinationsAreNeverBadgedByThisCondition() {

        /*
         * 记录失败只影响历史，不影响日程本身。
         *
         * 在每个页面上都挂角标会让"有东西需要注意"变成噪声——
         * 到处都在提示等于没有提示。
         */
        val failing = stateWith(recordingError = "boom")

        DismDestination.entries
            .filter { it != DismDestination.HISTORY }
            .forEach { destination ->
                assertNull(
                    badgeFor(destination, failing),
                    "unexpected badge on $destination",
                )
            }
    }

    @Test
    fun everyDestinationIsConsidered() {

        /*
         * 这条测试的价值在**穷举**。
         *
         * `badgeFor` 用的是穷举 `when`（没有 `else`），
         * 所以新增目的地时编译器会报错。这条测试再把
         * 所有取值跑一遍，确保没有一个会抛异常。
         */
        DismDestination.entries.forEach { destination ->
            badgeFor(destination, DismUiState())
            badgeFor(
                destination,
                stateWith(recordingError = "boom"),
            )
        }
    }

    @Test
    fun theBadgeCarriesAMeaningfulDescription() {

        val failing = stateWith(recordingError = "boom")

        val description =
            badgeDescriptionFor(
                DismDestination.HISTORY,
                failing,
            )

        /*
         * 只有一个 "!" 是不够的：
         * 读屏会念出"History 感叹号"，而用户不知道发生了什么。
         */
        assertTrue(
            description != null &&
                description.contains("incomplete"),
            "the badge needs a description a screen reader " +
                "can act on; was='$description'",
        )

        assertNull(
            badgeDescriptionFor(
                DismDestination.HISTORY,
                stateWith(recordingError = null),
            ),
        )
    }

    @Test
    fun theDescriptionMatchesTheBadge() {

        /*
         * 两者必须同时在或同时不在。
         *
         * 如果只有角标没有描述，读屏用户会看到一个
         * 解释不了的符号；反过来则是什么都看不到、
         * 却被告知出了问题。
         */
        listOf(
            stateWith(recordingError = null),
            stateWith(recordingError = "boom"),
        ).forEach { state ->
            DismDestination.entries.forEach { destination ->
                val badge = badgeFor(destination, state)
                val description =
                    badgeDescriptionFor(destination, state)

                assertEquals(
                    badge != null,
                    description != null,
                    "badge and description disagree on " +
                        "$destination",
                )
            }
        }
    }
}
