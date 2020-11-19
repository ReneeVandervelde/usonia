package usonia.core

import usonia.core.cron.CronJob
import usonia.server.HttpController
import usonia.server.WebSocketController

/**
 * Main entry-point for modules to add functionality to backend services.
 */
interface Plugin {
    val daemons: List<Daemon> get() = emptyList()
    val httpControllers: List<HttpController> get() = emptyList()
    val socketController: List<WebSocketController> get() = emptyList()
    val staticResources: List<String> get() = emptyList()
    val crons: List<CronJob> get() = emptyList()
}
