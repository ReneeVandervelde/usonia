package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

@Module
interface CommandModule {
    @Multibinds
    @JvmSuppressWildcards
    fun commands(): Set<CliktCommand>

    @Binds
    @IntoSet
    fun run(command: RunCommand): CliktCommand

    @Binds
    @IntoSet
    fun clientLogs(command: ClientLogsCommand): CliktCommand

    @Binds
    @IntoSet
    fun clientEvents(command: ClientEventsCommand): CliktCommand

    @Binds
    @IntoSet
    fun intentSend(command: ClientIntentSendCommand): CliktCommand
}
