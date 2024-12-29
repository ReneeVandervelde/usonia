package usonia.core.state

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.foundation.Event
import usonia.foundation.EventCategory
import usonia.foundation.Identifier
import usonia.foundation.TemperatureSnapshot
import kotlin.reflect.KClass
import kotlin.time.Duration

object EventAccessStub: EventAccess {
    override val events: OngoingFlow<Event> get() = ongoingFlowOf()
    override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> get() = ongoingFlowOf()
    override val oldestEventTime: OngoingFlow<Instant?> = ongoingFlowOf()

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = null
    override fun deviceEventHistory(id: Identifier, size: Int?): OngoingFlow<List<Event>> = ongoingFlowOf()
    override fun temperatureHistorySnapshots(devices: Collection<Identifier>, limit: Duration?): OngoingFlow<List<TemperatureSnapshot>> = ongoingFlowOf()
    override fun getLatestEvent(id: Identifier): OngoingFlow<Event> = ongoingFlowOf()
    override fun eventCount(id: Identifier, category: EventCategory): OngoingFlow<Long> = ongoingFlowOf()
}
