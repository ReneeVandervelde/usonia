package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.Kimchi
import kimchi.logger.KimchiLogger
import kimchi.logger.defaultWriter

@Module
class ExternalModule {
    @Provides
    @Reusable
    fun logger(): KimchiLogger = Kimchi.apply {
        addLog(defaultWriter)
    }
}
