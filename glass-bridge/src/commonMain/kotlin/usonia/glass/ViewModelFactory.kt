package usonia.glass

import com.inkapplications.glassconsole.client.pin.PinValidator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import usonia.core.client.latestDeviceEventOfType
import usonia.core.state.booleanFlags
import usonia.foundation.*
import usonia.kotlin.*
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.weather.WeatherAccess
import usonia.weather.combinedData

internal class ViewModelFactory(
    private val client: BackendClient,
    private val challengeContainer: ChallengeContainer,
    private val timedArmSecurityController: TimedArmSecurityController,
    private val pinValidator: PinValidator,
    private val weatherAccess: WeatherAccess,
    private val clock: Clock = Clock.System,
) {
    private val sleepMode = client.booleanFlags.map { it[Flags.SleepMode] ?: false }
    private val movieMode = client.booleanFlags.map { it[Flags.MovieMode] ?: false }
    private val flags = combine(sleepMode, movieMode) { sleep, movie -> DisplayViewModel.Flags(sleep, movie) }
    private val doorStates = client.site
        .map { it.devices.entryPoints.latchableDevices }
        .flatMapLatest { devices ->
            devices.map { client.latestDeviceEventOfType<Event.Latch>(it).asFlow() }
                .let { if (it.isEmpty()) flowOf(emptyList()) else combine(*it.toTypedArray()) { it.toList() } }
        }
    private val isArming = timedArmSecurityController.isActive.asOngoing()
    private val weatherInfo = weatherAccess.combinedData.map { (conditions, forecast) ->
        DisplayViewModel.WeatherInfo(
            forecast = forecast,
            conditions = conditions
        )
    }

    fun create(config: GlassPluginConfig): OngoingFlow<DisplayViewModel> {
        val securityInfo = combine(challenge(config), isArming) { challenge, isArming ->
            DisplayViewModel.SecurityInfo(
                challenge = challenge,
                isArming = isArming,
                armDelayMinutes = timedArmSecurityController.delay.inWholeMinutes.toInt()
            )
        }

        return combine(flags, doorStates, securityInfo, weatherInfo) { flags, doors, security, weather ->
            DisplayViewModel(
                config = config,
                flags = flags,
                doorStates = doors,
                security = security,
                weather = weather,
            )
        }
    }

    private fun challenge(config: GlassPluginConfig) = client.securityState
        .distinctUntilChanged()
        .map {
            if (it != SecurityState.Armed) null else {
                DisplayViewModel.ChallengeData(
                    nonce = challengeContainer.issue(),
                    witness = pinValidator.digest(
                        psk = config.psk,
                        pin = config.pin,
                        timestamp = clock.now(),
                        nonce = challengeContainer.issue(),
                    )
                )
            }
        }
}
