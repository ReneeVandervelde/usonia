package usonia.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SecurityState.Serializer::class)
enum class SecurityState {
    Armed,
    Disarmed;

    internal class Serializer : KSerializer<SecurityState> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SecurityState", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: SecurityState) = encoder.encodeString(value.name)
        override fun deserialize(decoder: Decoder): SecurityState = valueOf(decoder.decodeString())
    }
}
