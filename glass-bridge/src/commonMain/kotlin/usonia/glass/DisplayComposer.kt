package usonia.glass

import com.inkapplications.glassconsole.client.pin.NonceGenerator
import com.inkapplications.glassconsole.client.pin.PinValidator
import com.inkapplications.glassconsole.structures.*
import com.inkapplications.glassconsole.structures.Action
import com.inkapplications.glassconsole.structures.pin.Nonce
import ink.ui.structures.Positioning
import ink.ui.structures.Sentiment
import ink.ui.structures.TextStyle
import ink.ui.structures.elements.StatusIndicatorElement
import ink.ui.structures.elements.TextElement
import ink.ui.structures.elements.ThrobberElement
import ink.ui.structures.elements.UiElement
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
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
    private val challengeContainer: ChallengeContainer,
    private val pinValidator: PinValidator,
    private val nonceGenerator: NonceGenerator,
    private val timedArmSecurityController: TimedArmSecurityController,
    private val clock: Clock,
    private val logger: KimchiLogger,
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

    fun composeSmallPortrait(config: GlassPluginConfig): OngoingFlow<DisplayConfig> {
        return combine(
            flagControls(config.homeIp),
            doorStates
                .map { it.filter { it.event?.state != LatchState.CLOSED } }
                .toStatusElements(),
            client.securityState
                .distinctUntilChanged()
                .map { if (it == SecurityState.Armed) challengeContainer.issue() else null },
            securityRow(config),
        ) { flags, doors, challenge, security ->
            if (challenge != null) pinpadConfig(config, challenge)
            else DisplayConfig(
                items = listOfNotNull(
                    *flags,
                    *security,
                    TextElement(
                        text = "Doors",
                        style = TextStyle.H1,
                    ).asDisplayItem(
                        span = 2
                    ).takeIf { doors.isNotEmpty() },
                    *doors.asDisplayItems(),
                ),
                layout = LayoutType.VerticalGrid(2),
                expiration = UPDATE_RATE + UPDATE_GRACE,
            )

        }
    }

    fun composeLargePortrait(config: GlassPluginConfig): OngoingFlow<DisplayConfig> {
        return combine(
            flagControls(config.homeIp),
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

    private fun securityRow(
        config: GlassPluginConfig,
    ): OngoingFlow<Array<out DisplayItem>> {
        return timedArmSecurityController.isActive
            .asOngoing()
            .map { arming ->
                if (arming) {
                    arrayOf(
                        ThrobberElement(
                            caption = "Arming Security in 5 minutes."
                        ).asDisplayItem(
                            position = Positioning.Center,
                            span = 2
                        ),
                        ButtonItem(
                            text = "Cancel",
                            action = Action.Put("http://${config.homeIp}/glass/arm", "false"),
                            position = Positioning.Center,
                            latching = true,
                            sentiment = Sentiment.Nominal,
                            span = 2,
                        )
                    )
                } else {
                    arrayOf(
                        ButtonItem(
                            text = "Lock",
                            action = Action.Put("http://${config.homeIp}/glass/arm", "true"),
                            position = Positioning.Center,
                            latching = true,
                            sentiment = Sentiment.Caution,
                            span = 2
                        )
                    )
                }
            }
    }

    private fun pinpadConfig(
        config: GlassPluginConfig,
        challenge: Nonce,
    ): DisplayConfig {
        val now = clock.now()
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
                    witness = pinValidator.digest(
                        psk = config.psk,
                        pin = config.pin,
                        timestamp = now,
                        nonce = nonceGenerator.generateNonce(),
                    ),
                    challengeNonce = challenge,
                    callbackUrl = "http://${config.homeIp}/glass/${config.bridgeId.value}/disarm",
                )
            ),
            expiration = UPDATE_RATE + UPDATE_GRACE,
        )
    }

    private fun Array<out UiElement.Static>.asDisplayItems(
        position: Positioning = Positioning.Start,
        span: Int = 1,
    ) = map {
        it.asDisplayItem(position, span)
    }.toTypedArray()

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
