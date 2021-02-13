package usonia.server

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.core.state.*
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.Site
import usonia.server.client.ComposedBackendClient
import kotlin.reflect.KClass

val DummyClient = ComposedBackendClient(
    actionAccess = object: ActionAccess {
        override val actions: Flow<Action> get() = TODO("Not yet implemented")
    },
    actionPublisher = object: ActionPublisher {
        override suspend fun publishAction(action: Action) = TODO("Not yet implemented")
    },
    eventAccess = object: EventAccess {
        override val events: Flow<Event> get() = TODO("Not yet implemented")
        override val eventsByDay: Flow<Map<LocalDate, Int>> get() = TODO()
        override val oldestEventTime: Flow<Instant?> get() = TODO()
        override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = TODO("Not yet implemented")
    },
    eventPublisher = object: EventPublisher {
        override suspend fun publishEvent(event: Event) = TODO("Not yet implemented")
    },
    configurationAccess = object: ConfigurationAccess {
        override val site: Flow<Site> get() = TODO("Not yet implemented")
        override val flags: Flow<Map<String, String?>> get() = TODO("Not yet implemented")
        override suspend fun updateSite(site: Site) = TODO("Not yet implemented")
        override suspend fun setFlag(key: String, value: String?) = TODO()
        override suspend fun removeFlag(key: String) = TODO()
    },
)
