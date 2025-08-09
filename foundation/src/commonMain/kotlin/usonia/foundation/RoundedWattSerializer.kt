package usonia.foundation

import inkapplications.spondee.measure.Power
import inkapplications.spondee.measure.metric.watts
import inkapplications.spondee.structure.roundToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class RoundedWattSerializer: KSerializer<Power>
{
    override val descriptor: SerialDescriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Power)
    {
        encoder.encodeInt(value.toWatts().roundToInt())
    }

    override fun deserialize(decoder: Decoder): Power
    {
        return decoder.decodeInt().watts
    }
}
