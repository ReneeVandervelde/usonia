package usonia.notion.api.structures.block

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
internal value class RichTextType(val value: String)
{
    companion object
    {
        val Text = RichTextType("text")
    }
}
