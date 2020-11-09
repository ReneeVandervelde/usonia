package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Component
import usonia.cli.command.CommandModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    CommandModule::class
])
interface CliComponent {
    fun getCommands(): @JvmSuppressWildcards Set<CliktCommand>
}
