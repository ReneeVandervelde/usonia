package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import usonia.cli.client.*
import usonia.cli.server.ServerRunCommand

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

    @Binds
    @IntoSet
    fun siteUpdate(command: SiteUpdateCommand): CliktCommand

    @Binds
    @IntoSet
    fun setFlag(command: SetFlagCommand): CliktCommand

    @Binds
    @IntoSet
    fun removeFlag(command: RemoveFlagCommand): CliktCommand
}
