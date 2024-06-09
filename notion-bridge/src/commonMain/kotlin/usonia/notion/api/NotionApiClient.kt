package usonia.notion.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import usonia.client.ktor.PlatformEngine
import usonia.notion.api.structures.NotionBearerToken
import usonia.notion.api.structures.NotionResponse
import usonia.notion.api.structures.database.DatabaseId
import usonia.notion.api.structures.database.DatabaseQuery
import usonia.notion.api.structures.page.NewPage
import usonia.notion.api.structures.page.Page
import usonia.notion.api.structures.page.PageId
import usonia.notion.api.structures.property.PropertyArgument
import usonia.notion.api.structures.property.PropertyName
import kotlin.time.Duration.Companion.seconds

internal class NotionApiClient: NotionApi {
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = HttpClient(PlatformEngine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20.seconds.inWholeMilliseconds
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun queryDatabase(
        token: NotionBearerToken,
        database: DatabaseId,
        query: DatabaseQuery,
    ): NotionResponse.ListResponse<Page> {
        return httpClient.post("https://api.notion.com/v1/databases/${database.value}/query") {
            notionHeaders(token)
            setBody(query)
        }.body()
    }

    override suspend fun createPage(
        token: NotionBearerToken,
        page: NewPage,
    ) {
        val response = httpClient.post("https://api.notion.com/v1/pages") {
            notionHeaders(token)
            setBody(page)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Failed to create page: ${response.status}. ${response.bodyAsText()}")
        }
    }

    override suspend fun updatePage(
        token: NotionBearerToken,
        page: PageId,
        properties: Map<PropertyName, PropertyArgument>
    ) {
        val response = httpClient.patch("https://api.notion.com/v1/pages/${page.value}") {
            notionHeaders(token)
            setBody(mapOf("properties" to properties))
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Failed to update page: ${response.status}. ${response.bodyAsText()}")
        }
    }

    override suspend fun archivePage(
        token: NotionBearerToken,
        page: PageId,
    ) {
        val response = httpClient.patch("https://api.notion.com/v1/pages/${page.value}") {
            notionHeaders(token)
            setBody(mapOf("archived" to true))
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Failed to archive page: ${response.status}. ${response.bodyAsText()}")
        }
    }

    private fun HttpMessageBuilder.notionHeaders(token: NotionBearerToken) {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        header("Authorization", token.value)
        header("Notion-Version", "2022-06-28")
    }
}
