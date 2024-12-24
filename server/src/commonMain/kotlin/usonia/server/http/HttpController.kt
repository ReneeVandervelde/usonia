package usonia.server.http

/**
 * Handle an incoming HTTP request.
 */
interface HttpController {
    val path: String
    val method: String get() = "GET"

    suspend fun requiresAuthorization(
        request: HttpRequest
    ): Boolean = true

    suspend fun getResponse(
        request: HttpRequest
    ): HttpResponse
}
