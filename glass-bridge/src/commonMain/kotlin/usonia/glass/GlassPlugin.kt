package usonia.glass

import com.inkapplications.glassconsole.client.GlassClient
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class GlassPlugin(
    client: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        DisplayUpdater(
            client = client,
            composer = DisplayComposer(client, logger),
            glass = GlassClient(),
            logger = logger,
        ),
    )
}
