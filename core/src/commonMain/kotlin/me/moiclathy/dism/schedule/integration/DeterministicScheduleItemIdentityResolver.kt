package me.moiclathy.dism.schedule.integration

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionIdentityResolver
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 缝合层：把 schedule item 映射到稳定的 RecommendationKey。
 *
 * 说明：
 *
 * 生产环境里这个映射应该来自：
 *
 * ```text
 * External Schedule Entry
 *        ↓
 * 11.7 Entity Binding
 *        ↓
 * Entity / Event
 *        ↓
 * RecommendationKey
 * ```
 *
 * 这一版 Vertical Slice 还没有接 11.7 的 Entity Registry，
 * 因此先用 [DeterministicUuid] 从 external key
 * 派生**确定性**的 Uuid：
 *
 * - 同一个 external key 永远得到同一个 key；
 * - 不依赖随机数，因此 Replay / Verification 是确定性的。
 *
 * 等 11.7 Entity Binding 接入后，
 * 只需要替换这一个 resolver。
 */
@OptIn(ExperimentalUuidApi::class)
class DeterministicScheduleItemIdentityResolver(
    private val keys:
        Map<DynamicScheduleItemId, RecommendationKey>,
) : DynamicScheduleAttentionIdentityResolver {

    override fun resolve(
        itemId: DynamicScheduleItemId,
    ): RecommendationKey? =
        keys[itemId]

    companion object {

        /**
         * 依据 external key 为每个 item 生成稳定的 RecommendationKey。
         */
        fun from(
            items:
                Map<DynamicScheduleItemId, String>,
        ): DeterministicScheduleItemIdentityResolver {

            return DeterministicScheduleItemIdentityResolver(
                keys =
                    items.mapValues { (_, externalKey) ->
                        RecommendationKey(
                            type =
                                RecommendationType
                                    .CHECK_INFORMATION,

                            eventId =
                                DeterministicUuid.from(
                                    "dism:event:$externalKey",
                                ),

                            requiredEntityId =
                                DeterministicUuid.from(
                                    "dism:entity:$externalKey",
                                ),
                        )
                    },
            )
        }
    }
}

/**
 * 从字符串派生确定性 Uuid（FNV-1a 64bit × 2）。
 *
 * 这不是加密哈希，只用于把 external key 稳定地映射成 Uuid，
 * 保证同一输入在多次运行 / Replay 中得到相同的 key。
 */
@OptIn(ExperimentalUuidApi::class)
object DeterministicUuid {

    fun from(
        value: String,
    ): Uuid {

        val bytes = value.encodeToByteArray()

        val high = fnv1a(bytes, seed = FNV_OFFSET)
        val low = fnv1a(bytes, seed = high xor FNV_OFFSET)

        return Uuid.parse(
            format(high, low),
        )
    }

    private const val FNV_OFFSET =
        -0x340d631b7bdddcdbL

    private const val FNV_PRIME =
        0x100000001b3L

    private fun fnv1a(
        bytes: ByteArray,
        seed: Long,
    ): Long {

        var hash = seed

        bytes.forEach { byte ->
            hash = hash xor (byte.toLong() and 0xffL)
            hash *= FNV_PRIME
        }

        return hash
    }

    private fun format(
        high: Long,
        low: Long,
    ): String {

        val highHex =
            high.toULong()
                .toString(16)
                .padStart(16, '0')

        val lowHex =
            low.toULong()
                .toString(16)
                .padStart(16, '0')

        val all = highHex + lowHex

        return buildString {
            append(all, 0, 8)
            append('-')
            append(all, 8, 12)
            append("-4")
            append(all, 13, 16)
            append("-8")
            append(all, 17, 20)
            append('-')
            append(all, 20, 32)
        }
    }
}
