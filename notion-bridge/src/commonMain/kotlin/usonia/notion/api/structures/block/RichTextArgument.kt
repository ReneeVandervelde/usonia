package usonia.notion.api.structures.block

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RichTextArgumentSerializer::class)
internal sealed interface RichTextArgument
{
    data class Text(
        val text: TextContent
    ): RichTextArgument {
        @Serializable
        data class TextContent(
            val content: String,
            val link: String? = null,
        )
    }
}

internal class RichTextArgumentSerializer: KSerializer<RichTextArgument>
{
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): RichTextArgument = TODO("Not implemented")

    override fun serialize(encoder: Encoder, value: RichTextArgument)
    {
        val surrogate = when (value) {
            is RichTextArgument.Text -> Surrogate(
                text = value.text,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        val text: RichTextArgument.Text.TextContent? = null,
    )
}
