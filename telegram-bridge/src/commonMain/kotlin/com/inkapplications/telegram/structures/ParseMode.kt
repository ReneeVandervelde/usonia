package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.jvm.JvmInline

/**
 * Modes for parsing entities in the message text.
 */
@Serializable(with = ParseMode.InlineSerializer::class)
@JvmInline
value class ParseMode(val key: String) {
    companion object {
        val MarkdownV2 = ParseMode("MarkdownV2")
        val Html = ParseMode("html")
        val Markdown = ParseMode("Markdown")
    }

    internal object InlineSerializer: DelegateSerializer<String, ParseMode>(String.serializer()) {
        override fun serialize(data: ParseMode): String = data.key
        override fun deserialize(data: String): ParseMode = ParseMode(data)
    }
}
