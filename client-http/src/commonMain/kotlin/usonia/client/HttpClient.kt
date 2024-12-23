package usonia.client

import com.ionspin.kotlin.crypto.hash.Hash
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.consumeEach
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import usonia.auth.Auth
import usonia.client.ktor.PlatformEngine
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlow
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * HTTP Client for interacting with a Usonia server.
 */
class HttpClient(
    private val authenticationProvider: AuthenticationProvider,
    private val host: String,
    private val port: Int = 80,
    private val json: Json,
    private val clock: Clock,
    private val logger: KimchiLogger = EmptyLogger,
): FrontendClient {
    private val httpClient = HttpClient(PlatformEngine) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    override val securityState: OngoingFlow<SecurityState> = ongoingFlow {
        httpClient.ws(
            host = host,
            port = port,
            path = "security",
            request = {
                withSocketAuth()
            },
        ) {
            incoming.consumeEach {
                if (it !is Frame.Text) return@consumeEach

                try {
                    emit(json.decodeFromString(it.readText()))
                } catch (error: Throwable) {
                    logger.error("Failed to deserialize security state", error)
                }
            }
        }
    }

    override val logs: OngoingFlow<LogMessage> = ongoingFlow {
        httpClient.ws(
            host = host,
            port = port,
            path = "logs",
            request = {
                withSocketAuth()
            },
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
                withSocketAuth()
                parameter("bufferCount", limit)
            },
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
            path = "events",
            request = {
                withSocketAuth()
            },
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
            path = "events/by-day",
            request = {
                withSocketAuth()
            },
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
            path = "events/metric-oldest",
            request = {
                withSocketAuth()
            },
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
            request = {
                withSocketAuth()
            },
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
            request = {
                withSocketAuth()
            },
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
            withAuthHeaders()
            accept(ContentType.Application.Json)
            setBody(json.encodeToString(Site.serializer(), site))
        }

        httpClient.post(request)
    }

    override suspend fun setFlag(key: String, value: String?) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/flags/$key",
        ).apply {
            withAuthHeaders()
            accept(ContentType.Application.Json)
            setBody(json.encodeToString(String.serializer().nullable, value))
        }

        httpClient.put(request)
    }

    override suspend fun removeFlag(key: String) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/flags/$key",
        ).apply {
            withAuthHeaders()
            accept(ContentType.Application.Json)
        }

        httpClient.delete(request)
        // TODO: Check status returns
    }

    override suspend fun armSecurity() {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/security/arm",
        ).apply {
            withAuthHeaders()
            accept(ContentType.Application.Json)
        }

        httpClient.put(request)
    }

    override suspend fun <T: Event> getState(id: Identifier, type: KClass<T>): T? {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/events/latest/${id.value}/${type.simpleName}",
        ).apply {
            withAuthHeaders()
            accept(ContentType.Application.Json)
        }

        val response = httpClient.get(request)

        return when (response.status) {
            HttpStatusCode.OK -> response.bodyAsText().let { json.decodeFromString(EventSerializer, it) as T }
            HttpStatusCode.NotFound -> null
            else -> throw RuntimeException("Unexpected Status: ${response.status}")
        }
    }

    override fun temperatureHistorySnapshots(
        devices: Collection<Identifier>,
        limit: Duration?
    ): OngoingFlow<List<TemperatureSnapshot>> {
        return ongoingFlow {
            httpClient.ws(
                host = host,
                port = port,
                path = "events/temperature-history-snapshots",
                request = {
                    withSocketAuth()
                    devices.forEach { parameter("devices", it.value) }
                    if (limit != null) parameter("limit", limit.inWholeMilliseconds)
                }
            ) {
                incoming.consumeEach {
                    if (it !is Frame.Text) return@consumeEach

                    try {
                        emit(json.decodeFromString(it.readText()))
                    } catch (error: Throwable) {
                        logger.error("Failed to deserialize temperature snapshots", error)
                    }
                }
            }
        }
    }

    override fun getLatestEvent(id: Identifier): OngoingFlow<Event> {
        return ongoingFlow {
            httpClient.ws(
                host = host,
                port = port,
                path = "events/latest/${id.value}",
                request = {
                    withSocketAuth()
                },
            ) {
                incoming.consumeEach {
                    if (it !is Frame.Text) return@consumeEach

                    try {
                        emit(json.decodeFromString(EventSerializer, it.readText()))
                    } catch (error: Throwable) {
                        logger.error("Failed to deserialize device event", error)
                    }
                }
            }
        }
    }

    override fun deviceEventHistory(id: Identifier, size: Int?): OngoingFlow<List<Event>> {
        return ongoingFlow {
            httpClient.ws(
                host = host,
                port = port,
                path = "events/history/${id.value}",
                request = {
                    withSocketAuth()
                    if (size != null) parameter("count", size)
                },
            ) {
                incoming.consumeEach {
                    if (it !is Frame.Text) return@consumeEach

                    try {
                        emit(json.decodeFromString(ListSerializer(EventSerializer), it.readText()))
                    } catch (error: Throwable) {
                        logger.error("Failed to deserialize device event", error)
                    }
                }
            }
        }
    }

    override fun eventCount(id: Identifier, category: EventCategory): OngoingFlow<Long> {
        return ongoingFlow {
            httpClient.ws(
                host = host,
                port = port,
                path = "events/count/${id.value}/${category.name}",
                request = {
                    withSocketAuth()
                },
            ) {
                incoming.consumeEach {
                    if (it !is Frame.Text) return@consumeEach

                    try {
                        emit(json.decodeFromString(it.readText()))
                    } catch (error: Throwable) {
                        logger.error("Failed to deserialize event count", error)
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
            withAuthHeaders()
            accept(ContentType.Application.Json)
            setBody(json.encodeToString(ActionSerializer, action))
        }

        httpClient.post(request)
    }

    override suspend fun publishEvent(event: Event) {
        val request = HttpRequestBuilder(
            host = host,
            port = port,
            path = "/events",
        ).apply {
            withAuthHeaders()
            accept(ContentType.Application.Json)
            setBody(json.encodeToString(EventSerializer, event))
        }

        httpClient.post(request)
    }

    private fun HttpRequestBuilder.withSocketAuth() {
        val timestamp = Auth.Timestamp(clock.now())
        val nonce = Auth.Nonce(Random.nextLong().toString())
        val psk = authenticationProvider.auth ?: throw IllegalStateException("Authentication not configured")
        val hash = Auth.createSignature(
            body = null,
            timestamp = timestamp,
            psk = psk,
            nonce = nonce,
        )
        parameter(Auth.Timestamp.HEADER, timestamp.instant.toEpochMilliseconds().toString())
        parameter(Auth.Nonce.HEADER, nonce.value)
        parameter(Auth.Bridge.HEADER, authenticationProvider.bridgeIdentifier)
        parameter(Auth.Signature.HEADER, hash.value)
    }

    private fun HttpMessageBuilder.withAuthHeaders() {
        val timestamp = Auth.Timestamp(clock.now())
        val nonce = Auth.Nonce(Random.nextLong().toString())
        val psk = authenticationProvider.auth ?: throw IllegalStateException("Authentication not configured")
        val hash = Auth.createSignature(
            body = null,
            timestamp = timestamp,
            psk = psk,
            nonce = nonce,
        )
        header(Auth.Timestamp.HEADER, timestamp.instant.toEpochMilliseconds().toString())
        header(Auth.Nonce.HEADER, nonce.value)
        header(Auth.Bridge.HEADER, authenticationProvider.bridgeIdentifier)
        header(Auth.Signature.HEADER, hash.value)
    }
}
