package usonia.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.cio.websocket.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import usonia.foundation.*
import usonia.serialization.*

/**
 * HTTP Client for interacting with a Usonia server.
 */
class UsoniaClient(
    private val host: String,
    private val port: Int = 80,
    private val siteSerializer: SiteSerializer = SiteSerializer(emptySet()),
    private val logger: KimchiLogger = EmptyLogger,
) {
    private val httpClient = HttpClient {
        install(WebSockets)
    }

    /**
     * Ongoing flow of log statements being recorded in the server.
     */
    val logs: Flow<LogMessage> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "logs"
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach

                try {
                    val logMessage = Json.decodeFromString(LogMessageSerializer, it.readText())
                    emit(logMessage)
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize Log Message", error)
                }
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
                try {
                    val event = it.readText().let { Json.decodeFromString(EventSerializer, it) }
                    emit(event)
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize Event", error)
                }
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

                try {
                    val site = it.readText().let { Json.decodeFromString(siteSerializer, it) }
                    emit(site)
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize site config", error)
                }
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
