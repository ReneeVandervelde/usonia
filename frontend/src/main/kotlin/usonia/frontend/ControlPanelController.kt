package usonia.frontend

import usonia.server.StaticHtmlController

object ControlPanelController: StaticHtmlController() {
    override val path: String = "/"
    override val resource: String = "usonia/frontend-controls/ControlPanel.html"
}
