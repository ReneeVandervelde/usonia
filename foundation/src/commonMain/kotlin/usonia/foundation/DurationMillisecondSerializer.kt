package usonia.foundation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Serializes a duration to/from milliseconds.
 */
object DurationMillisecondSerializer: KSerializer<Duration> {
    private val backingSerializer = Long.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor

    override fun deserialize(decoder: Decoder): Duration = backingSerializer.deserialize(decoder).milliseconds
    override fun serialize(encoder: Encoder, value: Duration) = backingSerializer.serialize(encoder, value.inWholeMilliseconds)
}
