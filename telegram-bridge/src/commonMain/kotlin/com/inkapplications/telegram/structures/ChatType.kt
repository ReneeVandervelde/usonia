package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.jvm.JvmInline

/**
 * Data wrapper for chat types.
 *
 * The value may be one of any of the companion objects listed. If it does
 * not match any of these, it is an unknown type to this SDK version.
 */
@JvmInline
@Serializable(with = ChatType.InlineSerializer::class)
value class ChatType private constructor(val key: String) {
    companion object {
        val Private = ChatType("private")
        val Group = ChatType("group")
        val SuperGroup = ChatType("supergroup")
        val Channel = ChatType("channel")

        fun values() = setOf(Private, Group, SuperGroup, Channel)
    }

    internal object InlineSerializer: DelegateSerializer<String, ChatType>(String.serializer()) {
        override fun serialize(data: ChatType): String = data.key
        override fun deserialize(data: String): ChatType = ChatType(data)
    }
}
