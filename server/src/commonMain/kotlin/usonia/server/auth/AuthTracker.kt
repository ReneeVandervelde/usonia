package usonia.server.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

internal class AuthTracker (
    private val clock: Clock,
) {
    private val allowedWindow = 1.minutes
    private val replayCacheWindow = 5.minutes
    private val auths = MutableStateFlow(emptySet<AuthToken>())

    fun consume(token: AuthToken)
    {
        val now = clock.now()
        if (token.timestamp < now.minus(allowedWindow)) {
            throw StaleToken
        }
        auths.getAndUpdate { previous ->
            if (token in previous) throw AlreadyConsumed

            (previous + token)
                .filter { it.timestamp > now.minus(replayCacheWindow) }
                .toSet()
        }
    }

    object AlreadyConsumed: IllegalArgumentException("Token already consumed")
    object StaleToken: IllegalArgumentException("Token is too old")
}
