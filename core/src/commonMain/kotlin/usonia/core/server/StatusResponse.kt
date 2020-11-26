package usonia.core.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StatusResponse(
    val success: Boolean
)

val SUCCESS = RestResponse(StatusResponse(true))

@Serializable
data class ErrorResponse(
    val message: String
)

fun errorResponseBody(message: String) = ErrorResponse(message).let {
    Json.encodeToString(ErrorResponse.serializer(), it)
}
