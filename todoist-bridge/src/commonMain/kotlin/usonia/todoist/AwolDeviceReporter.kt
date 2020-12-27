package usonia.todoist

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import usonia.core.cron.CronJob
import usonia.core.cron.Schedule
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.getSite
import usonia.foundation.Device
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.todoist.api.Task
import usonia.todoist.api.TaskParameters
import usonia.todoist.api.TodoistApi
import kotlin.time.ExperimentalTime

private const val TODOIST_SERVICE = "todoist"

@OptIn(ExperimentalTime::class)
internal class AwolDeviceReporter(
    private val config: ConfigurationAccess,
    private val events: EventAccess,
    private val api: TodoistApi,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob {
    override val schedule: Schedule = Schedule().withMinutes { it % 20 == 0 }

    private fun isRecent(
        time: Instant,
        event: Event?,
        device: Device
    ): Boolean {
        if (event == null) return false
        val duration = device.capabilities.heartbeat ?: return true

        return event.timestamp > time - duration
    }

    private suspend fun getHeartbeatEvent(
        time: Instant,
        device: Device,
    ): Event? {
        val threshold = device.capabilities.heartbeat ?: return null
        val events = device.capabilities.events.map { type ->
            events.getState(device.id, type).also {
                val timestamp = it?.timestamp

                if (timestamp != null && timestamp > time - threshold) {
                    return it
                }
            }
        }

        return events.lastOrNull()
    }

    override suspend fun run(time: LocalDateTime, timeZone: TimeZone) {
        val bridge = config.getSite().bridges.singleOrNull {
            it.service == TODOIST_SERVICE
        } ?: run {
            logger.warn("Todoist not configured. Configure a bridge for the service `$TODOIST_SERVICE`")
            return
        }
        val token = bridge.parameters["token"] ?: run {
            logger.warn("Todoist token not set. Configure `token` parameter for `$TODOIST_SERVICE`")
            return
        }
        val project = bridge.parameters["project"]?.toLong()
        val label = bridge.parameters["label"]?.toLong()
        val timeInstant = time.toInstant(timeZone)

        val devices = config.getSite()
            .rooms
            .flatMap { it.devices }
            .filter { it.capabilities.heartbeat != null }
            .also { logger.debug("Checking ${it.size} device heartbeats") }

        val deviceHeartbeats = devices.map { device ->
            device to getHeartbeatEvent(timeInstant, device)
        }

        val awol = deviceHeartbeats
            .filter { (device, event) -> !isRecent(timeInstant, event, device) }
            .also { logger.debug("${it.size} devices are AWOL") }

        val reports = api.getTasks(token, project, label)
        val new = awol
            .filter { (device, event) -> event != null }
            .map { (device, _) -> device}
            .also { logger.debug("${it.size} devices are new reports") }
        val found = reports
            .filter { it.device != null }// && it.device !in awol.map { it.id } }
            .filter { it.device !in awol.map { (device, _) -> device.id } }
            .also { logger.debug("${it.size} devices have been found") }

        new.forEach {
            val parameters = TaskParameters(
                content = "Replace Batteries in ${it.name} (id: ${it.id.value})",
                projectId = project,
                labels = label?.let { listOf(it) },
            )
            api.create(token, parameters)
        }

        found.forEach {
            api.close(token, it.id)
        }
    }

    private val Task.device: Identifier? get() = Regex("""\(id: (.+)\)""")
        .find(content)
        ?.groupValues
        ?.get(1)
        ?.let { Identifier(it) }
}
