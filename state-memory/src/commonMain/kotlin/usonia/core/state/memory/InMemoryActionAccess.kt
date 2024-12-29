package usonia.core.state.memory

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.foundation.Action

class InMemoryActionAccess: ActionPublisher, ActionAccess {
    private val sharedActions = MutableSharedFlow<Action>()
    override val actions: OngoingFlow<Action> = sharedActions.asOngoing()

    override suspend fun publishAction(action: Action) {
        sharedActions.emit(action)
    }
}
