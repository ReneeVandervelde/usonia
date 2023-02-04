package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.json.Json
import usonia.cli.CliComponent
import usonia.foundation.Site
import javax.inject.Inject

/**
 * Uploads a file as a new site configuration.
 */
class SiteUpdateCommand @Inject constructor(
    component: CliComponent,
    private val json: Json,
): ClientCommand(
    component = component,
    help = "Update a server's site configuration",
) {
    private val configFile by argument(
        name = "file",
        help = "File to load a site configuration from",
    ).file(mustExist = true, mustBeReadable = true, canBeDir = false)

    override suspend fun execute() {
        val site = json.decodeFromString(Site.serializer(), configFile.readText())
        client.updateSite(site)
    }
}
