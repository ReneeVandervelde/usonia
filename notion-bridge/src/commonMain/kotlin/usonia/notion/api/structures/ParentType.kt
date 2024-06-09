package usonia.notion.api.structures

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
internal value class ParentType(val value: String) {
    companion object {
        val DatabaseId = ParentType("database_id")
    }
}
