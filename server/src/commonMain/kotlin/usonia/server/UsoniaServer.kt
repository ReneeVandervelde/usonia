package usonia.server

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import regolith.init.Initializer
import regolith.init.RegolithInitRunner
import regolith.init.TargetManager
import regolith.processes.cron.CoroutineCronDaemon
import regolith.processes.daemon.Daemon
import regolith.processes.daemon.DaemonCallbacks
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
    private val daemonScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
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
        callbacks = object: DaemonCallbacks by kimchiRegolith {
            override fun onPanic(daemon: Daemon, error: Throwable) {
                kimchiRegolith.onPanic(daemon, error)
                daemonScope.coroutineContext.job.cancel()
            }
        },
        daemonScope = daemonScope,
    )
    private val postPluginDaemonInitializer = object: Initializer by daemonInitializer {
        override suspend fun initialize(targetManager: TargetManager) {
            targetManager.awaitTarget(PluginInitTarget::class)
            daemonInitializer.initialize(targetManager)
        }
    }
    private val pluginInitializer = PluginInitializer(
        initializers = plugins.flatMap { it.initializers },
        initCallbacks = kimchiRegolith,
        initScope = daemonScope,
    )
    private val regolithInitRunner = RegolithInitRunner(
        initializers = listOf(pluginInitializer, postPluginDaemonInitializer),
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
        daemonScope.coroutineContext.job.join()
    }
}
