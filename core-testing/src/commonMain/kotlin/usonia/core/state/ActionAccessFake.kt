package usonia.core.state

import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Action

class ActionAccessFake: ActionAccess {
    override val actions = MutableSharedFlow<Action>()
}
