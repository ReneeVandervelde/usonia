package usonia.notion.api.structures.block

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RichTextSerializer::class)
internal sealed interface RichText
{
    data class Text(
        val plain_text: String,
        val text: TextContent,
    ): RichText {
        @Serializable
        data class TextContent(
            val content: String,
            val link: String? = null,
        )
    }
}

internal class RichTextSerializer: KSerializer<RichText>
{
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RichText) = TODO("Not implemented")

    override fun deserialize(decoder: Decoder): RichText
    {
        val surrogate = Surrogate.serializer().deserialize(decoder)
        return when (surrogate.type) {
            RichTextType.Text -> RichText.Text(
                plain_text = surrogate.plain_text ?: error("Missing plain_text"),
                text = surrogate.text ?: error("Missing text content"),
            )
            else -> error("Unknown block type")
        }
    }

    @Serializable
    private data class Surrogate(
        val type: RichTextType,
        val plain_text: String? = null,
        val text: RichText.Text.TextContent? = null,
    )
}
