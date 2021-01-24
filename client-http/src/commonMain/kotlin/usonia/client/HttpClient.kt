package usonia.client

import io.ktor.client.*
import io.ktor.client.features.*
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
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import usonia.foundation.*
import kotlin.reflect.KClass

/**
 * HTTP Client for interacting with a Usonia server.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalCoroutinesApi::class)
class HttpClient(
    private val host: String,
    private val port: Int = 80,
    private val json: Json,
    private val logger: KimchiLogger = EmptyLogger,
): FrontendClient {
    private val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
    }

    override val logs: Flow<LogMessage> = flow {
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

    override fun bufferedLogs(limit: Int): Flow<LogMessage> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "logs",
            request = {
                parameter("bufferCount", limit)
            }
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

    override val events: Flow<Event> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "events"
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                try {
                    emit(json.decodeFromString(EventSerializer, it.readText()))
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize Event", error)
                }
            }
        }
    }

    override val site: Flow<Site> = flow {
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

    override suspend fun updateSite(site: Site) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/site",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(Site.serializer(), site)
        }

        httpClient.post<Status>(request)
    }

    override suspend fun <T: Event> getState(id: Identifier, type: KClass<T>): T? {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/events/latest/${id.value}/${type.simpleName}",
        ).apply {
            accept(ContentType.Application.Json)
        }

        return try {
            httpClient.get<String>(request).let { json.decodeFromString(EventSerializer, it) as T }
        } catch (error: ClientRequestException) {
            if (error.response.status.value == 404) null else throw error
        }
    }

    override suspend fun publishAction(action: Action) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/actions",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(ActionSerializer, action)
        }

        httpClient.post<Status>(request)
    }

    override suspend fun publishEvent(event: Event) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/events",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(EventSerializer, event)
        }

        httpClient.post<Status>(request)
    }
}
