package usonia.glass

import com.inkapplications.glassconsole.client.pin.PinValidator
import inkapplications.spondee.spatial.GeoCoordinates
import inkapplications.spondee.spatial.latitude
import inkapplications.spondee.spatial.longitude
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import usonia.core.client.latestDeviceEventOfType
import usonia.core.state.booleanFlags
import usonia.foundation.*
import usonia.kotlin.*
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.weather.Forecast
import usonia.weather.LocalWeatherAccess
import usonia.weather.LocationWeatherAccess
import usonia.weather.combinedData
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes

data class ExpandedLocationForecast(
    val name: String,
    val forecasts: List<ExpandedForecast>,
) {
    data class ExpandedForecast(
        val title: String,
        val forecast: Forecast,
        val daytime: Boolean,
    )
}

internal class ViewModelFactory(
    private val client: BackendClient,
    private val challengeContainer: ChallengeContainer,
    private val timedArmSecurityController: TimedArmSecurityController,
    private val pinValidator: PinValidator,
    private val localWeatherAccess: LocalWeatherAccess,
    private val locationWeatherAccess: LocationWeatherAccess,
    private val clock: ZonedClock = ZonedSystemClock,
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
    private val localWeatherInfo = localWeatherAccess.combinedData.map { (conditions, forecast) ->
        DisplayViewModel.WeatherInfo(
            forecast = forecast,
            conditions = conditions
        )
    }
    private val expandedWeatherInfo: OngoingFlow<List<ExpandedLocationForecast>> = ongoingFlow {
        while(coroutineContext.isActive) {
            val locations = listOf(
                "Columbia Heights, MN" to GeoCoordinates(45.0491.latitude, (-93.2472).longitude),
                "Enderlin, ND" to GeoCoordinates(46.6077.latitude, (-97.6011).longitude),
                "Portage, WI" to GeoCoordinates(43.5393.latitude, (-89.4626).longitude),
                "Superior, WI" to GeoCoordinates(46.7208.latitude, (-92.1041).longitude),
            )
            val today = clock.todayIn(clock.timeZone)
            emit(locations.map { (name, location) ->
                ExpandedLocationForecast(
                    name = name,
                    forecasts = (0..5).map { day ->
                        val forecastDay = today + DatePeriod(days = day / 2)
                        val daytime = day % 2 == 0
                        val forecast = locationWeatherAccess.getWeatherForLocation(
                            location = location,
                            date = forecastDay,
                            daytime = daytime
                        ) ?: return@map null
                        ExpandedLocationForecast.ExpandedForecast(
                            title = when (forecastDay.dayOfWeek.value) {
                                1 -> "Mon"
                                2 -> "Tue"
                                3 -> "Wed"
                                4 -> "Thu"
                                5 -> "Fri"
                                6 -> "Sat"
                                7 -> "Sun"
                                else -> "--"
                            },
                            forecast = forecast,
                            daytime = daytime,
                        )
                    }.filterNotNull()
                )
            })
            delay(20.minutes)
        }
    }

    fun create(config: GlassPluginConfig): OngoingFlow<DisplayViewModel> {
        val securityInfo = combine(challenge(config), isArming) { challenge, isArming ->
            DisplayViewModel.SecurityInfo(
                challenge = challenge,
                isArming = isArming,
                armDelayMinutes = timedArmSecurityController.delay.inWholeMinutes.toInt()
            )
        }

        return combine(flags, doorStates, securityInfo, localWeatherInfo, expandedWeatherInfo) { flags, doors, security, weather, expandedWeather ->
            DisplayViewModel(
                config = config,
                flags = flags,
                doorStates = doors,
                security = security,
                weather = weather,
                expandedWeather = expandedWeather,
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
