package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.Kimchi
import kimchi.logger.KimchiLogger
import kimchi.logger.defaultWriter
import usonia.frontend.LogSocket

@Module
class ExternalModule {
    @Provides
    @Reusable
    fun logger(
        logSocket: LogSocket
    ): KimchiLogger = Kimchi.apply {
        addLog(defaultWriter)
        addLog(logSocket)
    }
}
