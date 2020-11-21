package usonia.core.server

import usonia.core.AppConfig

interface WebServer {
    suspend fun serve(config: AppConfig)
}
