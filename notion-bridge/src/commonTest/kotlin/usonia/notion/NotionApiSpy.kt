package usonia.notion

import usonia.notion.api.NotionApi
import usonia.notion.api.structures.NotionBearerToken
import usonia.notion.api.structures.NotionResponse
import usonia.notion.api.structures.database.DatabaseId
import usonia.notion.api.structures.database.DatabaseQuery
import usonia.notion.api.structures.page.NewPage
import usonia.notion.api.structures.page.Page
import usonia.notion.api.structures.page.PageId
import usonia.notion.api.structures.property.PropertyArgument
import usonia.notion.api.structures.property.PropertyName

internal class NotionApiSpy: NotionApi {
    val queries = mutableListOf<DatabaseQuery>()
    val createdPages = mutableListOf<NewPage>()
    val archivedPages = mutableListOf<PageId>()
    val updatedPages = mutableListOf<Pair<PageId, Map<PropertyName, PropertyArgument>>>()

    override suspend fun queryDatabase(token: NotionBearerToken, database: DatabaseId, query: DatabaseQuery): NotionResponse.ListResponse<Page> {
        queries.add(query)
        return NotionResponse.ListResponse(emptyList())
    }

    override suspend fun createPage(token: NotionBearerToken, page: NewPage) {
        createdPages.add(page)
    }

    override suspend fun updatePage(
        token: NotionBearerToken,
        page: PageId,
        properties: Map<PropertyName, PropertyArgument>
    ) {
        updatedPages.add(page to properties)
    }

    override suspend fun archivePage(token: NotionBearerToken, page: PageId) {
        archivedPages.add(page)
    }
}

internal fun NotionApi.withFakeQueryResponse(response: NotionResponse.ListResponse<Page>) = object : NotionApi by this {
    override suspend fun queryDatabase(token: NotionBearerToken, database: DatabaseId, query: DatabaseQuery): NotionResponse.ListResponse<Page> {
        this@withFakeQueryResponse.queryDatabase(token, database, query)
        return response
    }
}
