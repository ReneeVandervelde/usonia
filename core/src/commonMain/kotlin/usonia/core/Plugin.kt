package usonia.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import usonia.core.cron.CronJob
import usonia.core.server.HttpController
import usonia.core.server.WebSocketController

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
val test: Flow<List<Any>> = TODO()
val test2: Flow<List<Any>> = TODO()
val test3 = combine(listOf(test, test2)) {
    it.toList().flatten()
}
