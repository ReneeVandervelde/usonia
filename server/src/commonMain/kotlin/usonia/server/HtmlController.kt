package usonia.server

/**
 * Controller that sends back a simple html response.
 */
abstract class HtmlController: HttpController {
    /**
     * Generate an HTML response body.
     */
    abstract suspend fun getHtml(): String

    final override suspend fun getResponse(request: HttpRequest): HttpResponse {
        return HttpResponse(
            body = getHtml(),
            contentType = "text/html"
        )
    }
}
