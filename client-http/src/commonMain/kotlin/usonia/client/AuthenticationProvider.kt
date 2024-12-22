package usonia.client

import usonia.auth.Auth

/**
 * Access to authentication data needed for authorized server requests.
 */
interface AuthenticationProvider {
    /**
     * ID of the bridge that the auth token is associated with.
     */
    val bridgeIdentifier: String

    /**
     * The authentication secret to use for authorized requests.
     */
    val auth: Auth.Psk?
}
