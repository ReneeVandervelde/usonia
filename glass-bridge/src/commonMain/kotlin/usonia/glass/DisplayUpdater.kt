package usonia.glass

import com.inkapplications.coroutines.ongoing.*
import com.inkapplications.glassconsole.client.HttpException
import com.inkapplications.glassconsole.client.remote.GlassHttpClient
import com.inkapplications.glassconsole.structures.pin.Pin
import com.inkapplications.glassconsole.structures.pin.Psk
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.merge
import regolith.processes.daemon.Daemon
import regolith.timemachine.InexactDurationMachine
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.server.client.BackendClient
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

internal val UPDATE_RATE = 30.minutes
internal val UPDATE_GRACE = 5.minutes

internal class DisplayUpdater(
    private val client: BackendClient,
    private val viewModelFactory: ViewModelFactory,
    private val composer: DisplayConfigFactory,
    private val glassClient: GlassHttpClient,
    private val logger: KimchiLogger = EmptyLogger,
    clock: ZonedClock = ZonedSystemClock,
): Daemon {
    private val updateTicks = InexactDurationMachine(UPDATE_RATE, clock).ticks.asOngoing()

    override suspend fun startDaemon(): Nothing {
        client.site
            .map { it.bridges.filter { it.service == "glass" } }
            .flatMapLatest { bridges ->
                bridges.map { bridge ->
                    val homeIp = bridge.parameters["homeIp"] ?: return@map null.also {
                        logger.warn("No Home IP set for bridge: ${bridge.id}")
                    }
                    val psk = bridge.parameters["psk"]?.let(::Psk) ?: return@map null.also {
                        logger.warn("No PSK set for bridge: ${bridge.id}")
                    }
                    val pin = bridge.parameters["pin"]?.let(::Pin) ?: return@map null.also {
                        logger.warn("No PIN set for bridge: ${bridge.id}")
                    }
                    val type = bridge.parameters["type"]
                        ?.let { runCatching { GlassPluginConfig.DisplayType.valueOf(it) }.getOrNull() }
                        ?: return@map null.also {
                            logger.warn("No Display Type set for bridge: ${bridge.id}")
                        }
                    val deviceIp = bridge.parameters["deviceIp"] ?: return@map null.also {
                        logger.warn("No Device IP set for bridge: ${bridge.id}")
                    }
                    val sleepMode = bridge.parameters["mode_sleep"]
                        ?.let {
                            runCatching { GlassPluginConfig.DisplayMode.valueOf(it) }
                                .onFailure { logger.error("Illegal Display Mode", it) }
                                .getOrNull()
                        }
                        ?: GlassPluginConfig.DisplayMode.Normal
                    val movieMode = bridge.parameters["mode_movie"]
                        ?.let {
                            runCatching { GlassPluginConfig.DisplayMode.valueOf(it) }
                                .onFailure { logger.error("Illegal Display Mode", it) }
                                .getOrNull()
                        }
                        ?: GlassPluginConfig.DisplayMode.Normal
                    val pluginConfig = GlassPluginConfig(
                        bridgeId = bridge.id,
                        homeIp = homeIp,
                        deviceIp = deviceIp,
                        psk = psk,
                        pin = pin,
                        type = type,
                        sleepMode = sleepMode,
                        movieMode = movieMode,
                    )
                    val update = viewModelFactory.create(pluginConfig)
                        .map { composer.compose(it) }

                    combine(update, updateTicks) { displayConfig, _ ->
                        UpdateCommand(pluginConfig, displayConfig)
                    }.asFlow()
                }.filterNotNull().merge()
            }
            .collect {
                try {
                    logger.trace("Updating display: ${it.pluginConfig.deviceIp} with config: ${it.displayConfig}")
                    glassClient.updateDisplay(it.displayConfig, it.pluginConfig.deviceIp)
                } catch (e: CancellationException) {
                    logger.warn("Display update canceled", e)
                    throw e
                } catch (e: HttpException) {
                    logger.error("HTTP Request to display failed with code: ${e.statusCode}", e)
                } catch (e: IOException) {
                    logger.warn("Display update request failed", e)
                } catch (error: Throwable) {
                    logger.error(error) { "Failed to update glass display: ${error.message}" }
                }
            }
    }
}
