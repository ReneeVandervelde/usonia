package usonia.core

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*

class Usonia(
    val plugins: Set<Plugin>,
    private val logger: KimchiLogger = EmptyLogger,
    private val daemonScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    suspend fun start() {
        logger.info("Hello World! 👋")

        logger.debug("Loaded ${plugins.size} plugins.")
        plugins.forEach {
            logger.debug { " - ${it::class.simpleName}" }
        }

        val daemons = plugins.flatMap { it.daemons }
        logger.debug("Starting ${daemons.size} daemons.")

        val daemonJobs = daemons.map { daemon ->
            daemonScope.launch {
                while (isActive) {
                    logger.debug { "Starting Daemon <${daemon::class.simpleName}>" }
                    try { daemon.start(this@Usonia) }
                    catch (error: Throwable) {
                        logger.error("Daemon <${daemon::class.simpleName}> has stopped. Re-starting.", error)
                        delay(500)
                    }
                }
            }
        }

        daemonJobs.joinAll()
    }
}
