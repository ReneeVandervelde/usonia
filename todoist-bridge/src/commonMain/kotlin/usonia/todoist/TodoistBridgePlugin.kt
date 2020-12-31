package usonia.todoist

import kimchi.logger.KimchiLogger
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.cron.CronJob
import usonia.todoist.api.TodoistApiClient

class TodoistBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    private val api = TodoistApiClient()

    override val crons: List<CronJob> = listOf(
        AwolDeviceReporter(client, api, logger)
    )
}
