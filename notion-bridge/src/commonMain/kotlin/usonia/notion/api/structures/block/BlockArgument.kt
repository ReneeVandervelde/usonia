package usonia.notion.api.structures.block

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BlockArgumentSerializer::class)
internal sealed interface BlockArgument {
    data class RichText(
        val text: Text
    ): BlockArgument {
        @Serializable
        data class Text(
            val content: String,
            val link: String? = null,
        )
    }
}

internal class BlockArgumentSerializer: KSerializer<BlockArgument> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): BlockArgument = TODO("Not implemented")

    override fun serialize(encoder: Encoder, value: BlockArgument) {
        val surrogate = when (value) {
            is BlockArgument.RichText -> Surrogate(
                text = value.text,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        val text: BlockArgument.RichText.Text? = null,
    )
}
