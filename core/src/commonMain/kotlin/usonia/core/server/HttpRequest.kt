package usonia.core.server

/**
 * Request data received by the server from the client.
 */
data class HttpRequest(
    val body: String? = null,
    val headers: Map<String, List<String>>,
    val parameters: Map<String, List<String>>
)
