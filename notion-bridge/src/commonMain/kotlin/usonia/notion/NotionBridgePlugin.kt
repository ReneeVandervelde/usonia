package usonia.notion

import kimchi.logger.KimchiLogger
import kimchi.logger.LogWriter
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.notion.api.NotionApiClient
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class NotionBridgePlugin(
    backendClient: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    private val awolDeviceReporter = AwolDeviceReporter(
        notionClient = notion,
        backendClient = backendClient,
        logger = logger,
    )

    override val crons: List<CronJob> = listOf(awolDeviceReporter)
    override val daemons: List<Daemon> = listOf(
        awolDeviceReporter,
        notionBugLogger.also { it.client.value = backendClient },
    )

    companion object
    {
        internal val notion = NotionApiClient()
        private val notionBugLogger = NotionBugLogger(notion)
        val logWriter: LogWriter = notionBugLogger
    }
}
