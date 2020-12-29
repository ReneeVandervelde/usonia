package usonia.core.client

import kotlinx.coroutines.flow.*
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
