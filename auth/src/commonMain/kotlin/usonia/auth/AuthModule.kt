package usonia.auth

import regolith.init.Initializer

object AuthModule {
    val initializers: List<Initializer> = listOf(LibSodiumInitializer)
}
