package usonia.web

import usonia.server.http.HttpController

object AppRouteController: HttpController by DefaultController {
    override val path: String = "/{...}"
}
