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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlow
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

    override val logs: OngoingFlow<LogMessage> = ongoingFlow {
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

    override fun bufferedLogs(limit: Int): OngoingFlow<LogMessage> = ongoingFlow {
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

    override val events: OngoingFlow<Event> = ongoingFlow {
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

    override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> = ongoingFlow {
        httpClient.ws(
            host = host,
            port = port,
            path = "events/by-day"
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                try {
                    emit(json.decodeFromString(DateMetricSerializer, it.readText()))
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize Date-Events", error)
                }
            }
        }
    }
    override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlow {
        httpClient.ws(
            host = host,
            port = port,
            path = "events/metric-oldest"
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach
                try {
                    emit(json.decodeFromString(InstantSerializer.nullable, it.readText()))
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize oldest event metric", error)
                }
            }
        }
    }

    override val site: OngoingFlow<Site> = ongoingFlow {
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

    override val flags: OngoingFlow<Map<String, String?>> = ongoingFlow {
        httpClient.ws(
            host = host,
            port = port,
            path = "flags",
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach

                try {
                    emit(json.decodeFromString(FlagSerializer, it.readText()))
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize flags", error)
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

    override suspend fun setFlag(key: String, value: String?) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/flags/$key",
        ).apply {
            accept(ContentType.Application.Json)
            body = json.encodeToString(String.serializer().nullable, value)
        }

        httpClient.put<Status>(request)
    }

    override suspend fun removeFlag(key: String) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/flags/$key",
        ).apply {
            accept(ContentType.Application.Json)
        }

        httpClient.delete<Status>(request)
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

    override fun temperatureHistory(devices: Collection<Identifier>): OngoingFlow<Map<Int, Float>> {
        return ongoingFlow {
            httpClient.ws(
                host = host,
                port = port,
                path = "events/metric-temperature-history",
                request = {
                    devices.forEach { parameter("devices", it.value) }
                }
            ) {
                incoming.consumeEach {
                    if (it !is Frame.Text) return@consumeEach

                    try {
                        emit(json.decodeFromString(RelativeHourMetricSerializer, it.readText()))
                    } catch (error: Throwable) {
                        logger.error("Failed to deserialize temperature metrics", error)
                    }
                }
            }
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
