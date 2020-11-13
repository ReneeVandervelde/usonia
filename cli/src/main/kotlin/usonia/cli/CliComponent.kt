package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Component
import usonia.cli.command.CommandModule
import usonia.cli.server.ServerModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    CommandModule::class,
    ServerModule::class,
])
interface CliComponent {
    fun getCommands(): @JvmSuppressWildcards Set<CliktCommand>
}
