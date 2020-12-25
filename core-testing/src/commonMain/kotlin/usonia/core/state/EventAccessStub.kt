package usonia.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

object EventAccessStub: EventAccess {
    override val events: Flow<Event> get() = emptyFlow()
    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = null
}
