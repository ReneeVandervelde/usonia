package usonia.foundation

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * Serializes a key/value pair to a json object.
 */
object DateMetricSerializer: KSerializer<Map<LocalDate, Int>> {
    private val backingSerializer = JsonObject.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<LocalDate, Int> {
        val json = backingSerializer.deserialize(decoder)

        return json.mapValues {
            it.value.jsonPrimitive.int
        }.mapKeys {
            LocalDate.parse(it.key)
        }
    }

    override fun serialize(encoder: Encoder, value: Map<LocalDate, Int>) {
        val json = value
            .mapValues { JsonPrimitive(it.value) }
            .mapKeys { it.key.toString() }
            .let(::JsonObject)

        backingSerializer.serialize(encoder, json)
    }
}
