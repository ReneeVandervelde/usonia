package usonia.client

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import usonia.foundation.*

/**
 * HTTP Client for interacting with a Usonia server.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
class UsoniaClient constructor(
    private val host: String,
    private val port: Int = 80,
    private val json: Json,
    private val logger: KimchiLogger = EmptyLogger,
) {
    private val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
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
                    emit(json.decodeFromString(it.readText()))
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
                    emit(json.decodeFromString(it.readText()))
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
                    emit(json.decodeFromString(it.readText()))
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
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/actions",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(ActionSerializer, action)
        }

        return httpClient.post(request)
    }

    /**
     * Send an event to the server to be broadcast
     */
    suspend fun sendEvent(event: Event): Status {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/events",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(EventSerializer, event)
        }

        return httpClient.post(request)
    }
}
