package usonia.core.state

import usonia.foundation.Action

/**
 * Write access to outgoing actions to be taken on devices.
 */
interface ActionPublisher {
    suspend fun publishAction(action: Action)
}

/**
 * Publish a collection of actions one-by-one.
 */
suspend fun ActionPublisher.publishAll(actions: Collection<Action>) {
    actions.forEach { publishAction(it) }
}
