package usonia.foundation

import inkapplications.spondee.measure.ColorTemperature
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.structure.roundToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ColorTemperatureKelvinRoundedIntSerializer: KSerializer<ColorTemperature>
{
    override val descriptor: SerialDescriptor = Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): ColorTemperature
    {
        return decoder.decodeInt().kelvin
    }

    override fun serialize(encoder: Encoder, value: ColorTemperature)
    {
        return encoder.encodeInt(value.toKelvin().roundToInt())
    }
}
