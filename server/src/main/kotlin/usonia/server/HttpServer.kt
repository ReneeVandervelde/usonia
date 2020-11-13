package usonia.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class HttpServer(
    private val controllers: List<HttpController>,
    private val port: Int = 80
) {
    @OptIn(ExperimentalTime::class)
    suspend fun run(
        gracePeriod: Duration = 5.seconds,
        timeout: Duration = 20.seconds
    ) {
        suspendCancellableCoroutine<Unit> {
            val server = embeddedServer(Netty, port) {
                routing {
                    controllers.forEach { controller ->
                        route(controller.path, controller.method.let(::HttpMethod)) {
                            handle {
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

            it.invokeOnCancellation {
                server.stop(
                    gracePeriod.inMilliseconds.toLong(),
                    timeout.inMilliseconds.toLong()
                )
            }
        }
    }
}
