package usonia.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UsoniaClient(
    private val host: String,
    private val port: Int = 80
) {
    private val httpClient = HttpClient {
        install(WebSockets)
    }

    val logs: Flow<String> = flow {
        httpClient.ws(
            host = host,
            port = port,
            path = "logs"
        ) {
            incoming.consumeEach {
                if (it is Frame.Text) emit(it.readText())
            }
        }
    }
}
