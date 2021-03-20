package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.json.Json
import usonia.foundation.EventSerializer
import usonia.kotlin.collect
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

/**
 * Socket that outputs all event data.
 */
internal class EventSocket(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path: String = "/events"

    override suspend fun start(
        input: ReceiveChannel<String>,
        output: SendChannel<String>,
        parameters: Map<String, List<String>>,
    ) {
        client.events.collect {
            try {
                output.offer(json.encodeToString(EventSerializer, it))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                logger.error("Failed to encode event to socket: $it", error)
            }
        }
    }
}
