package usonia.server.http

data class RestResponse<T>(
    val data: T,
    val status: Int = 200
)
