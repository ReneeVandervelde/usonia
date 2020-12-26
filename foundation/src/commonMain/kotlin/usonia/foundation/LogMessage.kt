package usonia.foundation

import kimchi.logger.LogLevel
import kotlinx.serialization.Serializable

@Serializable
data class LogMessage(
    val level: LogLevel,
    val message: String,
    val stackTrace: String? = null,
)
