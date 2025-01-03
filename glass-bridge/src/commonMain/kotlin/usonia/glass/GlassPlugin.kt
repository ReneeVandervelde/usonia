package usonia.glass

import com.inkapplications.datetime.ZonedClock
import com.inkapplications.glassconsole.client.GlassClientModule
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import regolith.init.Initializer
import regolith.processes.daemon.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.http.HttpController
import usonia.weather.LocalWeatherAccess
import usonia.weather.LocationWeatherAccess

class GlassPlugin(
    client: BackendClient,
    weatherAccess: LocalWeatherAccess,
    locationWeatherAccess: LocationWeatherAccess,
    logger: KimchiLogger,
    json: Json,
    clock: ZonedClock,
): ServerPlugin {
    private val nonceGenerator = GlassClientModule.createNonceGenerator()
    private val challengeContainer = ChallengeContainer(nonceGenerator)
    private val pinValidator = GlassClientModule.createPinValidator()
    private val timedArmSecurityController = TimedArmSecurityController(
        config = client,
        json = json,
        logger = logger,
    )
    private val disarmController = DisarmSecurityController(
        backendClient = client,
        pinValidator = pinValidator,
        challengeContainer = challengeContainer,
        json = json,
        logger = logger,
    )

    override val daemons: List<Daemon> = listOf(
        DisplayUpdater(
            client = client,
            composer = DisplayConfigFactory(logger),
            viewModelFactory = ViewModelFactory(
                client = client,
                localWeatherAccess = weatherAccess,
                challengeContainer = challengeContainer,
                timedArmSecurityController = timedArmSecurityController,
                pinValidator = pinValidator,
                clock = clock,
                locationWeatherAccess = locationWeatherAccess,
            ),
            glassClient = GlassClientModule.createHttpClient(),
            logger = logger,
        ),
    )

    override val initializers: List<Initializer> = listOf(
        GlassClientModule.createInitializer()
    )

    override val httpControllers: List<HttpController> = listOf(
        disarmController,
        timedArmSecurityController,
    )
}
