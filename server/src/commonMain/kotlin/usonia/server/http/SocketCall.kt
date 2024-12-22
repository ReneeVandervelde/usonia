package usonia.server.http

/**
 * Request data receive by the server from the client socket open.
 */
data class SocketCall(
    val parameters: Map<String, List<String>>
)
