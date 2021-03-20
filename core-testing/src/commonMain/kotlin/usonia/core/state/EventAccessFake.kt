package usonia.core.state

import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Event
import usonia.kotlin.asOngoing

open class EventAccessFake: EventAccess by EventAccessStub {
    val mutableEvents = MutableSharedFlow<Event>()
    override val events = mutableEvents.asOngoing()
}
