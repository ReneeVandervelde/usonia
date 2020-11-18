package usonia.core

import usonia.server.StaticHtmlController

internal object ControlPanelController: StaticHtmlController() {
    override val path: String = "/"
    override val resource: String = "ControlPanel.html"
}
