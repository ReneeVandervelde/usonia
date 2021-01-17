package usonia.state

import com.mongodb.reactivestreams.client.MongoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.descending
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

internal class DatabaseStateAccess(
    val database: MongoDatabase,
): MongoState {
    private val eventsCollection get() = database.getCollection("events", Event::class.java).coroutine
    private val eventsFlow = MutableSharedFlow<Event>()

    override val events: Flow<Event> = eventsFlow

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return eventsCollection
            .find("""{"type": "${type.simpleName}", "source": "${id.value}"}""")
            .sort(descending(Event::timestamp))
            .limit(1)
            .first()
            as T?
    }

    override suspend fun publishEvent(event: Event) {
        eventsCollection.insertOne(event)
        eventsFlow.emit(event)
    }
}
