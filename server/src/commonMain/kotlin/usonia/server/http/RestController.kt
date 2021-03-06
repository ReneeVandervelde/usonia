package usonia.server.http

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.foundation.Status

abstract class RestController<IN, OUT>(
    protected val json: Json,
    protected val logger: KimchiLogger = EmptyLogger,
): HttpController {
    abstract val serializer: KSerializer<OUT>
    abstract val deserializer: KSerializer<IN>

    abstract suspend fun getResponse(data: IN, request: HttpRequest): RestResponse<OUT>

    final override suspend fun getResponse(request: HttpRequest): HttpResponse {
        val data = try {
            json.decodeFromString(deserializer, request.body!!)
        } catch (error: Throwable) {
            logger.error("Failed to decode request body", error)
            return HttpResponse(
                body = encodedString(2, "Failed to decode request body"),
                contentType = "text/json",
                status = 400,
            )
        }

        try {
            val response = getResponse(data, request)

            return HttpResponse(
                body = json.encodeToString(serializer, response.data),
                contentType = "application/json",
                status = response.status,
            )
        } catch (e: CancellationException) {
            logger.warn("Cancelled while generating response", e)
            throw e
        } catch (error: Throwable) {
            logger.error("Failed generating response body.", error)
            return HttpResponse(
                body = encodedString(3, "Internal error generating response."),
                contentType = "applicationjson",
                status = 500,
            )
        }
    }

    private fun encodedString(
        code: Int,
        message: String
    ) = Status(code, message).let { Json.encodeToString(Status.serializer(), it) }
}

