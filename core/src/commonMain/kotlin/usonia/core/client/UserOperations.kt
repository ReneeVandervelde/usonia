package usonia.core.client

import kotlinx.coroutines.flow.*
import usonia.core.state.getSite
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.User

/**
 * Observes a specific user's presence state, starting with its last known state.
 */
fun UsoniaClient.userPresence(user: Identifier): Flow<Event.Presence?> {
    return events
        .filter { it.source == user }
        .filterIsInstance<Event.Presence>()
        .filterIsInstance<Event.Presence?>()
        .onStart { getState(user, Event.Presence::class).also { emit(it) } }
}

/**
 * Observes all users' presence states, starting with their last known state.
 */
val UsoniaClient.userPresenceStates: Flow<Pair<User, Event.Presence?>> get() {
    return site.map { it.users }
        .flatMapLatest { users ->
            users.map { user ->
                userPresence(user.id).map { user to it }
            }.merge()
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
) {
    getSite().users
        .filter { level >= it.alertLevel }
        .forEach { user ->
            publishAction(Action.Alert(
                target = user.id,
                message = message,
                level = level,
            ))
        }
}
