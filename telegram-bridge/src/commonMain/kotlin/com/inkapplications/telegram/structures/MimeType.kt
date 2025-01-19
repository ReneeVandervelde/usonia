package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.jvm.JvmInline

/**
 * Wrapper for the mime type of a file
 */
@JvmInline
@Serializable(with = MimeType.InlineSerializer::class)
value class MimeType(val code: String) {
    internal object InlineSerializer: DelegateSerializer<String, MimeType>(String.serializer()) {
        override fun serialize(data: MimeType): String = data.code
        override fun deserialize(data: String): MimeType = MimeType(data)
    }
}
