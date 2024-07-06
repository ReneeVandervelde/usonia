package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import usonia.foundation.Site
import usonia.kotlin.first
import usonia.serialization.SerializationModule
import usonia.state.DatabaseModule
import java.io.File

class ConfigurationDumpCommand(
    module: ClientModule,
): ClientCommand(
    clientModule = module,
    help = "Dump site json from a database file."
) {
    private val database by argument().file(canBeDir = false)
    private val settings by option().file(canBeDir = false)
        .default(File("settings.db"))
    private val backendModule get() = DatabaseModule(
        json = SerializationModule.json,
        logger = logger,
        settingsFile = settings,
    )

    override suspend fun execute() {
        backendModule.database(database.absolutePath)
            .site
            .first()
            .let { SerializationModule.json.encodeToString(Site.serializer(), it) }
            .run { echo(this) }
    }
}
