package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import usonia.cli.server.ServerRunCommand
import usonia.cli.client.ClientEventsCommand
import usonia.cli.client.ClientIntentSendCommand
import usonia.cli.client.ClientLogsCommand

@Module
interface CommandModule {
    @Multibinds
    @JvmSuppressWildcards
    fun commands(): Set<CliktCommand>

    @Binds
    @IntoSet
    fun run(command: ServerRunCommand): CliktCommand

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
