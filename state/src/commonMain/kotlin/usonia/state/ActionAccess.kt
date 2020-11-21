package usonia.state

import kotlinx.coroutines.flow.Flow
import usonia.foundation.Action

/**
 * Read access to actions that are to be taken on devices.
 */
interface ActionAccess {
    val actions: Flow<Action>
}
