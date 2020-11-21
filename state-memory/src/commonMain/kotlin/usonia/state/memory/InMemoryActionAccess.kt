package usonia.state.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Action
import usonia.state.ActionAccess
import usonia.state.ActionPublisher

class InMemoryActionAccess: ActionPublisher, ActionAccess {
    private val sharedActions = MutableSharedFlow<Action>()
    override val actions: Flow<Action> = sharedActions

    override suspend fun publishAction(action: Action) {
        sharedActions.emit(action)
    }
}
