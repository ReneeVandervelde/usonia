package usonia.glass

import com.inkapplications.glassconsole.structures.*
import ink.ui.structures.Positioning
import ink.ui.structures.Sentiment
import ink.ui.structures.TextStyle
import ink.ui.structures.elements.EmptyElement
import ink.ui.structures.elements.StatusIndicatorElement
import ink.ui.structures.elements.TextElement
import ink.ui.structures.elements.ThrobberElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.foundation.LatchState
import usonia.glass.GlassPluginConfig.DisplayType.Large
import usonia.glass.GlassPluginConfig.DisplayType.Small
import usonia.rules.Flags

internal class DisplayConfigFactory {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    fun compose(viewModel: DisplayViewModel): DisplayConfig {
        val challenge = viewModel.challenge
        return when {
            challenge != null -> pinScreen(viewModel, challenge)
            viewModel.security.isArming -> armingScreen(viewModel)
            else -> controlPad(viewModel)
        }
    }

    private fun armingScreen(viewModel: DisplayViewModel): DisplayConfig {
        return DisplayConfig(
            items = listOf(
                ThrobberElement(
                    caption = "Arming Security in ${viewModel.security.armDelayMinutes} minutes."
                ).asDisplayItem(
                    position = Positioning.Center,
                ),
                ButtonItem(
                    text = "Cancel",
                    action = Action.Put("http://${viewModel.config.homeIp}/glass/arm", "false"),
                    position = Positioning.Center,
                    latching = true,
                    sentiment = Sentiment.Nominal,
                )
            ),
            expiration = UPDATE_RATE + UPDATE_GRACE,
        )

    }

    private fun pinScreen(viewModel: DisplayViewModel, challenge: DisplayViewModel.ChallengeData): DisplayConfig {
        return DisplayConfig(
            items = listOf(
                TextElement(
                    text = "Unlock",
                    style = TextStyle.H1,
                ).asDisplayItem(
                    position = Positioning.Center,
                ),
                PinPadItem(
                    position = Positioning.Center,
                    witness = challenge.witness,
                    challengeNonce = challenge.nonce,
                    callbackUrl = "http://${viewModel.config.homeIp}/glass/${viewModel.config.bridgeId.value}/disarm",
                )
            ),
            expiration = UPDATE_RATE + UPDATE_GRACE,
        )
    }

    private fun controlPad(viewModel: DisplayViewModel): DisplayConfig {
        return DisplayConfig(
            items = arrayOfNotNull(
                *controlRows(viewModel),
                *doorRows(viewModel),
            ).toList(),
            layout = LayoutType.VerticalGrid(viewModel.totalSpan),
            expiration = UPDATE_RATE + UPDATE_GRACE,
        )
    }

    private fun controlRows(viewModel: DisplayViewModel): Array<out DisplayItem> {
        val controlSpan = when (viewModel.config.type) {
            Large -> viewModel.totalSpan / 4
            Small -> viewModel.totalSpan / 3
        }
        val controls = arrayOfNotNull(
            ButtonItem(
                text = "Sleep",
                action = Action.Put("http://${viewModel.config.homeIp}/flags/${Flags.SleepMode}", json.encodeToString(viewModel.flags.sleepEnabled.not().toString())),
                latching = true,
                sentiment = if (viewModel.flags.sleepEnabled) Sentiment.Primary else Sentiment.Idle,
                position = Positioning.Center,
                span = controlSpan,
            ),
            ButtonItem(
                text = "Movie",
                action = Action.Put("http://${viewModel.config.homeIp}/flags/${Flags.MovieMode}", json.encodeToString(viewModel.flags.movieEnabled.not().toString())),
                latching = true,
                sentiment = if (viewModel.flags.movieEnabled) Sentiment.Primary else Sentiment.Idle,
                position = Positioning.Center,
                span = controlSpan,
            ),
            ButtonItem(
                text = "Lock",
                action = Action.Put("http://${viewModel.config.homeIp}/glass/arm", "true"),
                latching = true,
                sentiment = Sentiment.Caution,
                position = Positioning.Center,
                span = controlSpan,
            ),
        )
        val remainingSpace = (controls.size * controlSpan) % viewModel.totalSpan

        return arrayOfNotNull(
            TextElement(
                text = "Controls",
                style = TextStyle.H2,
            ).asDisplayItem(
                span = viewModel.totalSpan,
            ),
            *controls,
            EmptyElement.asDisplayItem(span = remainingSpace).takeIf { remainingSpace > 0 },
        )
    }

    private fun doorRows(viewModel: DisplayViewModel): Array<out DisplayItem> {
        val indicatorSpan = when (viewModel.config.type) {
            Large -> viewModel.totalSpan / 2
            Small -> viewModel.totalSpan
        }
        val items = viewModel.doorStates
            .filter { viewModel.config.type == Large || it.event?.state != LatchState.CLOSED }
            .sortedBy { it.device.name }
            .map { (device, event) ->
                StatusIndicatorElement(
                    text = device.name,
                    sentiment = when (event?.state) {
                        LatchState.OPEN -> Sentiment.Caution
                        LatchState.CLOSED -> Sentiment.Positive
                        null -> Sentiment.Idle
                    },
                ).asDisplayItem(
                    span = indicatorSpan,
                )
            }
        val remainingSpace = (items.size * indicatorSpan) % viewModel.totalSpan

        return arrayOfNotNull(
            TextElement(
                text = "Doors",
                style = TextStyle.H2,
            ).asDisplayItem(
                span = viewModel.totalSpan,
            ),
            *items.toTypedArray(),
            EmptyElement.asDisplayItem(span = remainingSpace).takeIf { remainingSpace > 0 },
        )
    }

    private val DisplayViewModel.totalSpan: Int get() = when (config.type) {
        Large -> 12
        Small -> 6
    }

    private inline fun <reified T> arrayOfNotNull(vararg items: T?): Array<T> = items.filterNotNull().toTypedArray()
}