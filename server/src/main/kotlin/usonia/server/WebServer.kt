package usonia.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class WebServer(
    private val httpControllers: List<HttpController> = emptyList(),
    private val socketControllers: List<WebSocketController> = emptyList(),
    private val port: Int = 80,
    private val logger: KimchiLogger = EmptyLogger
) {
    @OptIn(ExperimentalTime::class)
    suspend fun run(
        gracePeriod: Duration = 5.seconds,
        timeout: Duration = 20.seconds
    ) {
        logger.info("Starting WebServer")
        suspendCancellableCoroutine<Unit> {
            val server = embeddedServer(Netty, port) {
                install(WebSockets)

                routing {
                    socketControllers.forEach { controller ->
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
                        route(controller.path, controller.method.let(::HttpMethod)) {
                            handle {
                                logger.info("Handling HTTP Request to ${controller::class.simpleName}")
                                val request = HttpRequest(
                                    body = call.receiveText(),
                                    headers = call.request.headers.toMap()
                                )
                                val response = controller.getResponse(request)
                                call.respondText(
                                    text = response.body,
                                    contentType = response.contentType.split('/').let { ContentType(it[0], it.getOrNull(1).orEmpty()) },
                                    status = HttpStatusCode(response.status, "")
                                )
                            }
                        }
                    }
                }
            }
            server.start()
            logger.trace("WebServer running")

            it.invokeOnCancellation {
                logger.info("Stopping WebServer")
                server.stop(
                    gracePeriod.inMilliseconds.toLong(),
                    timeout.inMilliseconds.toLong()
                )
            }
        }
    }
}
