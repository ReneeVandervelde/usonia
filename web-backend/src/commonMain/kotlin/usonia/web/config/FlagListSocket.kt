package usonia.web.config

import com.inkapplications.coroutines.ongoing.collectLatest
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.FlagSerializer
import usonia.server.http.WebSocketController

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

