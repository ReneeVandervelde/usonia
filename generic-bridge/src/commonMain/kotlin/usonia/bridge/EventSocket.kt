package usonia.bridge

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import usonia.core.server.WebSocketController
import usonia.serialization.EventSerializer
import usonia.state.EventAccess

/**
 * Socket that outputs all event data.
 */
internal class EventSocket(
    private val eventAccess: EventAccess,
    private val logger: KimchiLogger = EmptyLogger
): WebSocketController {
    override val path: String = "/events"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        eventAccess.events.collect {
            try {
                val data = Json.encodeToString(EventSerializer, it)
                output.send(data)
            } catch (error: Throwable) {
                logger.error("Failed to encode event to socket: $it", error)
            }
        }
    }
}
