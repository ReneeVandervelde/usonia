package usonia.core

import usonia.server.StaticHtmlController

object ControlPanelController: StaticHtmlController() {
    override val path: String = "/"
    override val resource: String = "ControlPanel.html"
}
