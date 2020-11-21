package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Component
import usonia.cli.command.CommandModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        ExternalModule::class,
        CommandModule::class,
    ]
)
interface CliComponent {
    @JvmSuppressWildcards
    fun commands(): Set<CliktCommand>
}
