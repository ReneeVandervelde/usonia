package usonia.core

/**
 * A service that runs forever.
 *
 * Defines a service that will start with the main application and
 * will be re-started should anything cause it to stop.
 */
interface Daemon {
    suspend fun start(app: Usonia): Nothing
}
