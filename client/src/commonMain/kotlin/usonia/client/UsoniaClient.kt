package usonia.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.Site
import usonia.serialization.ActionSerializer
import usonia.serialization.EventSerializer
import usonia.serialization.SiteSerializer
import usonia.serialization.StatusSerializer

/**
 * HTTP Client for interacting with a Usonia server.
 */
class UsoniaClient(
    private val host: String,
    private val port: Int = 80
) {
    private val httpClient = HttpClient {
        install(WebSockets)
    }

    /**
     * Ongoing flow of log statements being recorded in the server.
     */
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

    /**
     * Ongoing flow of events being broadcast in the server.
     */
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

    val config: Flow<Site> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "config",
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                val site = it.readText().let { Json.decodeFromString(SiteSerializer, it) }
                emit(site)
            }
        }
    }

    /**
     * Send an action to the server to be broadcast.
     */
    suspend fun sendAction(action: Action) {
        val request = HttpRequestBuilder().apply {
            body = Json.encodeToString(ActionSerializer, action)
        }

        httpClient.post<HttpResponse>(request).readText()
    }
}
