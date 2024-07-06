package usonia.server

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.server.client.ComposedBackendClient
import kotlin.reflect.KClass
import kotlin.time.Duration

val DummyClient = ComposedBackendClient(
    actionAccess = object: ActionAccess {
        override val actions: OngoingFlow<Action> get() = TODO("Not yet implemented")
    },
    actionPublisher = object: ActionPublisher {
        override suspend fun publishAction(action: Action) = TODO("Not yet implemented")
    },
    eventAccess = object: EventAccess {
        override val events: OngoingFlow<Event> get() = TODO("Not yet implemented")
        override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> get() = TODO()
        override val oldestEventTime: OngoingFlow<Instant?> get() = TODO()
        override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = TODO("Not yet implemented")
        override fun deviceEventHistory(id: Identifier, size: Int?): OngoingFlow<List<Event>> = TODO()
        override fun temperatureHistorySnapshots(devices: Collection<Identifier>, limit: Duration?): OngoingFlow<List<TemperatureSnapshot>> = TODO()
        override fun getLatestEvent(id: Identifier): OngoingFlow<Event> = TODO()
    },
    eventPublisher = object: EventPublisher {
        override suspend fun publishEvent(event: Event) = TODO("Not yet implemented")
    },
    configurationAccess = object: ConfigurationAccess {
        override val site: OngoingFlow<Site> get() = TODO("Not yet implemented")
        override val flags: OngoingFlow<Map<String, String?>> get() = TODO("Not yet implemented")
        override suspend fun updateSite(site: Site) = TODO("Not yet implemented")
        override suspend fun setFlag(key: String, value: String?) = TODO()
        override suspend fun removeFlag(key: String) = TODO()
        override suspend fun armSecurity() = TODO()
        override val securityState: OngoingFlow<SecurityState> get() = TODO()
    },
    fullSecurityAccess = object: FullSecurityAccess {
        override suspend fun disarmSecurity() = TODO()
    },
)
