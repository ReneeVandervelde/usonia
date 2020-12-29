package usonia.server

import usonia.server.cron.CronJob
import usonia.server.http.HttpController
import usonia.server.http.WebSocketController

/**
 * Main entry-point for modules to add functionality to backend services.
 */
interface ServerPlugin {
    val daemons: List<Daemon> get() = emptyList()
    val httpControllers: List<HttpController> get() = emptyList()
    val socketController: List<WebSocketController> get() = emptyList()
    val staticResources: List<String> get() = emptyList()
    val crons: List<CronJob> get() = emptyList()
}
