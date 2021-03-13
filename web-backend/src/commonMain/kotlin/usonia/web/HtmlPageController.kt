package usonia.web

import usonia.kotlin.getResourceContents
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

object HtmlPageController: HttpController {
    override val path: String = "/{name}.html"

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        return HttpResponse(
            body = getResourceContents(request.parameters.getValue("name").single() + ".html"),
            contentType = "text/html"
        )
    }
}
