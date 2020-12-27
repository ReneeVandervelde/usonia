package usonia.todoist

import kimchi.logger.KimchiLogger
import usonia.core.ServerPlugin
import usonia.core.cron.CronJob
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
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
