package usonia.state

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json

class DatabaseModule(
    private val json: Json,
    private val logger: KimchiLogger,
) {
    fun database(databasePath: String): DatabaseServices = create("jdbc:sqlite:$databasePath")
    fun inMemoryDatabase(): DatabaseServices = create(JdbcSqliteDriver.IN_MEMORY)

    private fun create(url: String): DatabaseServices {
        val database = lazy {
            JdbcSqliteDriver(url)
                .also { Database.Schema.create(it) }
                .also { Database.Schema.migrate(it, 1, 2)}
                .also { Database.Schema.migrate(it, 2, 3)}
                .let { Database(it) }
        }
        return DatabaseStateAccess(
            eventQueries = lazy { database.value.eventQueries },
            siteQueries = lazy { database.value.siteQueries },
            flagQueries = lazy { database.value.flagQueries },
            json = json,
            logger = logger,
        )
    }
}
