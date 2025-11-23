package usonia.glass

import com.inkapplications.glassconsole.structures.*
import ink.ui.structures.Positioning
import ink.ui.structures.Sentiment
import ink.ui.structures.Symbol
import ink.ui.structures.TextStyle
import ink.ui.structures.elements.*
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.compareTo
import inkapplications.spondee.structure.roundToInt
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.foundation.Identifier
import usonia.foundation.LatchState
import usonia.foundation.unit.compareTo
import usonia.glass.GlassPluginConfig.DisplayMode
import usonia.glass.GlassPluginConfig.DisplayType.Large
import usonia.glass.GlassPluginConfig.DisplayType.Small
import usonia.rules.Flags

internal class DisplayConfigFactory(
    private val logger: KimchiLogger,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    fun compose(viewModel: DisplayViewModel): DisplayConfig {
        val challenge = viewModel.security.challenge
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
                    leadingSymbol = Symbol.Close,
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
                *weatherRows(viewModel),
            ).toList(),
            layout = LayoutType.VerticalGrid(viewModel.totalSpan),
            expiration = UPDATE_RATE + UPDATE_GRACE,
            backlight = with(viewModel) {
                when {
                    config.movieMode.takeIf { flags.movieEnabled } == DisplayMode.Off -> BacklightConfig.Off()
                    config.sleepMode.takeIf { flags.sleepEnabled } == DisplayMode.Off -> BacklightConfig.Off()
                    config.movieMode.takeIf { flags.movieEnabled } == DisplayMode.Dim -> BacklightConfig.Fixed(10.percent)
                    config.sleepMode.takeIf { flags.sleepEnabled } == DisplayMode.Dim -> BacklightConfig.Fixed(10.percent)
                    else -> BacklightConfig.Auto
                }
            },
        )
    }

    private fun controlRows(viewModel: DisplayViewModel): Array<out DisplayItem> {
        val controlSpan = when (viewModel.config.type) {
            Large -> viewModel.totalSpan / 4
            Small -> viewModel.totalSpan / 3
        }
        val controls = arrayOfNotNull(
            ButtonItem(
                text = "Sleep".takeIf { viewModel.config.type == Large }.orEmpty(),
                leadingSymbol = Symbol.Bed,
                action = Action.Put("http://${viewModel.config.homeIp}/flags/${Flags.SleepMode}", json.encodeToString(viewModel.flags.sleepEnabled.not().toString())),
                latching = true,
                sentiment = if (viewModel.flags.sleepEnabled) Sentiment.Primary else Sentiment.Idle,
                position = Positioning.Center,
                span = controlSpan,
            ),
            ButtonItem(
                text = "Movie".takeIf { viewModel.config.type == Large }.orEmpty(),
                leadingSymbol = Symbol.Movie,
                action = Action.Put("http://${viewModel.config.homeIp}/flags/${Flags.MovieMode}", json.encodeToString(viewModel.flags.movieEnabled.not().toString())),
                latching = true,
                sentiment = if (viewModel.flags.movieEnabled) Sentiment.Primary else Sentiment.Idle,
                position = Positioning.Center,
                span = controlSpan,
            ),
            ButtonItem(
                text = "Snooze".takeIf { viewModel.config.type == Large }.orEmpty(),
                leadingSymbol = Symbol.AlarmOff,
                action = Action.Post("http://${viewModel.config.homeIp}/actions", usonia.foundation.Action.Intent(
                    target = Identifier("usonia.rules.lights.WakeLight"),
                    action = "usonia.rules.lights.WakeLight.dismiss"
                ).let { Json.encodeToString(usonia.foundation.Action.serializer(), it) }),
                latching = true,
                sentiment = Sentiment.Idle,
                position = Positioning.Center,
                span = controlSpan,
            ).takeIf { viewModel.config.type != Large },
            ButtonItem(
                text = "Lock".takeIf { viewModel.config.type == Large }.orEmpty(),
                leadingSymbol = Symbol.Lock,
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

    private fun weatherRows(viewModel: DisplayViewModel): Array<out DisplayItem> {
        if (viewModel.config.type == Small) return emptyArray()
        return viewModel.expandedWeather.map { location ->
            val weatherElementSpan = if (location.forecasts.isNotEmpty()) {
                viewModel.totalSpan / location.forecasts.size
            } else 0
            listOf(
                TextElement(
                    text = location.name,
                    style = TextStyle.H3,
                ).asDisplayItem(
                    span = viewModel.totalSpan,
                ),
                *(0 until (viewModel.totalSpan - (location.forecasts.size * weatherElementSpan))).map {
                    EmptyElement.asDisplayItem()
                }.toTypedArray(),
                *location.forecasts.map { forecast ->
                    val precipitationExpected = forecast.forecast.precipitation.toWholePercentage() > 15.percent
                    val lowTemperature = forecast.forecast.lowTemperature?.toFahrenheit()
                    val highTemperature = forecast.forecast.highTemperature?.toFahrenheit()
                    val condition = when {
                        precipitationExpected && lowTemperature != null && lowTemperature >= 36.fahrenheit-> WeatherElement.Condition.Rainy
                        precipitationExpected -> WeatherElement.Condition.Snowy
                        else -> WeatherElement.Condition.Clear
                    }
                    WeatherElement(
                        // TODO: Remove when text cutoff issues are resolved.
                        temperature = when {
                            forecast.daytime && highTemperature != null -> highTemperature.roundToInt().formatTemp()
                            !forecast.daytime && lowTemperature != null -> lowTemperature.roundToInt().formatTemp()
                            else -> "--"
                        },
                        condition = condition,
                        daytime = forecast.daytime,
                        title = forecast.title,
                        // TODO: Remove when night color is fixed in InkUI
                        sentiment = if (!forecast.daytime && condition == WeatherElement.Condition.Clear) Sentiment.Idle else null,
                        secondaryText = forecast.forecast.precipitation.toWholePercentage().takeIf { it > 15.percent }?.roundToInt()?.let { "$it%" },
                    ).asDisplayItem(
                        span = weatherElementSpan,
                    )
                }.toTypedArray()
            )
        }.flatten().toTypedArray()
    }

    private fun Int.formatTemp(): String
    {
        return if (this >= 0) "$this°" else "$this"
    }

    private fun doorRows(viewModel: DisplayViewModel): Array<out DisplayItem> {
        val indicatorSpan = when (viewModel.config.type) {
            Large -> viewModel.totalSpan / 2
            Small -> viewModel.totalSpan
        }
        val items = viewModel.doorStates
            .filter { it.event?.state != LatchState.CLOSED }
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
            ).takeIf { items.isNotEmpty() },
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
