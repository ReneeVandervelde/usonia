package usonia.web.config

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.server.http.WebSocketController

internal class ConfigSocket(
    private val configAccess: ConfigurationAccess,
    private val json: Json,
): WebSocketController {
    override val path: String = "config"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        configAccess.site.collectLatest {
            output.send(json.encodeToString(it))
        }
    }
}
