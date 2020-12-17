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
import usonia.foundation.Status
import usonia.serialization.ActionSerializer
import usonia.serialization.EventSerializer
import usonia.serialization.SiteSerializer
import usonia.serialization.StatusSerializer

/**
 * HTTP Client for interacting with a Usonia server.
 */
class UsoniaClient(
    private val host: String,
    private val port: Int = 80,
    private val siteSerializer: SiteSerializer = SiteSerializer(emptySet()),
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

    /**
     * Ongoing flow of changes to the site configuration.
     */
    val config: Flow<Site> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "config",
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                val site = it.readText().let { Json.decodeFromString(siteSerializer, it) }
                emit(site)
            }
        }
    }

    /**
     * Send an action to the server to be broadcast.
     */
    suspend fun sendAction(action: Action): Status {
        val request = HttpRequestBuilder().apply {
            body = Json.encodeToString(ActionSerializer, action)
        }

        val response = httpClient.post<HttpResponse>(request).readText()

        return Json.decodeFromString(StatusSerializer, response)
    }

    /**
     * Send an event to the server to be broadcast
     */
    suspend fun sendEvent(event: Event): Status {
        val request = HttpRequestBuilder().apply {
            body = Json.encodeToString(EventSerializer, event)
        }

        val response = httpClient.post<HttpResponse>(request).readText()

        return Json.decodeFromString(StatusSerializer, response)
    }
}
