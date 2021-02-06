package usonia.frontend.flags

sealed class FlagViewModel {
    abstract val key: String

    data class StringFlag(
        override val key: String,
        val value: String,
    ): FlagViewModel()

    data class DisabledFlag(
        override val key: String,
    ): FlagViewModel()

    data class EnabledFlag(
        override val key: String,
    ): FlagViewModel()
}

fun createFlagViewModel(key: String, value: String?): FlagViewModel {
    return when (value) {
        "true" -> FlagViewModel.EnabledFlag(key)
        "false" -> FlagViewModel.DisabledFlag(key)
        else -> FlagViewModel.StringFlag(key, value ?: "null")
    }
}
