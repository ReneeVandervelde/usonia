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
    fun clientLogs(command: LogsCommand): CliktCommand

    @Binds
    @IntoSet
    fun clientEvents(command: EventsListenCommand): CliktCommand

    @Binds
    @IntoSet
    fun intentSend(command: IntentSendCommand): CliktCommand

    @Binds
    @IntoSet
    fun siteUpdate(command: SiteUpdateCommand): CliktCommand

    @Binds
    @IntoSet
    fun setFlag(command: FlagSetCommand): CliktCommand

    @Binds
    @IntoSet
    fun removeFlag(command: FlagRemoveCommand): CliktCommand

    @Binds
    @IntoSet
    fun configDump(command: ConfigurationDumpCommand): CliktCommand
}
