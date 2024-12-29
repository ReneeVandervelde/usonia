package usonia.core.state

import com.inkapplications.coroutines.ongoing.OngoingFlow
import usonia.foundation.Action

/**
 * Read access to actions that are to be taken on devices.
 */
interface ActionAccess {
    val actions: OngoingFlow<Action>
}
