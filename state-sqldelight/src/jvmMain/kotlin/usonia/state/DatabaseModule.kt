package usonia.state

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.serialization.json.Json

class DatabaseModule(
    private val json: Json,
) {
    fun database(databasePath: String): DatabaseServices {
        val database = lazy {
            JdbcSqliteDriver("jdbc:sqlite:$databasePath")
                .also { Database.Schema.create(it) }
                .let { Database(it) }
        }
        return DatabaseStateAccess(
            eventQueries = lazy { database.value.eventQueries },
            json = json,
        )
    }
}
