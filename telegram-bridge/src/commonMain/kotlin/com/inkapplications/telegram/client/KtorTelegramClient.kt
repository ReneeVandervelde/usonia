package com.inkapplications.telegram.client

import com.inkapplications.telegram.structures.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val API_DOMAIN = "api.telegram.org"

internal class KtorTelegramClient(
    private val token: String,
): TelegramBotClient {
    private val client = HttpClient(KtorPlatformModule.engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun setWebhook(parameters: WebhookParameters) {
        post<Boolean>("setWebhook") {
            jsonBody(parameters)
        }
    }

    override suspend fun getWebhookInfo(): WebhookInfo {
        return get("getWebhookInfo")
    }

    override suspend fun sendMessage(parameters: MessageParameters): Message {
        return post("sendMessage") {
            jsonBody(parameters)
        }
    }

    override suspend fun sendSticker(parameters: StickerParameters): Message {
        return post("sendSticker") {
            jsonBody(parameters)
        }
    }

    private suspend inline fun <reified T> get(vararg path: String, builder: HttpRequestBuilder.() -> Unit = {}): T {
        return client.post {
            telegramEndpoint(*path)
            builder()
        }.responseOrThrow()
    }

    private suspend inline fun <reified T> post(vararg path: String, builder: HttpRequestBuilder.() -> Unit): T {
        return client.post {
            telegramEndpoint(*path)
            builder()
        }.responseOrThrow()
    }

    private fun HttpRequestBuilder.telegramEndpoint(vararg path: String) {
        url {
            protocol = URLProtocol.HTTPS
            host = API_DOMAIN
            encodedPathSegments = listOf(token, *path)
        }
    }
    private inline fun <reified T> HttpRequestBuilder.jsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend inline fun <reified T> HttpResponse.responseOrThrow(): T {
        val response = try {
             body<Response<T>>()
        } catch (e: Throwable) {
            throw TelegramException.Internal("Unable to parse response with status ${status.value}")
        }
        return when (response) {
            is Response.Result -> response.data
            is Response.Error -> throw TelegramException.External(response)
            else -> throw TelegramException.Internal("Unknown response type ${response::class.simpleName}>")
        }
    }
}
