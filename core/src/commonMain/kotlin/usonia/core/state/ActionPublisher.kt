package usonia.core.state

import usonia.foundation.Action

/**
 * Write access to outgoing actions to be taken on devices.
 */
interface ActionPublisher {
    suspend fun publishAction(action: Action)
}

