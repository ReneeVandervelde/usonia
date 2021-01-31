package usonia.web.events

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import usonia.foundation.DateMetricSerializer
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

class EventsByDaySocket(
    private val client: BackendClient,
    private val json: Json,
): WebSocketController {
    override val path: String = "/events/by-day"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        client.eventsByDay.collectLatest {
            output.send(json.encodeToString(DateMetricSerializer, it))
        }
    }
}
