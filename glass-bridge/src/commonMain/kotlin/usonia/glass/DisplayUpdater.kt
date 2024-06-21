package usonia.glass

import com.inkapplications.glassconsole.client.GlassClient
import com.inkapplications.glassconsole.client.HttpException
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import regolith.processes.daemon.Daemon
import regolith.timemachine.InexactDurationMachine
import usonia.kotlin.collect
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.flatMapLatest
import usonia.kotlin.map
import usonia.server.client.BackendClient
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes

internal val UPDATE_RATE = 30.minutes
internal val UPDATE_GRACE = 5.minutes

internal class DisplayUpdater(
    private val client: BackendClient,
    private val composer: DisplayComposer,
    private val glass: GlassClient,
    private val logger: KimchiLogger = EmptyLogger,
    clock: ZonedClock = ZonedSystemClock,
): Daemon {
    private val updateTicks = InexactDurationMachine(UPDATE_RATE, clock).ticks

    override suspend fun startDaemon(): Nothing {
        client.site
            .map { it.bridges.filter { it.service == "glass" } }
            .flatMapLatest { bridges ->
                bridges.map { bridge ->
                    val homeIp = bridge.parameters["homeIp"] ?: return@map null
                    when (bridge.parameters["type"]) {
                        "large" -> composer.composeLargePortrait(homeIp)
                        else -> composer.composeSmallPortrait(homeIp)
                    }.asFlow().combine(updateTicks) { config, _ -> UpdateCommand(bridge, config) }
                }.filterNotNull().merge()
            }
            .collect {
                try {
                    glass.updateDisplay(it.config, it.bridge.parameters["deviceIp"]!!)
                } catch (e: CancellationException) {
                    logger.warn("Display update canceled", e)
                    throw e
                } catch (e: HttpException) {
                    logger.error("HTTP Request to display failed with code: ${e.statusCode}")
                } catch (e: IOException) {
                    logger.warn("Display update request failed", e)
                } catch (error: Throwable) {
                    logger.error(error) { "Failed to update glass display: ${error.message}" }
                }
            }
    }
}
