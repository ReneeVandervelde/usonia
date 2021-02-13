package usonia.foundation

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer: KSerializer<Instant> {
    private val backingSerializer = Long.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor

    override fun deserialize(decoder: Decoder): Instant {
        return backingSerializer.deserialize(decoder).let(Instant::fromEpochMilliseconds)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        backingSerializer.serialize(encoder, value.toEpochMilliseconds())
    }
}
