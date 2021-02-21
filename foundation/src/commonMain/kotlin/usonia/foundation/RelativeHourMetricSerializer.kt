package usonia.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializes a key/value pair to a json object.
 */
object RelativeHourMetricSerializer: KSerializer<Map<Int, Float>> {
    private val backingSerializer = JsonObject.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<Int, Float> {
        val json = backingSerializer.deserialize(decoder)

        return json.mapValues {
            it.value.jsonPrimitive.float
        }.mapKeys {
            it.key.toInt()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<Int, Float>) {
        val json = value
            .mapValues { JsonPrimitive(it.value) }
            .mapKeys { it.key.toString() }
            .let(::JsonObject)

        backingSerializer.serialize(encoder, json)
    }
}
