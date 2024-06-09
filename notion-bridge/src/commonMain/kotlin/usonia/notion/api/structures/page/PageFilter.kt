package usonia.notion.api.structures.page

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.notion.api.structures.property.PropertyName

@Serializable(with = PageFilterSerializer::class)
internal sealed interface PageFilter {
    data class Select(
        val property: PropertyName,
        val select: FilterQuery,
    ): PageFilter

    data class MultiSelect(
        val property: PropertyName,
        val multi_select: FilterQuery,
    ): PageFilter

    data class Text(
        val property: PropertyName,
        val text: TextFilter? = null,
    ): PageFilter

    data class Or(
        val filters: List<PageFilter>,
    ): PageFilter

    data class And(
        val filters: List<PageFilter>,
    ): PageFilter
}

@Serializable(with = TextFilterSerializer::class)
sealed interface TextFilter {
    data class Equals(
        val equals: String,
    ): TextFilter

    data class Empty(
        val empty: Boolean,
    ): TextFilter
}

internal object TextFilterSerializer: KSerializer<TextFilter> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): TextFilter = TODO()

    override fun serialize(encoder: Encoder, value: TextFilter) {
        val surrogate = when (value) {
            is TextFilter.Equals -> Surrogate(
                equals = value.equals,
            )
            is TextFilter.Empty -> if (value.empty) Surrogate(
                is_empty = true,
            ) else Surrogate(
                is_not_empty = true,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        val equals: String? = null,
        val is_empty: Boolean? = null,
        val is_not_empty: Boolean? = null,
    )
}

internal object PageFilterSerializer: KSerializer<PageFilter> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): PageFilter = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: PageFilter) {
        val surrogate = when (value) {
            is PageFilter.Select -> Surrogate(
                property = value.property,
                select = value.select,
            )
            is PageFilter.MultiSelect -> Surrogate(
                property = value.property,
                multi_select = value.multi_select,
            )
            is PageFilter.Text -> Surrogate(
                property = value.property,
                rich_text = value.text,
            )
            is PageFilter.Or -> Surrogate(
                or = value.filters,
            )
            is PageFilter.And -> Surrogate(
                and = value.filters,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        val property: PropertyName? = null,
        val multi_select: FilterQuery? = null,
        val select: FilterQuery? = null,
        val rich_text: TextFilter? = null,
        val or: List<PageFilter>? = null,
        val and: List<PageFilter>? = null,
    )
}
