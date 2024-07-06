package usonia.web.config

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.kotlin.collectLatest
import usonia.server.http.WebSocketController

/**
 * Socket that emits changes in the site's security setting.
 */
class SecurityStateSocket(
    private val config: ConfigurationAccess,
    private val json: Json,
): WebSocketController {
    override val path: String = "/security"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        config.securityState.collectLatest {
            val serialized = json.encodeToString(it)
            output.send(serialized)
        }
    }
}
