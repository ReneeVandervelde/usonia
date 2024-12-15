package usonia.server.ktor

import com.ionspin.kotlin.crypto.hash.Hash
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
import com.ionspin.kotlin.crypto.util.toHexString
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import usonia.core.state.findBridgeAuthById
import usonia.foundation.Identifier
import usonia.server.AppConfig
import usonia.server.WebServer
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import kotlin.coroutines.suspendCoroutine

class KtorWebServer(
    private val client: BackendClient,
    private val port: Int = 80,
    private val logger: KimchiLogger = EmptyLogger
): WebServer {
    override suspend fun serve(config: AppConfig) {
        val httpControllers = config.plugins.flatMap { it.httpControllers }
        val socketControllers = config.plugins.flatMap { it.socketController }
        val staticResources = config.plugins.flatMap { it.staticResources }

        suspendCoroutine<Nothing> {
            val server = embeddedServer(Netty, port) {
                install(WebSockets)
                intercept(ApplicationCallPipeline.Monitoring) {
                    logger.trace("${call.request.httpMethod.value}: ${call.request.uri}")
                    proceed()
                }

                routing {
                    socketControllers.forEach { controller ->
                        logger.debug { "Loading Socket Controller: ${controller::class.simpleName}" }
                        webSocket(controller.path) {
                            logger.trace { "OPEN: ${controller::class.simpleName}" }
                            val input = Channel<String>(Channel.RENDEZVOUS)
                            val output = Channel<String>(Channel.RENDEZVOUS)
                            val controllerJob = launch {
                                controller.start(input, output, call.parameters.toMap())
                            }
                            val incomingJob = launch {
                                incoming.consumeEach { frame ->
                                    when (frame) {
                                        is Frame.Text -> input.send(frame.readText())
                                        is Frame.Close -> {
                                            logger.debug { "CLOSE: <${controller::class.simpleName}>" }
                                            controllerJob.cancel()
                                            close()
                                        }
                                        else -> logger.debug("Unhandled frame type: ${it::class.simpleName}")
                                    }
                                }
                                logger.trace("Incoming Job completed on socket <${controller::class.simpleName}>")
                            }
                            val outputJob = launch {
                                output.consumeEach {
                                    send(it)
                                }
                            }
                            incomingJob.join()
                            outputJob.cancel()
                            controllerJob.cancel()
                            logger.trace { "CLOSED: ${controller::class.simpleName}" }
                            close()
                        }
                    }
                    httpControllers.forEach { controller ->
                        logger.debug("Loading HTTP Controller: ${controller::class.simpleName}")
                        route(controller.path, controller.method.let(::HttpMethod)) {
                            handle {
                                logger.trace { "HANDLE: ${controller::class.simpleName}" }
                                val request = HttpRequest(
                                    body = call.receiveText(),
                                    headers = call.request.headers.toMap(),
                                    parameters = call.parameters.toMap(),
                                )
                                if (controller.authorized) {
                                    val auth = request.headers["X-Signature"]
                                        ?.singleOrNull()
                                        ?: return@handle call.respond(HttpStatusCode.Unauthorized, "Illegal/Missing Authorization")
                                            .also { logger.trace("Rejecting Request with no signature") }
                                    val timestamp = request.headers["X-Timestamp"]
                                        ?.singleOrNull()
                                        ?.toLongOrNull()
                                        ?.let { Instant.fromEpochMilliseconds(it) }
                                        ?: return@handle call.respond(HttpStatusCode.Unauthorized, "Illegal/Missing timestamp")
                                            .also { logger.trace("Rejecting Request with no timestamp") }
                                    val bridge = request.headers["X-Bridge-Id"]
                                        ?.singleOrNull()
                                        ?.let(::Identifier)
                                        ?: return@handle call.respond(HttpStatusCode.Unauthorized, "Illegal/Missing bridge id")
                                            .also { logger.trace("Rejecting Request with no ID") }
                                    val bridgePsk = client.findBridgeAuthById(bridge)
                                        ?.psk
                                        ?: return@handle call.respond(HttpStatusCode.Unauthorized, "Bridge not authorized")
                                            .also { logger.trace("Rejecting Request with no bridge config") }
                                    val expectedAuth = (request.body.orEmpty() + timestamp.toEpochMilliseconds().toString() + bridgePsk)
                                        .encodeToUByteArray()
                                        .let(Hash::sha256)
                                        .toHexString()

                                    if (auth != expectedAuth) {
                                        logger.trace("Rejecting Request with invalid auth. Expected <$expectedAuth> but got <$auth>")
                                        return@handle call.respond(HttpStatusCode.Unauthorized, "Invalid Authorization")
                                    }
                                }
                                val response = runCatching { controller.getResponse(request) }
                                    .onFailure {
                                        logger.error("Failed to get response", it)
                                    }
                                    .getOrThrow()
                                call.respondText(
                                    text = response.body,
                                    contentType = response.contentType.split('/').let { ContentType(it[0], it.getOrNull(1).orEmpty()) },
                                    status = HttpStatusCode(response.status, "")
                                )
                            }
                        }
                    }
                    static("static") {
                        staticResources.forEach {
                            logger.debug("Including Static Resource: <$it>")
                            resource(it)
                        }
                    }
                }
            }
            server.start()
            logger.trace("WebServer running")
        }
    }
}
