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
        val filter: FilterQuery,
    ): PageFilter

    data class Status(
        val property: PropertyName,
        val filter: FilterQuery,
    ): PageFilter

    data class MultiSelect(
        val property: PropertyName,
        val filter: FilterQuery,
    ): PageFilter

    data class Text(
        val property: PropertyName,
        val filter: TextFilter? = null,
    ): PageFilter

    data class Or(
        val filters: List<PageFilter>,
    ): PageFilter

    data class And(
        val filters: List<PageFilter>,
    ): PageFilter
}

internal class PageFilterSerializer: KSerializer<PageFilter> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): PageFilter = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: PageFilter) {
        val surrogate = when (value) {
            is PageFilter.Select -> Surrogate(
                property = value.property,
                select = value.filter,
            )
            is PageFilter.MultiSelect -> Surrogate(
                property = value.property,
                multi_select = value.filter,
            )
            is PageFilter.Status -> Surrogate(
                property = value.property,
                status = value.filter,
            )
            is PageFilter.Text -> Surrogate(
                property = value.property,
                rich_text = value.filter,
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
        val status: FilterQuery? = null,
        val rich_text: TextFilter? = null,
        val or: List<PageFilter>? = null,
        val and: List<PageFilter>? = null,
    )
}
