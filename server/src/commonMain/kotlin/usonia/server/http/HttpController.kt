package usonia.server.http

/**
 * Handle an incoming HTTP request.
 */
interface HttpController {
    val path: String
    val method: String get() = "GET"
    val authorized: Boolean get() = false

    suspend fun getResponse(
        request: HttpRequest
    ): HttpResponse
}

