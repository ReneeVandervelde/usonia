package usonia.todoist

import kimchi.logger.KimchiLogger
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.server.ServerPlugin
import usonia.server.cron.CronJob
import usonia.todoist.api.TodoistApiClient

class TodoistBridgePlugin(
    config: ConfigurationAccess,
    events: EventAccess,
    logger: KimchiLogger,
): ServerPlugin {
    private val api = TodoistApiClient()

    override val crons: List<CronJob> = listOf(
        AwolDeviceReporter(config, events, api, logger)
    )
}
