package usonia.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.serialization.EventSerializer

class UsoniaClient(
    private val host: String,
    private val port: Int = 80
) {
    private val httpClient = HttpClient {
        install(WebSockets)
    }

    val logs: Flow<String> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "logs"
        ) {
            incoming.consumeEach {
                if (it is Frame.Text) emit(it.readText())
            }
        }
    }

    val events: Flow<Event> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "events"
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                val event = it.readText().let { Json.decodeFromString(EventSerializer, it) }
                emit(event)
            }
        }
    }
}
