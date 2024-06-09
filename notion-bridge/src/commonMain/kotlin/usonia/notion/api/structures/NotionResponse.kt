package usonia.notion.api.structures

import kotlinx.serialization.Serializable

internal sealed interface NotionResponse {
    @Serializable
    data class ListResponse<T>(
        val results: List<T>,
    ): NotionResponse
}
