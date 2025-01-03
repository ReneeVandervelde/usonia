package usonia.notion.api.structures.block

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
internal value class BlockType(val value: String)
{
    companion object
    {
        val PARAGRAPH = BlockType("paragraph")
        val CODE = BlockType("code")
    }
}
