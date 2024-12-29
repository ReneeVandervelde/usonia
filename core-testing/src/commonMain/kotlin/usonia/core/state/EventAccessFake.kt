package usonia.core.state

import com.inkapplications.coroutines.ongoing.asOngoing
import kotlinx.coroutines.flow.MutableSharedFlow
import usonia.foundation.Event

open class EventAccessFake: EventAccess by EventAccessStub {
    val mutableEvents = MutableSharedFlow<Event>()
    override val events = mutableEvents.asOngoing()
}
