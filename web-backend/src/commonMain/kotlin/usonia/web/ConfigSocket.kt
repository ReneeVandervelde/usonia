package usonia.web

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import usonia.core.server.WebSocketController
import usonia.core.state.ConfigurationAccess
import usonia.serialization.SiteSerializer

internal class ConfigSocket(
    private val configAccess: ConfigurationAccess,
    private val siteSerializer: SiteSerializer,
): WebSocketController {
    override val path: String = "config"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        configAccess.site.collectLatest {
            output.send(Json.encodeToString(siteSerializer, it))
        }
    }
}
