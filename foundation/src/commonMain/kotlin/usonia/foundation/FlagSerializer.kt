package usonia.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Serializes a key/value pair to a json object.
 */
object FlagSerializer: KSerializer<Map<String, String?>> {
    private val backingSerializer = JsonObject.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<String, String?> {
        val json = backingSerializer.deserialize(decoder)

        return json.mapValues {
            when (it.value) {
                JsonNull -> null
                else -> (it.value as JsonPrimitive).content
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String?>) {
        val json = value.mapValues {
            JsonPrimitive(it.value)
        }.let(::JsonObject)

        backingSerializer.serialize(encoder, json)
    }
}
