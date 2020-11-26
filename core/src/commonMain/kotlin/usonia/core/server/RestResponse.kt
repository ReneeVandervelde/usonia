package usonia.core.server

data class RestResponse<T>(
    val data: T,
    val status: Int = 200
)
