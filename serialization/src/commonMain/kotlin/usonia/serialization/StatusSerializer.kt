package usonia.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import usonia.foundation.Status

object StatusSerializer: KSerializer<Status> {
    private val serializer = StatusJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Status {
        val json = decoder.decodeSerializableValue(serializer)

        return Status(json.code, json.message)
    }

    override fun serialize(encoder: Encoder, value: Status) {
        val json = StatusJson(value.code, value.message)

        encoder.encodeSerializableValue(serializer, json)
    }

    fun encodedString(
        code: Int,
        message: String
    ) = Status(code, message).let { Json.encodeToString(this, it) }
}

@Serializable
internal data class StatusJson(val code: Int, val message: String)
