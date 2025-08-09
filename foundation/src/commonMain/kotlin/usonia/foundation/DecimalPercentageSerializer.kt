package usonia.foundation

import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.decimalPercentage
import inkapplications.spondee.structure.toFloat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DecimalPercentageSerializer: KSerializer<Percentage>
{
    override val descriptor: SerialDescriptor = Float.serializer().descriptor

    override fun deserialize(decoder: Decoder): Percentage
    {
        return decoder.decodeFloat().decimalPercentage
    }

    override fun serialize(encoder: Encoder, value: Percentage)
    {
        return encoder.encodeFloat(value.toDecimal().toFloat())
    }
}
