package usonia.notion.api.structures.block

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BlockSerializer::class)
internal sealed interface Block {
    data class RichText(
        val plain_text: String,
        val text: Text,
    ): Block {
        @Serializable
        data class Text(
            val content: String,
            val link: String? = null,
        )
    }
}


internal class BlockSerializer: KSerializer<Block> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Block) = TODO("Not implemented")

    override fun deserialize(decoder: Decoder): Block {
        val surrogate = Surrogate.serializer().deserialize(decoder)
        return when (surrogate.type) {
            BlockType.Text -> Block.RichText(
                plain_text = surrogate.plain_text ?: error("Missing plain_text"),
                text = surrogate.text ?: error("Missing text content"),
            )
            else -> error("Unknown block type")
        }
    }

    @Serializable
    private data class Surrogate(
        val type: BlockType,
        val plain_text: String? = null,
        val text: Block.RichText.Text? = null,
    )
}
