package usonia.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A Universally unique Identifier String.
 */
@Serializable(with = IdSerializer::class)
data class Identifier(val value: String)

internal object IdSerializer: KSerializer<Identifier> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): Identifier {
        return Identifier(String.serializer().deserialize(decoder))
    }

    override fun serialize(encoder: Encoder, value: Identifier) {
        String.serializer().serialize(encoder, value.value)
    }
}
