package usonia.state

import usonia.foundation.Action

class ActionPublisherSpy: ActionPublisher {
    val actions = mutableListOf<Action>()
    override suspend fun publishAction(action: Action) {
        actions += action
    }
}
