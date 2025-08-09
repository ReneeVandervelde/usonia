package usonia.foundation

import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.WholePercentage
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.toFloat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class WholePercentageSerializer: KSerializer<Percentage>
{
    override val descriptor: SerialDescriptor = Float.serializer().descriptor

    override fun deserialize(decoder: Decoder): Percentage
    {
        return WholePercentage(decoder.decodeFloat())
    }

    override fun serialize(encoder: Encoder, value: Percentage)
    {
        return encoder.encodeFloat(value.toWholePercentage().toFloat())
    }
}
