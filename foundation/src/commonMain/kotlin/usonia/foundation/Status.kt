package usonia.foundation

import kotlinx.serialization.Serializable

@Serializable
data class Status(
    val code: Int,
    val message: String,
)

object Statuses {
    val SUCCESS = Status(
        code = 0,
        message = "We're fine. We're all fine here, now. Thank you. How are you?",
    )

    val UNKNOWN = Status(
        code = 1,
        message = "Something unexpected went wrong.",
    )

    val DEVICE_NOT_FOUND = Status(
        code = 2,
        message = "Could not find a device with the specified ID.",
    )

    fun missingRequired(param: String) = Status(
        code = 3,
        message = "Missing required Parameter <$param>",
    )

    fun illegalArgument(param: String) = Status(
        code = 4,
        message = "Illegal Argument for <$param>",
    )

    val EVENT_NOT_FOUND = Status(
        message = "No events were found.",
        code = 5,
    )

    val BRIDGE_NOT_FOUND = Status(
        message = "Could not find a bridge with the specified ID",
        code = 6,
    )

    fun bridgeNotConfigured(bridgeId: Identifier) = Status(
        code = 7,
        message = "Bridge $bridgeId is not configured properly",
    )
}
