package usonia.todoist

import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.findBridgeByServiceTag
import usonia.core.state.findDevicesBy
import usonia.core.state.getOldestEvent
import usonia.foundation.*
import usonia.foundation.unit.compareTo
import usonia.kotlin.*
import usonia.kotlin.datetime.ZonedDateTime
import usonia.server.Daemon
import usonia.server.client.BackendClient
import usonia.server.cron.CronJob
import usonia.server.cron.Schedule
import usonia.todoist.api.Task
import usonia.todoist.api.TaskCreateParameters
import usonia.todoist.api.TaskUpdateParameters
import usonia.todoist.api.TodoistApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TODOIST_SERVICE = "todoist"
private const val TODOIST_TOKEN = "token"
private const val TODOIST_PROJECT = "project"
private const val TODOIST_LABEL = "label"

internal class AwolDeviceReporter(
    private val client: BackendClient,
    private val api: TodoistApi,
    private val logger: KimchiLogger = EmptyLogger,
    private val clock: Clock = Clock.System,
): CronJob, Daemon {
    override val schedule: Schedule = Schedule().withMinutes { it % 20 == 0 }

    override suspend fun start(): Nothing {
        client.events
            .filterIsInstance<Event.Battery>()
            .filter { it.percentage < 20.percent }
            .combineToPair(client.site)
            .collectLatest { (event, site) -> lowBatteries(site, event) }
    }

    override suspend fun runCron(time: ZonedDateTime) {
        val bridge = client.findBridgeByServiceTag(TODOIST_SERVICE) ?: run {
            logger.warn("Todoist not configured. Configure a bridge for the service `$TODOIST_SERVICE`")
            return
        }
        val token = bridge.parameters[TODOIST_TOKEN] ?: run {
            logger.warn("Todoist token not set. Configure `token` parameter for `$TODOIST_SERVICE`")
            return
        }
        val project = bridge.parameters[TODOIST_PROJECT]
        val label = bridge.parameters[TODOIST_LABEL]
        val timeInstant = time.instant

        val devices = client.findDevicesBy { it.capabilities.heartbeat != null }
            .also { logger.debug("Checking ${it.size} device heartbeats") }

        val deviceHeartbeats = devices.map { device ->
            device to getHeartbeatEvent(timeInstant, device)
        }

        val awol = deviceHeartbeats
            .filter { (device, event) -> !isRecent(timeInstant, event, device) }
            .also { logger.debug("${it.size} devices are AWOL") }

        val reports = api.getTasks(token, project, label)
            .filter { it.device != null }
        val new = awol
            .filter { (device, _) -> device.id !in reports.map { it.device } }
            .map { (device, _) -> device}
            .also { logger.debug("${it.size} devices are new reports") }
        val upgraded = awol
            .map { (device, _) -> device to reports.filter { it.isWarning }.find { it.device == device.id } }
            .filter { (_, report) -> report != null }
            .map { it as Pair<Device, Task> }
            .also { logger.debug("${it.size} devices are upgraded reports") }
        val found = reports
            .filter { !it.isWarning }
            .filter { it.device !in awol.map { (device, _) -> device.id } }
            .filter { it.device in deviceHeartbeats.filter { (_, event) -> event != null }.map { it.first.id } }
            .also { logger.debug("${it.size} devices have been found") }
        val saved = reports
            .filter { it.isWarning }
            .filter { client.getState(it.device!!, Event.Battery::class)?.percentage?.let { it > 80.percent } ?: false }
            .also { logger.debug("${it.size} devices are no longer low-battery.") }
        val retryStrategy = RetryStrategy.Bracket(
            attempts = 5,
            timeouts = listOf(500.milliseconds, 5.seconds, 10.seconds)
        )
        val timeout = 30.seconds

        new.forEach { device ->
            val parameters = TaskCreateParameters(
                content = device.awolContentString,
                projectId = project,
                labels = label?.let { listOf(it) },
                dueString = "Today",
                description = "(id: ${device.id.value})"
            )

            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error attempting to create AWOL task", error) },
            ) {
                api.create(token, parameters)
            }.onSuccess {
                logger.info("Created AWOL Task: ${it.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to create AWOL task", error)
            }
        }

        upgraded.forEach { (device, report) ->
            val parameters = TaskUpdateParameters(
                content = device.awolContentString,
            )

            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error updating AWOL task", error) },
            ) {
                api.update(token, report.id, parameters)
            }.onSuccess {
                logger.info("Updated AWOL Task: ${it.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to update AWOL task", error)
            }
        }

        (found + saved).forEach { task ->
            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error updating AWOL task", error) },
            ) {
                api.close(token, task.id)
            }.onSuccess {
                logger.info("Closed Task: ${task.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to close task: ${task.id}", error)
            }
        }
    }

    private suspend fun lowBatteries(site: Site, event: Event.Battery) {
        val device = site.findDevice(event.source) ?: run {
            logger.error("Unable to find device for low battery event: ${event.source}")
            return
        }
        val bridge = client.findBridgeByServiceTag(TODOIST_SERVICE) ?: run {
            logger.warn("Todoist not configured. Configure a bridge for the service `$TODOIST_SERVICE`")
            return
        }
        val token = bridge.parameters[TODOIST_TOKEN] ?: run {
            logger.warn("Todoist token not set. Configure `token` parameter for `$TODOIST_SERVICE`")
            return
        }
        val project = bridge.parameters[TODOIST_PROJECT]
        val label = bridge.parameters[TODOIST_LABEL]

        val parameters = TaskCreateParameters(
            content = "Low battery in ${device.name}",
            projectId = project,
            labels = label?.let { listOf(it) },
            dueString = "Today",
            description = "(id: ${device.id.value})"
        )

        runRetryable(
            strategy = RetryStrategy.Bracket(
                attempts = 10,
                timeouts = listOf(500.milliseconds, 5.seconds, 30.seconds, 5.minutes)
            ),
            attemptTimeout = 60.seconds,
            onError = { error -> logger.warn("Error attempting to create low battery task", error) },
        ) {
            api.create(token, parameters)
        }.onSuccess {
            logger.info("Created Low Battery Task: ${it.id}")
        }.throwCancels().onFailure { error ->
            logger.error("Unable to create low battery task", error)
        }
    }

    private suspend fun isRecent(
        time: Instant,
        event: Event?,
        device: Device
    ): Boolean {
        val duration = device.capabilities.heartbeat ?: return true
        if (event == null) {
            val oldest = client.getOldestEvent() ?: return true
            return oldest >= clock.now() - duration
        }

        return event.timestamp > time - duration
    }

    private suspend fun getHeartbeatEvent(
        time: Instant,
        device: Device,
    ): Event? {
        val threshold = device.capabilities.heartbeat ?: return null
        val events = device.capabilities.events.map { type ->
            client.getState(device.id, type).also {
                val timestamp = it?.timestamp

                if (timestamp != null && timestamp > time - threshold) {
                    return it
                }
            }
        }

        return events.lastOrNull()
    }

    /**
     * String used as the content for an AWOL device
     */
    private val Device.awolContentString get() = "Replace Batteries in $name"

    /**
     * Whether the task is for a low battery warning, as opposed to an AWOL device.
     */
    private val Task.isWarning: Boolean get() = content.startsWith("Low battery")

    /**
     * Extract a device ID from a task's description.
     */
    private val Task.device: Identifier? get() = Regex("""\(id: (.+)\)""")
        .find(description.orEmpty())
        ?.groupValues
        ?.get(1)
        ?.let { Identifier(it) }
}
