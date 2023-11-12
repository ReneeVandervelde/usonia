package usonia.server

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import regolith.init.RegolithInitRunner
import regolith.processes.cron.CoroutineCronDaemon
import regolith.processes.daemon.Daemon
import regolith.processes.daemon.DaemonInitializer
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock

/**
 * The "backend" part of the application that starts up long running services.
 */
class UsoniaServer(
    override val plugins: Set<ServerPlugin>,
    private val server: WebServer,
    private val logger: KimchiLogger = EmptyLogger,
    daemonScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    clock: ZonedClock = ZonedSystemClock,
): AppConfig, Daemon {
    private val kimchiRegolith = KimchiRegolithAdapter(logger)
    private val cronDaemon = CoroutineCronDaemon(
        jobs = plugins.flatMap { it.crons },
        clock = clock,
        zone = clock.timeZone,
    )
    private val daemonInitializer = DaemonInitializer(
        daemons = plugins.flatMap { it.daemons } + cronDaemon + this,
        callbacks = kimchiRegolith,
        daemonScope = daemonScope,
    )
    private val regolithInitRunner = RegolithInitRunner(
        initializers = plugins.flatMap { it.initializers } + daemonInitializer,
        callbacks = kimchiRegolith,
        initializerScope = daemonScope,
    )

    override suspend fun startDaemon(): Nothing {
        server.serve(this@UsoniaServer)
        throw IllegalStateException("Server stopped unexpectedly")
    }

    suspend fun start() {
        logger.info("Hello World! 👋")

        logger.debug("Loaded ${plugins.size} plugins.")
        plugins.forEach {
            logger.debug { " - ${it::class.simpleName}" }
        }

        regolithInitRunner.initialize().join()
        suspendCancellableCoroutine<Nothing> {  }
    }
}
