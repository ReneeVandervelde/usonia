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
import usonia.core.Daemon
import usonia.core.Usonia
import usonia.server.HttpRequest
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
internal class KtorWebServer(
    private val port: Int = 80,
    private val gracePeriod: Duration = 5.seconds,
    private val timeout: Duration = 20.seconds,
    private val logger: KimchiLogger = EmptyLogger
): Daemon {
    override suspend fun start(app: Usonia): Nothing {
        val httpControllers = app.plugins.flatMap { it.httpControllers }
        val socketControllers = app.plugins.flatMap { it.socketController }
        val staticResources = app.plugins.flatMap { it.staticResources }

        logger.info("Starting WebServer")
        suspendCoroutine<Nothing> {
            val server = embeddedServer(Netty, port) {
                install(WebSockets)

                routing {
                    socketControllers.forEach { controller ->
                        logger.debug("Loading Socket Controller: ${controller::class.simpleName}")
                        webSocket(controller.path) {
                            logger.info("Handling Socket Request to ${controller::class.simpleName}")
                            val input = Channel<String>(Channel.RENDEZVOUS)
                            val output = Channel<String>(Channel.RENDEZVOUS)
                            launch {
                                incoming.consumeEach {
                                    if (it is Frame.Text) input.send(it.readText())
                                }
                            }
                            launch {
                                output.consumeEach {
                                    send(it)
                                }
                            }
                            controller.start(input, output)
                            close()
                        }
                    }
                    httpControllers.forEach { controller ->
                        logger.debug("Loading HTTP Controller: ${controller::class.simpleName}")
                        route(controller.path, controller.method.let(::HttpMethod)) {
                            handle {
                                logger.info("Handling HTTP Request to ${controller::class.simpleName}")
                                val request = HttpRequest(
                                    body = call.receiveText(),
                                    headers = call.request.headers.toMap()
                                )
                                val response = runCatching { controller.getResponse(request) }
                                    .onFailure {
                                        logger.error("Failed to get response", it)
                                        it.printStackTrace()
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
