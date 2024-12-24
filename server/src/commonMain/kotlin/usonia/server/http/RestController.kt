package usonia.server.http

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.foundation.Status
import usonia.foundation.Statuses

abstract class RestController<IN, OUT>(
    protected val json: Json,
    protected val logger: KimchiLogger = EmptyLogger,
): HttpController {
    abstract val serializer: KSerializer<OUT>
    abstract val deserializer: KSerializer<IN>

    open suspend fun requiresAuthorization(data: IN, request: HttpRequest): Boolean {
        return true
    }
    abstract suspend fun getResponse(data: IN, request: HttpRequest): RestResponse<OUT>

    final override suspend fun requiresAuthorization(request: HttpRequest): Boolean {
        val data = getData(request)
        return requiresAuthorization(data, request)
    }

    final override suspend fun getResponse(request: HttpRequest): HttpResponse {
        val data = getData(request)

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
                body = Statuses.UNKNOWN.let { Json.encodeToString(Status.serializer(), it) },
                contentType = "application/json",
                status = 500,
            )
        }
    }

    private fun getData(request: HttpRequest): IN {
        return try {
            json.decodeFromString(deserializer, request.body!!)
        } catch (error: Throwable) {
            logger.error("Failed to decode request body", error)
            throw BodyDecodeFailure(error)
        }
    }

    private fun encodedString(
        code: Int,
        message: String
    ) = Status(code, message).let { Json.encodeToString(Status.serializer(), it) }
}

