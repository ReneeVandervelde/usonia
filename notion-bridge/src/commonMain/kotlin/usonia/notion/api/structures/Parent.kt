package usonia.notion.api.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.notion.api.structures.database.DatabaseId

@Serializable(with = ParentSerializer::class)
internal sealed interface Parent {
    data class Database(
        val database_id: DatabaseId,
    ): Parent
}

internal object ParentSerializer: KSerializer<Parent> {
    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Parent {
        val surrogate = Surrogate.serializer().deserialize(decoder)
        return when(surrogate.type) {
            ParentType.DatabaseId -> Parent.Database(
                database_id = surrogate.database_id ?: error("database_id property must be present"),
            )
            else -> error("Unknown parent type")
        }
    }

    override fun serialize(encoder: Encoder, value: Parent) {
        val surrogate = when (value) {
            is Parent.Database -> Surrogate(
                type = ParentType.DatabaseId,
                database_id = value.database_id,
            )
        }
        Surrogate.serializer().serialize(encoder, surrogate)
    }

    @Serializable
    private data class Surrogate(
        val type: ParentType,
        val database_id: DatabaseId? = null,
    )
}
