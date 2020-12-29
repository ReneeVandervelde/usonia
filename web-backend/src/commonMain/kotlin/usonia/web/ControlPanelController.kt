package usonia.web

import usonia.server.http.StaticHtmlController

internal object ControlPanelController: StaticHtmlController() {
    override val path: String = "/"
    override val resource: String = "ControlPanel.html"
}
