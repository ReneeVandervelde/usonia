package usonia.notion

import com.inkapplications.standard.throwCancels
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import usonia.core.state.findBridgeByServiceTag
import usonia.core.state.findDevicesBy
import usonia.core.state.getOldestEvent
import usonia.foundation.*
import usonia.foundation.unit.compareTo
import usonia.kotlin.*
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.notion.api.NotionApi
import usonia.notion.api.structures.NotionBearerToken
import usonia.notion.api.structures.Parent
import usonia.notion.api.structures.block.Block
import usonia.notion.api.structures.block.BlockArgument
import usonia.notion.api.structures.database.DatabaseId
import usonia.notion.api.structures.database.DatabaseQuery
import usonia.notion.api.structures.page.*
import usonia.notion.api.structures.property.*
import usonia.notion.api.structures.property.MultiSelectArgument
import usonia.notion.api.structures.property.Property
import usonia.notion.api.structures.property.PropertyArgument
import usonia.notion.api.structures.property.SelectArgument
import usonia.notion.api.structures.property.StatusArgument
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class AwolDeviceReporter(
    private val notionClient: NotionApi,
    private val backendClient: BackendClient,
    private val clock: ZonedClock = ZonedSystemClock,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob, Daemon {
    override val schedule: Schedule = Schedule().withMinutes { it % 20 == 0 }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val bridge = backendClient.findBridgeByServiceTag(NotionConfig.SERVICE) ?: run {
            logger.warn("Notion not configured. Configure a bridge for the service `${NotionConfig.SERVICE}`")
            return
        }
        val token = bridge.parameters[NotionConfig.TOKEN]?.let(::NotionBearerToken) ?: run {
            logger.warn("Notion token not set. Configure `token` parameter for `${NotionConfig.TOKEN}")
            return
        }
        val database = bridge.parameters[NotionConfig.DATABASE]?.let { DatabaseId(it) } ?: run {
            logger.warn("Notion database not set. Configure `database` parameter for `${NotionConfig.DATABASE}`")
            return
        }
        val timeInstant = time.toInstant(zone)
        val devices = backendClient.findDevicesBy { it.capabilities.heartbeat != null }
            .also { logger.debug("Checking ${it.size} device heartbeats") }

        val deviceHeartbeats = devices.map { device ->
            device to getHeartbeatEvent(timeInstant, device)
        }

        val awol = deviceHeartbeats
            .filter { (device, event) -> !isRecent(timeInstant, event, device) }
            .also { logger.debug("${it.size} devices are AWOL") }

        val reports = runRetryable(
            strategy = RetryStrategy.Bracket(
                attempts = 5,
                timeouts = listOf(500.milliseconds, 5.seconds, 10.seconds)
            ),
            onError = { logger.error("Error querying notion database", it) }
        ) {
            notionClient.queryDatabase(
                token = token,
                database = database,
                query = DatabaseQuery(
                    filter = PageFilter.And(
                        filters = listOf(
                            PageFilter.Text(
                                property = NotionConfig.Properties.REF,
                                filter = TextFilter.Empty(false),
                            ),
                            PageFilter.Status(
                                property = NotionConfig.Properties.STATUS,
                                filter = FilterQuery.DoesNotEqual(
                                    value = NotionConfig.PropertyValues.STATUS_DONE
                                )
                            ),
                            PageFilter.Or(
                                filters = listOf(
                                    PageFilter.MultiSelect(
                                        property = NotionConfig.Properties.TAGS,
                                        filter = FilterQuery.Contains(
                                            contains = NotionConfig.Tags.LOW_BATTERY
                                        )
                                    ),
                                    PageFilter.MultiSelect(
                                        property = NotionConfig.Properties.TAGS,
                                        filter = FilterQuery.Contains(
                                            contains = NotionConfig.Tags.DEAD_BATTERY
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ).results
        }.getOrNull() ?: return
        val new = awol
            .filter { (device, _) -> device.id !in reports.map { it.device } }
            .map { (device, _) -> device}
            .also { logger.debug("${it.size} devices are new reports") }
        val upgraded = awol
            .map { (device, _) -> reports.filter { it.isWarning }.find { it.device == device.id } }
            .filterNotNull()
            .also { logger.debug("${it.size} devices are upgraded reports") }
        val found = reports
            .filter { !it.isWarning }
            .filter { it.device !in awol.map { (device, _) -> device.id } }
            .filter { it.device in deviceHeartbeats.filter { (_, event) -> event != null }.map { it.first.id } }
            .also { logger.debug("${it.size} devices have been found") }
        val saved = reports
            .filter { it.isWarning }
            .filter { backendClient.getState(it.device!!, Event.Battery::class)?.percentage?.let { it > 80.percent } ?: false }
            .also { logger.debug("${it.size} devices are no longer low-battery.") }
        val retryStrategy = RetryStrategy.Bracket(
            attempts = 5,
            timeouts = listOf(500.milliseconds, 5.seconds, 10.seconds)
        )
        val timeout = 30.seconds

        new.forEach { device ->
            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error attempting to create AWOL task", error) },
            ) {
                newBatteryTicket(token, database, "Replace batteries in ${device.name}", device.id, NotionConfig.Tags.DEAD_BATTERY)
            }.onSuccess {
                logger.info("Created AWOL Task for device: ${device.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to create AWOL task", error)
            }
        }
        upgraded.forEach { report ->
            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error updating AWOL task", error) },
            ) {
                notionClient.updatePage(token, report.id, mapOf(
                    NotionConfig.Properties.TAGS to PropertyArgument.MultiSelect(
                        multi_select = listOf(
                            MultiSelectArgument(
                                name = NotionConfig.Tags.DEAD_BATTERY
                            )
                        )
                    )
                ))
            }.onSuccess {
                logger.info("Updated AWOL Task: ${report.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to update AWOL task", error)
            }
        }
        (saved + found).forEach { task ->
            runRetryable(
                strategy = retryStrategy,
                attemptTimeout = timeout,
                onError = { error -> logger.warn("Error updating AWOL task", error) },
            ) {
                notionClient.updatePage(token, task.id, mapOf(
                    NotionConfig.Properties.STATUS to PropertyArgument.Status(
                        status = StatusArgument(
                            name = NotionConfig.PropertyValues.STATUS_DONE,
                        ),
                    ),
                ))
            }.onSuccess {
                logger.info("Closed Task: ${task.id}")
            }.throwCancels().onFailure { error ->
                logger.error("Unable to close task: ${task.id}", error)
            }
        }
    }

    override suspend fun startDaemon(): Nothing {
        backendClient.events
            .filterIsInstance<Event.Battery>()
            .filter { it.percentage < 20.percent }
            .combineToPair(backendClient.site)
            .collectLatest { (event, site) -> reportLowBattery(site, event) }
    }

    private suspend fun reportLowBattery(site: Site, event: Event.Battery) {
        val device = site.findDevice(event.source) ?: run {
            logger.error("Unable to find device for low battery event: ${event.source}")
            return
        }
        val bridge = backendClient.findBridgeByServiceTag(NotionConfig.SERVICE) ?: run {
            logger.warn("Notion not configured. Configure a bridge for the service `${NotionConfig.SERVICE}`")
            return
        }
        val token = bridge.parameters[NotionConfig.TOKEN]?.let(::NotionBearerToken) ?: run {
            logger.warn("Notion token not set. Configure `token` parameter for `${NotionConfig.TOKEN}")
            return
        }
        val database = bridge.parameters[NotionConfig.DATABASE]?.let { DatabaseId(it) } ?: run {
            logger.warn("Notion database not set. Configure `database` parameter for `${NotionConfig.DATABASE}`")
            return
        }

        runRetryable(
            strategy = RetryStrategy.Bracket(
                attempts = 10,
                timeouts = listOf(500.milliseconds, 5.seconds, 30.seconds, 5.minutes)
            ),
            attemptTimeout = 60.seconds,
            onError = { error -> logger.warn("Error attempting to create low battery Ticket", error) },
        ) {
            openBatteryTicket(
                token = token,
                database = database,
                content = "Low Battery: ${device.name}",
                ref = event.source,
                tag = NotionConfig.Tags.LOW_BATTERY,
            )
        }.onSuccess {
            logger.info("Created/Updated Low Battery Ticket for ${device.name}")
        }.throwCancels().onFailure { error ->
            logger.error("Unable to create low battery Ticket", error)
        }
    }

    private suspend fun openBatteryTicket(
        token: NotionBearerToken,
        database: DatabaseId,
        content: String,
        ref: Identifier,
        tag: String,
    ) {
        val existing = notionClient.queryDatabase(
            token = token,
            database = database,
            query = DatabaseQuery(
                filter = PageFilter.Text(
                    property = NotionConfig.Properties.REF,
                    filter = TextFilter.Equals(
                        equals = ref.value
                    )
                )
            )
        )

        if (existing.results.isEmpty()) {
            newBatteryTicket(token, database, content, ref, tag)
        } else {
            logger.debug("Battery ticket already exists for ${ref.value}")
        }
    }

    private suspend fun newBatteryTicket(
        token: NotionBearerToken,
        database: DatabaseId,
        content: String,
        ref: Identifier,
        tag: String
    ) {
        logger.debug("Creating new battery ticket for ${ref.value}")
        notionClient.createPage(
            token = token,
            page = NewPage(
                parent = Parent.Database(database),
                properties = mapOf(
                    NotionConfig.Properties.TITLE to PropertyArgument.Title(
                        title = listOf(
                            BlockArgument.RichText(
                                text = BlockArgument.RichText.Text(
                                    content = content,
                                )
                            )
                        )
                    ),
                    NotionConfig.Properties.REF to PropertyArgument.RichText(
                        rich_text = listOf(
                            BlockArgument.RichText(
                                text = BlockArgument.RichText.Text(
                                    content = ref.value,
                                )
                            )
                        )
                    ),
                    NotionConfig.Properties.TAGS to PropertyArgument.MultiSelect(
                        multi_select = listOf(
                            MultiSelectArgument(
                                name = tag,
                            )
                        )
                    ),
                    NotionConfig.Properties.IMPACT to PropertyArgument.Select(
                        select = SelectArgument(
                            name = NotionConfig.ImpactValues.MEDIUM
                        )
                    ),
                    NotionConfig.Properties.URGENCY to PropertyArgument.Select(
                        select = SelectArgument(
                            name = NotionConfig.UrgencyValues.HIGH
                        )
                    )
                )
            )
        )
    }

    private suspend fun isRecent(
        time: Instant,
        event: Event?,
        device: Device
    ): Boolean {
        val duration = device.capabilities.heartbeat ?: return true
        if (event == null) {
            val oldest = backendClient.getOldestEvent() ?: return true
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
            backendClient.getState(device.id, type).also {
                val timestamp = it?.timestamp

                if (timestamp != null && timestamp > time - threshold) {
                    return it
                }
            }
        }

        return events.lastOrNull()
    }

    private val Page.isWarning: Boolean get() {
        return properties[NotionConfig.Properties.TAGS]
            ?.let { it as? Property.MultiSelect }
            ?.multi_select
            ?.any { it.name == NotionConfig.Tags.LOW_BATTERY }
            ?: false
    }

    private val Page.device: Identifier? get() {
        return properties[NotionConfig.Properties.REF]
            ?.let { it as? Property.RichText }
            ?.rich_text
            ?.first()
            ?.let { it as? Block.RichText }
            ?.plain_text
            ?.let { Identifier(it) }
    }
}
