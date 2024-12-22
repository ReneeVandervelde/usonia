package usonia.server.auth

import usonia.auth.Auth

/**
 * Parameter data tracked as part of replay protection.
 */
internal data class AuthParamToken(
    val timestamp: Auth.Timestamp,
    val nonce: Auth.Nonce,
)
