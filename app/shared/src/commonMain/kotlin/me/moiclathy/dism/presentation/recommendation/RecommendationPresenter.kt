package me.moiclathy.dism.presentation.recommendation

import androidx.compose.runtime.Composable
import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import org.jetbrains.compose.resources.stringResource
import dism.app.shared.generated.resources.Res
import dism.app.shared.generated.resources.recommendation_generic_title
import dism.app.shared.generated.resources.recommendation_prepare_item_known_location
import dism.app.shared.generated.resources.recommendation_prepare_item_title
import dism.app.shared.generated.resources.recommendation_prepare_item_unknown_location

/**
 * Converts a language-neutral [Recommendation] into localized
 * [RecommendationText] using Compose Multiplatform string resources.
 */
@Composable
fun Recommendation.toPresentationText(): RecommendationText {
    return when (val msg = message) {

        is RecommendationMessage.PrepareItemKnownLocation -> {
            RecommendationText(
                title = stringResource(
                    Res.string.recommendation_prepare_item_title,
                    msg.itemName,
                ),
                message = stringResource(
                    Res.string.recommendation_prepare_item_known_location,
                    msg.eventName,
                    msg.itemName,
                    msg.locationName,
                ),
            )
        }

        is RecommendationMessage.PrepareItemUnknownLocation -> {
            RecommendationText(
                title = stringResource(
                    Res.string.recommendation_prepare_item_title,
                    msg.itemName,
                ),
                message = stringResource(
                    Res.string.recommendation_prepare_item_unknown_location,
                    msg.eventName,
                    msg.itemName,
                ),
            )
        }

        is RecommendationMessage.Generic -> {
            RecommendationText(
                title = stringResource(
                    Res.string.recommendation_generic_title,
                ),
                message = msg.key,
            )
        }
    }
}
