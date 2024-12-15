package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.json.Json
import usonia.foundation.EventCategory
import usonia.foundation.Identifier
import usonia.kotlin.collectLatest
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

internal class EventCountSocket(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path = "/events/count/{device}/{type}"

    override suspend fun start(
        input: ReceiveChannel<String>,
        output: SendChannel<String>,
        parameters: Map<String, List<String>>
    ) {
        val deviceId = parameters["device"]
            ?.firstOrNull()
            ?.let(::Identifier)
            ?: run {
                logger.error("Invalid device provided in events request")
                return
            }

        val type = parameters["type"]
            ?.firstOrNull()
            ?.let { type ->
                runCatching { EventCategory.valueOf(type) }
                    .onFailure { logger.error("Invalid type provided in events request") }
                    .getOrNull()
            }
            ?: run {
                logger.error("No type provided in events request")
                return
            }

        client.eventCount(deviceId, type).collectLatest {
            output.send(it.toString())
        }
    }
}
