package usonia.core.server

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Handle websocket I/O.
 */
interface WebSocketController {
    val path: String

    /**
     * Called when a websocket is opened by a client.
     *
     * @param input Messages sent from the client.
     * @param output Messages to send back to the client.
     */
    suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>)
}
