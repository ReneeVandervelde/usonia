package usonia.notion.api

import usonia.notion.api.structures.NotionBearerToken
import usonia.notion.api.structures.NotionResponse
import usonia.notion.api.structures.database.DatabaseId
import usonia.notion.api.structures.database.DatabaseQuery
import usonia.notion.api.structures.page.NewPage
import usonia.notion.api.structures.page.Page
import usonia.notion.api.structures.page.PageId
import usonia.notion.api.structures.property.PropertyArgument
import usonia.notion.api.structures.property.PropertyName

internal interface NotionApi {
    suspend fun queryDatabase(
        token: NotionBearerToken,
        database: DatabaseId,
        query: DatabaseQuery,
    ): NotionResponse.ListResponse<Page>

    suspend fun createPage(
        token: NotionBearerToken,
        page: NewPage,
    )

    suspend fun updatePage(
        token: NotionBearerToken,
        page: PageId,
        properties: Map<PropertyName, PropertyArgument>
    )

    suspend fun archivePage(
        token: NotionBearerToken,
        page: PageId,
    )
}
