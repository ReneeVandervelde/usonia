package usonia.plugins

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*

class DaemonManager(
    private val scope: CoroutineScope,
    private val logger: KimchiLogger = EmptyLogger
) {
    fun run(daemons: List<Daemon>): Job {
        val job = SupervisorJob()
        logger.debug("Starting ${daemons.size} daemons.")

        daemons.forEach { daemon ->
            scope.launch(job) {
                logger.info { "Starting Daemon <${daemon::class.simpleName}>" }
                daemon.start()
                logger.warn { "Daemon <${daemon::class.simpleName}> has stopped. Re-starting." }
            }
        }

        return job
    }
}
