package usonia.foundation

import kimchi.logger.LogLevel

data class LogMessage(
    val level: LogLevel,
    val message: String,
    val stackTrace: String?,
)
