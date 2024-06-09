package usonia.notion

import kimchi.logger.KimchiLogger
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.notion.api.NotionApiClient
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class NotionBridgePlugin(
    backendClient: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    private val notion = NotionApiClient()
    private val awolDeviceReporter = AwolDeviceReporter(
        notionClient = notion,
        backendClient = backendClient,
        logger = logger,
    )

    override val crons: List<CronJob> = listOf(awolDeviceReporter)
    override val daemons: List<Daemon> = listOf(awolDeviceReporter)
}
