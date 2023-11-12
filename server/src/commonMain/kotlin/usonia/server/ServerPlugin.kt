package usonia.server

import regolith.init.Initializer
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.server.http.HttpController
import usonia.server.http.WebSocketController

/**
 * Main entry-point for modules to add functionality to backend services.
 */
interface ServerPlugin {
    val initializers: List<Initializer> get() = emptyList()
    val daemons: List<Daemon> get() = emptyList()
    val crons: List<CronJob> get() = emptyList()
    val httpControllers: List<HttpController> get() = emptyList()
    val socketController: List<WebSocketController> get() = emptyList()
    val staticResources: List<String> get() = emptyList()
}
