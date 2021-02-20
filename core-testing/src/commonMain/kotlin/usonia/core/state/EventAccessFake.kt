package usonia.core.state

import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Event

open class EventAccessFake: EventAccess by EventAccessStub {
    override val events = MutableSharedFlow<Event>()
}
