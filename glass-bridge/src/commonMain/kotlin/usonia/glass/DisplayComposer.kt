package usonia.glass

import com.inkapplications.glassconsole.structures.*
import com.inkapplications.glassconsole.structures.Action
import ink.ui.structures.Positioning
import ink.ui.structures.Sentiment
import ink.ui.structures.TextStyle
import ink.ui.structures.elements.StatusIndicatorElement
import ink.ui.structures.elements.TextElement
import ink.ui.structures.elements.UiElement
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.client.DeviceProperty
import usonia.core.client.latestDeviceEventOfType
import usonia.core.state.booleanFlags
import usonia.foundation.*
import usonia.kotlin.*
import usonia.kotlin.map
import usonia.rules.Flags
import usonia.server.client.BackendClient

internal class DisplayComposer(
    private val client: BackendClient,
    val logger: KimchiLogger,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    private fun flagControls(homeIp: String) = combine(
        client.booleanFlags.map { it[Flags.SleepMode] ?: false },
        client.booleanFlags.map { it[Flags.MovieMode] ?: false },
    ) { sleep, movie ->
        val sleepButton = ButtonItem(
            text = "Sleep Mode",
            action = Action.Put("http://$homeIp/flags/${Flags.SleepMode}", json.encodeToString(sleep.not().toString())),
            latching = true,
            sentiment = if (sleep) Sentiment.Primary else Sentiment.Idle,
            position = Positioning.Center,
        )
        val movieButton = ButtonItem(
            text = "Movie Mode",
            action = Action.Put("http://$homeIp/flags/${Flags.MovieMode}", json.encodeToString(movie.not().toString())),
            latching = true,
            sentiment = if (movie) Sentiment.Primary else Sentiment.Idle,
            position = Positioning.Center,
        )

        arrayOf(sleepButton, movieButton)
    }.onEach { logger.debug { "New Flags: ${it.contentToString()}" } }

    private val doorStates = client.site
        .map { it.devices.entryPoints.latchableDevices }
        .flatMapLatest { devices ->
            devices.map { client.latestDeviceEventOfType<Event.Latch>(it).asFlow() }
                .let { if (it.isEmpty()) flowOf(emptyList()) else combine(*it.toTypedArray()) { it.toList() } }
        }

    fun composeSmallPortrait(
        homeIp: String
    ): OngoingFlow<DisplayConfig> {
        return combine(
            flagControls(homeIp),
            doorStates
                .map { it.filter { it.event?.state != LatchState.CLOSED } }
                .toStatusElements(),
        ) { flags, doors ->
            DisplayConfig(
                items = listOf(
                    TextElement(
                        text = "Flags",
                        style = TextStyle.H1,
                    ).asDisplayItem(
                        span = 2,
                    ),
                    *flags,
                    TextElement(
                        text = "Doors",
                        style = TextStyle.H1,
                    ).asDisplayItem(
                        span = 2
                    ).takeIf { doors.isNotEmpty() },
                    *doors.asDisplayItems()
                ).filterNotNull(),
                layout = LayoutType.VerticalGrid(2),
                expiration = UPDATE_RATE + UPDATE_GRACE,
            )
        }
    }

    fun Array<out UiElement.Static>.asDisplayItems(
        position: Positioning = Positioning.Start,
        span: Int = 1,
    ) = map {
        it.asDisplayItem(position, span)
    }.toTypedArray()

    fun composeLargePortrait(
        homeIp: String,
    ): OngoingFlow<DisplayConfig> {
        return combine(
            flagControls(homeIp),
            doorStates.toStatusElements(),
        ) { flags, doors ->
            DisplayConfig(
                items = listOf(
                    TextElement(
                        text = "Flags",
                        style = TextStyle.H1,
                    ).asDisplayItem(
                        span = 3,
                        position = Positioning.Center
                    ),
                    *flags,
                    TextElement(
                        text = "Doors",
                        style = TextStyle.H1,
                    ).asDisplayItem(
                        span = 3,
                        position = Positioning.Center
                    ),
                    *doors.asDisplayItems()
                ),
                layout = LayoutType.VerticalGrid(3),
                expiration = UPDATE_RATE + UPDATE_GRACE,
            )
        }
    }

    private fun OngoingFlow<List<DeviceProperty<Event.Latch?>>>.toStatusElements() = map {
        it.map { (device, event) ->
            StatusIndicatorElement(
                text = device.name,
                sentiment = when (event?.state) {
                    LatchState.OPEN -> Sentiment.Caution
                    LatchState.CLOSED -> Sentiment.Positive
                    null -> Sentiment.Idle
                },
            )
        }.toTypedArray()
    }
}
