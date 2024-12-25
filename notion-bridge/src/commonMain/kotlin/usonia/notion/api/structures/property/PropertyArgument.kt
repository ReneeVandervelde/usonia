package usonia.notion.api.structures.property

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.notion.api.structures.block.RichTextArgument

@Serializable(with = PropertyArgumentSerializer::class)
internal sealed interface PropertyArgument {
    data class MultiSelect(
        val multi_select: List<MultiSelectArgument>
    ): PropertyArgument
    data class Select(
        val select: SelectArgument
    ): PropertyArgument
    data class Title(
        val title: List<usonia.notion.api.structures.block.RichTextArgument>,
    ): PropertyArgument
    data class RichText(
        val rich_text: List<usonia.notion.api.structures.block.RichTextArgument>,
    ): PropertyArgument
    data class Status(
        val status: StatusArgument,
    ): PropertyArgument
}

internal class PropertyArgumentSerializer: KSerializer<PropertyArgument> {
    @Serializable
    private data class Surrogate(
        val multi_select: List<MultiSelectArgument>? = null,
        val select: SelectArgument? = null,
        val title: List<RichTextArgument>? = null,
        val rich_text: List<RichTextArgument>? = null,
        val status: StatusArgument? = null,
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): PropertyArgument {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: PropertyArgument) {
        val surrogate = when (value) {
            is PropertyArgument.MultiSelect -> Surrogate(
                multi_select = value.multi_select,
            )
            is PropertyArgument.Select -> Surrogate(
                select = value.select,
            )
            is PropertyArgument.Title -> Surrogate(
                title = value.title,
            )
            is PropertyArgument.RichText -> Surrogate(
                rich_text = value.rich_text,
            )
            is PropertyArgument.Status -> Surrogate(
                status = value.status,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

}
