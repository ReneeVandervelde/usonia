package usonia.cli.client

import usonia.auth.Auth
import usonia.client.AuthenticationProvider
import java.io.File
import java.util.*

object PropertiesAuthProvider: AuthenticationProvider {
    private val file by lazy {
        File(
            System.getProperty("user.home").takeIf { it.isNullOrEmpty() == false },
            ".usonia-cli.properties"
        )
    }
    private val properties by lazy {
        Properties().apply {
            if (file.exists()) file.inputStream().run(::load)
        }
    }

    override val auth: Auth.Psk? by lazy {
        properties.getProperty("auth.psk")?.let { Auth.Psk(it) }
    }

    override val bridgeIdentifier = "home.bridges.cli"
}
