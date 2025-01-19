package com.inkapplications.telegram.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Serialize a duration as an integer of seconds.
 */
object IntSecondsDurationSerializer: KSerializer<Duration> {
    private val backingSerializer = Int.serializer()
    override val descriptor: SerialDescriptor = backingSerializer.descriptor
    override fun deserialize(decoder: Decoder): Duration = backingSerializer.deserialize(decoder).seconds
    override fun serialize(encoder: Encoder, value: Duration) = backingSerializer.serialize(encoder, value.inWholeSeconds.toInt())
}
