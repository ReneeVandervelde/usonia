package usonia.server.ktor

import com.inkapplications.standard.throwCancels
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
import kotlinx.serialization.json.Json
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.AppConfig
import usonia.server.WebServer
import usonia.server.auth.AuthResult
import usonia.server.auth.Authorization
import usonia.server.http.BodyDecodeFailure
import usonia.server.http.HttpRequest
import usonia.server.http.SocketCall
import kotlin.coroutines.suspendCoroutine

class KtorWebServer(
    private val authorization: Authorization,
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
                            val socketRequest = SocketCall(
                                parameters = call.parameters.toMap(),
                            )
                            val authResult = authorization.validate(socketRequest)
                            if (controller.authorized && authResult is AuthResult.Failure) {
                                logger.info("Rejecting Request after failed auth.")
                                close()
                                return@webSocket
                            }
                            val controllerJob = launch {
                                controller.start(input, output, socketRequest.parameters)
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
                                runCatching {
                                    if (controller.requiresAuthorization(request)) {
                                        when (val authResult = authorization.validate(request)) {
                                            is AuthResult.Failure -> {
                                                logger.info("Rejecting Request after failed auth.")
                                                return@handle call.respond(HttpStatusCode.Unauthorized, authResult)
                                            }

                                            AuthResult.Success -> {
                                                logger.trace("Request authorized")
                                            }
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
                                }.throwCancels().onFailure { error ->
                                    when (error) {
                                        is BodyDecodeFailure -> call.respondText(
                                            text = Statuses.ILLEGAL_BODY.let { Json.encodeToString(Status.serializer(), it) },
                                        )
                                        else -> call.respondText(
                                            text = Statuses.UNKNOWN.let { Json.encodeToString(Status.serializer(), it) },
                                        )
                                    }
                                }
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
