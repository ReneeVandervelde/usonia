package usonia.core.state

import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Action
import usonia.kotlin.asOngoing

class ActionAccessFake: ActionAccess {
    val mutableActions = MutableSharedFlow<Action>()
    override val actions = mutableActions.asOngoing()
}
