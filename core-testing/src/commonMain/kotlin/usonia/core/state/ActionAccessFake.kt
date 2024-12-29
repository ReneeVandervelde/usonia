package usonia.core.state

import com.inkapplications.coroutines.ongoing.asOngoing
import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Action

class ActionAccessFake: ActionAccess {
    val mutableActions = MutableSharedFlow<Action>()
    override val actions = mutableActions.asOngoing()
}
