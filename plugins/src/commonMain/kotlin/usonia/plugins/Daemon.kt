package usonia.plugins

interface Daemon {
    suspend fun start(appConfig: Usonia)
}
