package usonia.core.state.memory

import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.foundation.Action
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing

class InMemoryActionAccess: ActionPublisher, ActionAccess {
    private val sharedActions = MutableSharedFlow<Action>()
    override val actions: OngoingFlow<Action> = sharedActions.asOngoing()

    override suspend fun publishAction(action: Action) {
        sharedActions.emit(action)
    }
}
