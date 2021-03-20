package usonia.state

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.serialization.json.Json

class DatabaseModule(
    private val json: Json,
) {
    fun database(databasePath: String): DatabaseServices = create("jdbc:sqlite:$databasePath")
    fun inMemoryDatabase(): DatabaseServices = create(JdbcSqliteDriver.IN_MEMORY)

    private fun create(url: String): DatabaseServices {
        val database = lazy {
            JdbcSqliteDriver(url)
                .also { Database.Schema.create(it) }
                .also { Database.Schema.migrate(it, 1, 2)}
                .let { Database(it) }
        }
        return DatabaseStateAccess(
            eventQueries = lazy { database.value.eventQueries },
            siteQueries = lazy { database.value.siteQueries },
            flagQueries = lazy { database.value.flagQueries },
            json = json,
        )
    }
}
