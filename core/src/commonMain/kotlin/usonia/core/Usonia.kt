package usonia.core

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import usonia.core.timemachine.SecondFrequency
import usonia.core.timemachine.minutes

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

        val daemonJobs = startDaemons()
        val cronJobs = startCrons()

        cronJobs.joinAll()
        daemonJobs.joinAll()
    }

    private suspend fun startDaemons(): List<Job> {
        val daemons = plugins.flatMap { it.daemons }
        logger.debug("Starting ${daemons.size} daemons.")

        return daemons.map { daemon ->
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
    }

    private suspend fun startCrons(): List<Job> {
        val crons = plugins.flatMap { it.crons }
        logger.debug("Starting ${crons.size} Cron Jobs")

        return crons.map { cron ->
            logger.debug { "Starting Cron <${cron::class.simpleName}>"}
            daemonScope.launch {
                SecondFrequency.minutes
                    .filter { it.minute in cron.schedule.minutes }
                    .filter { it.hour in cron.schedule.hours }
                    .filter { it.dayOfMonth in cron.schedule.days }
                    .filter { it.monthNumber in cron.schedule.months }
                    .onEach { logger.debug { "Running Cron <${cron::class.simpleName}>"} }
                    .collect { cron.run(it) }
            }
        }
    }
}
