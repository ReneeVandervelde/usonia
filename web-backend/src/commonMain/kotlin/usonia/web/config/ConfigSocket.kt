package usonia.web.config

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

internal class ConfigSocket(
    private val client: BackendClient,
    private val json: Json,
): WebSocketController {
    override val path: String = "config"

    override suspend fun start(
        input: ReceiveChannel<String>,
        output: SendChannel<String>,
        parameters: Map<String, List<String>>,
    ) {
        client.site.collectLatest {
            output.send(json.encodeToString(it))
        }
    }
}
