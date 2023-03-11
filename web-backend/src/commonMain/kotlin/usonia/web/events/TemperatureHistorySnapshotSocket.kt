package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.foundation.Identifier
import usonia.kotlin.collectLatest
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController
import kotlin.time.Duration.Companion.milliseconds

class TemperatureHistorySnapshotSocket(
    private val client: BackendClient,
    private val json: Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path: String = "/events/temperature-history-snapshots"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        val devices = parameters["devices"]?.map { Identifier(it) } ?: run {
            logger.warn("No devices specified to look up")
            return
        }
        val limit = parameters["limit"]?.firstOrNull()?.toLong()?.milliseconds

        client.temperatureHistorySnapshots(devices, limit).collectLatest {
            try {
                output.send(json.encodeToString(it))
            } catch (e: Throwable) {
                logger.error("Unable to send response", e)
            }
        }
    }
}
