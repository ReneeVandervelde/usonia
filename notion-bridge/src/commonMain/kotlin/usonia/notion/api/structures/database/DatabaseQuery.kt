package usonia.notion.api.structures.database

import kotlinx.serialization.Serializable
import usonia.notion.api.structures.page.PageFilter

@Serializable
internal data class DatabaseQuery(
    val filter: PageFilter? = null,
)
