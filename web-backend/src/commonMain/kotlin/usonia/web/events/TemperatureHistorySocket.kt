package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import usonia.foundation.Identifier
import usonia.foundation.RelativeHourMetricSerializer
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

class TemperatureHistorySocket(
    private val client: BackendClient,
    private val json: Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path: String = "/events/metric-temperature-history"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        val devices = parameters["devices"]?.map { Identifier(it) } ?: run {
            logger.warn("No devices specified to look up")
            return
        }
        client.temperatureHistory(devices).collectLatest {
            output.send(json.encodeToString(RelativeHourMetricSerializer, it))
        }
    }
}
