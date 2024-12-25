package usonia.notion.api.structures.block

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BlockArgumentSerializer::class)
internal sealed interface BlockArgument
{
    @Serializable
    data class Paragraph(
        @SerialName("rich_text")
        val richText: List<RichTextArgument>
    ): BlockArgument

    @Serializable
    data class Code(
        val language: CodeLanguage,
        @SerialName("rich_text")
        val content: List<RichTextArgument>
    ): BlockArgument
}

internal class BlockArgumentSerializer: KSerializer<BlockArgument>
{
    override val descriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): BlockArgument = TODO()

    override fun serialize(encoder: Encoder, value: BlockArgument)
    {
        val surrogate = when (value)
        {
            is BlockArgument.Paragraph -> Surrogate(
                type = BlockType.PARAGRAPH,
                paragraph = value,
            )
            is BlockArgument.Code -> Surrogate(
                type = BlockType.CODE,
                code = value,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        @SerialName("object")
        val objectType: String = "block",
        val type: BlockType,
        val paragraph: BlockArgument.Paragraph? = null,
        val code: BlockArgument.Code? = null,
    )
}
