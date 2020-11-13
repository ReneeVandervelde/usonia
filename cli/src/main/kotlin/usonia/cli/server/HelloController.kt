package usonia.cli.server

import usonia.server.HttpController
import usonia.server.HttpRequest
import usonia.server.HttpResponse

object HelloController: HttpController {
    override val path: String = "hello"

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        return HttpResponse("World!")
    }
}
