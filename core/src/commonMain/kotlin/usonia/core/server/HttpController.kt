package usonia.core.server

/**
 * Handle an incoming HTTP request.
 */
interface HttpController {
    val path: String
    val method: String get() = "GET"

    suspend fun getResponse(
        request: HttpRequest
    ): HttpResponse
}

