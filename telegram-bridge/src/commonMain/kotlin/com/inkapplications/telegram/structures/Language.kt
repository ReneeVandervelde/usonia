package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.jvm.JvmInline

/**
 * Inline class wrapping an IETF Language Tag.
 */
@JvmInline
@Serializable(with = Language.InlineSerializer::class)
value class Language(val code: String) {
    internal object InlineSerializer: DelegateSerializer<String, Language>(String.serializer()) {
        override fun serialize(data: Language): String = data.code
        override fun deserialize(data: String): Language = Language(data)
    }
}
