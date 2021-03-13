package usonia.frontend

/**
 * Class bound at view startup.
 */
interface Controller {
    suspend fun onReady()
}
