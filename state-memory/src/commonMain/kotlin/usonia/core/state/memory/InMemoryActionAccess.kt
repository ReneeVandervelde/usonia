package usonia.core.state.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.foundation.Action

class InMemoryActionAccess: ActionPublisher, ActionAccess {
    private val sharedActions = MutableSharedFlow<Action>()
    override val actions: Flow<Action> = sharedActions

    override suspend fun publishAction(action: Action) {
        sharedActions.emit(action)
    }
}
