package usonia.serialization

import kimchi.logger.LogLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.LogMessage

object LogMessageSerializer: KSerializer<LogMessage> {
    private val serializer = LogMessageJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): LogMessage {
        val json = decoder.decodeSerializableValue(serializer)

        return LogMessage(
            level = LogLevel.valueOf(json.level),
            message = json.message,
            stackTrace = json.stackTrace,
        )
    }

    override fun serialize(encoder: Encoder, value: LogMessage) {
        val json = LogMessageJson(
            level = value.level.name,
            message = value.message,
            stackTrace = value.stackTrace,
        )

        encoder.encodeSerializableValue(serializer, json)
    }
}

@Serializable
internal class LogMessageJson(
    val level: String,
    val message: String,
    val stackTrace: String? = null,
)
