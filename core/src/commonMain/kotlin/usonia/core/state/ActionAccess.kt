package usonia.core.state

import usonia.foundation.Action
import usonia.kotlin.OngoingFlow

/**
 * Read access to actions that are to be taken on devices.
 */
interface ActionAccess {
    val actions: OngoingFlow<Action>
}
