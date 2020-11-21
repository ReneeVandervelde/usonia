package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import usonia.core.LogSocket

@Module
object ExternalModule {
    @Provides
    @Reusable
    fun logger(): KimchiLogger {
        return setOf(LogSocket, ColorWriter)
            .let(::CompositeLogWriter)
            .let(::ConsolidatedLogger)
    }

    @Provides
    @Reusable
    @Client
    fun clientLogger(): KimchiLogger {
        return setOf(ColorWriter)
            .let(::CompositeLogWriter)
            .let(::ConsolidatedLogger)
    }
}

