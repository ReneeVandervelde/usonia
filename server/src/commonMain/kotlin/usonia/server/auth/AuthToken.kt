package usonia.server.auth

import kotlinx.datetime.Instant

internal data class AuthToken(
    val signature: String,
    val timestamp: Instant,
)
