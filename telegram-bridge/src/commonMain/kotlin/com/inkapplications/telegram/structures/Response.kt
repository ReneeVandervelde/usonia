package com.inkapplications.telegram.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Wraps responses for the API.
 */
@Serializable(with = Response.Serializer::class)
abstract class Response<out T> private constructor() {
    data class Error(
        val code: Int,
        val description: String? = null,
    ): Response<Nothing>()

    data class Result<T>(
        val data: T,
        val description: String? = null,
    ): Response<T>()

    @Serializable
    internal data class PolymorphicResponse<T>(
        val ok: Boolean,
        @SerialName("error_code")
        val code: Int? = null,
        val description: String? = null,
        val result: T? = null,
    )

    internal class Serializer<T>(
        private val responseTypeSerializer: KSerializer<T>
    ): KSerializer<Response<T>> {
        private val responseSerializer = PolymorphicResponse.serializer(responseTypeSerializer)
        override val descriptor: SerialDescriptor = responseSerializer.descriptor

        override fun deserialize(decoder: Decoder): Response<T> {
            val polymorphic = responseSerializer.deserialize(decoder)

            if (polymorphic.ok && polymorphic.result != null) {
                return Result(
                    data = polymorphic.result,
                    description = polymorphic.description,
                )
            }
            if (!polymorphic.ok && polymorphic.code != null) {
                return Error(
                    code = polymorphic.code,
                    description = polymorphic.description,
                )
            }

            throw IllegalStateException("Unable to parse response without a result nor a code")
        }

        override fun serialize(encoder: Encoder, value: Response<T>) {
            PolymorphicResponse(
                ok = value is Result,
                code = (value as? Error)?.code,
                description = (value as? Result)?.description ?: (value as? Error)?.description,
                result = (value as? Result<T>)?.data,
            ).let { responseSerializer.serialize(encoder, it) }
        }
    }
}
