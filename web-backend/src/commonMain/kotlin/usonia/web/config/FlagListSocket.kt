package usonia.web.config

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.*
import usonia.core.state.ConfigurationAccess
import usonia.foundation.FlagSerializer
import usonia.server.http.*

class FlagListSocket(
    private val config: ConfigurationAccess,
    private val json: Json,
): WebSocketController {
    override val path: String = "/flags"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        config.flags.collectLatest {
            val serialized = json.encodeToString(FlagSerializer, it)
            output.send(serialized)
        }
    }
}

