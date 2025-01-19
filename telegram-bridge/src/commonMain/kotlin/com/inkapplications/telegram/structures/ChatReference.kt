package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

/**
 * Reference to a chat, either by name or ID
 */
@Serializable(with = ChatReference.InlineSerializer::class)
abstract class ChatReference private constructor() {
    internal abstract val serialized: String

    /**
     * The unique ID of the chat
     */
    @Serializable(with = Id.InlineSerializer::class)
    data class Id(val value: Long): ChatReference() {
        override val serialized: String = value.toString()

        internal class InlineSerializer: DelegateSerializer<Long, Id>(Long.serializer()) {
            override fun serialize(data: Id): Long = data.value
            override fun deserialize(data: Long): Id = Id(data)
        }
    }

    /**
     * A username handle or channel
     */
    class Handle(
        handle: String
    ): ChatReference() {
        val value = if (handle.startsWith('@')) handle else "@$handle"
        override val serialized: String = handle
        override fun equals(other: Any?): Boolean = (other as? Handle)?.value == value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = "Handle(value=$value)"
    }

    internal object InlineSerializer: DelegateSerializer<String, ChatReference>(String.serializer()) {
        override fun serialize(data: ChatReference): String = data.serialized
        override fun deserialize(data: String): ChatReference = when {
            data.startsWith('@') -> Handle(data)
            else -> Id(data.toLong())
        }
    }
}
