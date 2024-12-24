package usonia.server.http

class BodyDecodeFailure(cause: Throwable? = null): IllegalArgumentException(
    "Failed to decode request body",
    cause,
)
