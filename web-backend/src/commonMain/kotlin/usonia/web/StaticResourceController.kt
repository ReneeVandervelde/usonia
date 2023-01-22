package usonia.web

import usonia.kotlin.getResourceContents
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

class StaticResourceController(
    private val extension: String,
    private val contentType: String,
): HttpController {
    override val path: String = "/{name}.$extension"

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        return HttpResponse(
            body = getResourceContents(request.parameters.getValue("name").single() + ".$extension"),
            contentType = contentType
        )
    }
}
