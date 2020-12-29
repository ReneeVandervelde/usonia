package usonia.server.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import usonia.server.AppConfig
import usonia.server.WebServer
import usonia.server.http.HttpRequest
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KtorWebServer(
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
                                controller.start(input, output)
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
                                    }
                                }
                            }
                            val outputJob = launch {
                                output.consumeEach {
                                    send(it)
                                }
                            }
                            incomingJob.join()
                            outputJob.join()
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
