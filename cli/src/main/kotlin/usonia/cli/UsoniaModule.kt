package usonia.cli

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import usonia.core.state.*
import usonia.core.state.memory.InMemoryActionAccess
import usonia.core.state.memory.InMemoryEventAccess
import usonia.hue.HueArchetypes
import usonia.schlage.SchlageArchetypes
import usonia.serialization.SiteSerializer
import usonia.smartthings.SmartThingsArchetypes
import javax.inject.Singleton

@Module(includes = [UsoniaBindings::class])
object UsoniaModule {
    @Provides
    @Reusable
    fun siteSerializer(): SiteSerializer {
        val archetypes = setOf(
            *SmartThingsArchetypes.ALL.toTypedArray(),
            HueArchetypes.group,
            SchlageArchetypes.connectLock,
        )
        return SiteSerializer(archetypes)
    }

    @Provides
    @Reusable
    fun configurationAccess(
        siteSerializer: SiteSerializer
    ): ConfigurationAccess {
        return FileConfigAccess(siteSerializer)
    }

    @Provides
    @Singleton
    fun inMemoryEvents(
        config: ConfigurationAccess
    ) = InMemoryEventAccess(config)

    @Provides
    @Singleton
    fun inMemoryActions() = InMemoryActionAccess()
}

@Module
private interface UsoniaBindings {
    @Binds
    fun eventAccess(source: InMemoryEventAccess): EventAccess

    @Binds
    fun eventPublisher(source: InMemoryEventAccess): EventPublisher

    @Binds
    fun actionPublisher(source: InMemoryActionAccess): ActionPublisher

    @Binds
    fun actionAccess(source: InMemoryActionAccess): ActionAccess
}
