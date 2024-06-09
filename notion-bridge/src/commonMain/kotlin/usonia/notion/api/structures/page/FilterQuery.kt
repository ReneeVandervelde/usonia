package usonia.notion.api.structures.page

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = FilterQuerySerializer::class)
internal sealed interface FilterQuery {
    data class Contains(
        val contains: String,
    ): FilterQuery
}

internal object FilterQuerySerializer: KSerializer<FilterQuery> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): FilterQuery = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: FilterQuery) {
        val surrogate = when (value) {
            is FilterQuery.Contains -> Surrogate(
                contains = value.contains,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        @SerialName("contains")
        val contains: String? = null,
    )
}
