package usonia.state

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.registerModule
import usonia.foundation.EventSerializer

class MongoModule(
    databaseName: String = "local"
) {
    private val client by lazy {
        KMongo.createClient().also {
            registerModule(SerializersModule {
                contextual(EventSerializer)
            })
        }
    }
    private val database by lazy { client.getDatabase(databaseName) }

    fun stateAccess(): MongoState = DatabaseStateAccess(database)
}
