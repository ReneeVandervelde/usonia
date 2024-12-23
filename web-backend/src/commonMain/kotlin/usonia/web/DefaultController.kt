package usonia.web

import usonia.kotlin.getResourceContents
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

object DefaultController: HttpController {
    override val path: String = "/"
    override val authorized: Boolean = false

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        return HttpResponse(
            body = getResourceContents("index.html"),
            contentType = "text/html"
        )
    }
}
