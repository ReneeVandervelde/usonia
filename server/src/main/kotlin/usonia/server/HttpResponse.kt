package usonia.server

/**
 * Response data sent back to the client from this server.
 */
data class HttpResponse(
    val body: String,
    val contentType: String = "text/plain",
    val status: Int = 200
)
