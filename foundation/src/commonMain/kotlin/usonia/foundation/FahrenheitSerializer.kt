package usonia.foundation

import inkapplications.spondee.measure.Temperature
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.structure.toFloat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes a temperature to/from a float in degrees fahrenheit.
 */
object FahrenheitSerializer: KSerializer<Temperature> {
    private val backingSerializer = Float.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor
    override fun deserialize(decoder: Decoder): Temperature {
        return backingSerializer.deserialize(decoder).fahrenheit
    }

    override fun serialize(encoder: Encoder, value: Temperature) {
        backingSerializer.serialize(encoder, value.toFahrenheit().toFloat())
    }
}
