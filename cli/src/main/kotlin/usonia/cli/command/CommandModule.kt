package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
abstract class CommandModule {
    @Binds
    @IntoSet
    abstract fun helloCommand(command: HelloCommand): CliktCommand
}
