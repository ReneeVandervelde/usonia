package usonia.server

interface WebServer {
    suspend fun serve(config: AppConfig)
}
