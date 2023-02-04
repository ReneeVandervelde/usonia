package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import usonia.cli.CliComponent
import usonia.foundation.Site
import usonia.kotlin.first
import usonia.serialization.SerializationModule
import usonia.state.DatabaseModule
import javax.inject.Inject

class ConfigurationDumpCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    help = "Dump site json from a database file."
) {
    private val database by argument().file(canBeDir = false)
    private val backendModule get() = DatabaseModule(
        json = SerializationModule.json,
    )

    override suspend fun execute() {
        backendModule.database(database.absolutePath)
            .site
            .first()
            .let { SerializationModule.json.encodeToString(Site.serializer(), it) }
            .run { echo(this) }
    }
}
