package usonia.core.state

/**
 * Provides access to disarming security system.
 *
 * This interface should not be provided to untrusted clients.
 */
interface FullSecurityAccess {
    /**
     * Disarm the security system.
     */
    suspend fun disarmSecurity()
}
