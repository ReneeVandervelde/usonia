package usonia.core.client

import com.inkapplications.coroutines.ongoing.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import usonia.core.state.getSite
import usonia.foundation.*

/**
 * Observes a specific user's presence state, starting with its last known state.
 */
fun UsoniaClient.userPresence(user: Identifier): OngoingFlow<Event.Presence?> {
    return events
        .filter { it.source == user }
        .filterIsInstance<Event.Presence>()
        .filterIsInstance<Event.Presence?>()
        .unsafeTransform { onStart { getState(user, Event.Presence::class).also { emit(it) } }
    }
}

/**
 * Get the current user presence state for a user
 */
suspend fun UsoniaClient.getUserPresence(user: Identifier): PresenceState? {
    return getState(user, Event.Presence::class)?.state
}

/**
 * Observes all users' presence states, starting with their last known state.
 */
val UsoniaClient.userPresenceStates: OngoingFlow<List<Pair<User, Event.Presence?>>> get() {
    return site
        .map { it.users }
        .flatMapLatest { users ->
            users.map { user ->
                userPresence(user.id).asFlow().map { user to it }
            }.let {
                combine(it) { it.toList() }
            }
        }
}

/**
 * Send an Alert to all users that meet a given level threshold.
 *
 * @param message The alert message to send to each user
 * @param level The minimum threshold the user must be subscribed to to receive the alert.
 */
suspend fun UsoniaClient.alertAll(
    message: String,
    level: Action.Alert.Level,
    icon: Action.Alert.Icon? = null,
) {
    getSite().users
        .filter { level >= it.alertLevel }
        .forEach { user ->
            publishAction(Action.Alert(
                target = user.id,
                message = message,
                level = level,
                icon = icon,
            ))
        }
}
