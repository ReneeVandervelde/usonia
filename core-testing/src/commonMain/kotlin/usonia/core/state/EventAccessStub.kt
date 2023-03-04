package usonia.core.state

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import kotlin.reflect.KClass

object EventAccessStub: EventAccess {
    override val events: OngoingFlow<Event> get() = ongoingFlowOf()
    override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> get() = ongoingFlowOf()
    override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf()

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = null
    override fun deviceEventHistory(id: Identifier, size: Int?): OngoingFlow<List<Event>> = ongoingFlowOf()
    override fun temperatureHistory(devices: Collection<Identifier>): OngoingFlow<Map<Int, Float>> = ongoingFlowOf()
    override fun getLatestEvent(id: Identifier): OngoingFlow<Event> = ongoingFlowOf()
}
