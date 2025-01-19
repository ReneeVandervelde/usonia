package com.inkapplications.telegram.structures

import kotlinx.serialization.KSerializer
import com.inkapplications.telegram.structures.Message as MessageStructure
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.KClass

/**
 * This object represents an incoming update.
 */
@Serializable(with = Update.Serializer::class)
open class Update private constructor(
    /**
     * The update's unique identifier.
     *
     * Update identifiers start from a certain positive number and increase
     * sequentially.
     * This ID becomes especially handy if you're using webhooks,
     * since it allows you to ignore repeated updates or to restore
     * the correct update sequence, should they get out of order.
     *
     * If there are no new updates for at least a week, then identifier
     * of the next update will be chosen randomly instead of sequentially.
     */
    val id: Long
) {
    /**
     * New incoming message of any kind - text, photo, sticker, etc.
     */
    class MessageUpdate(
        id: Long,
        val message: MessageStructure,
    ): Update(id)

    /**
     * New version of a message that is known to the bot and was edited
     */
    class EditedMessageUpdate(
        id: Long,
        val message: MessageStructure,
    ): Update(id)

    /**
     * New incoming channel post of any kind - text, photo, sticker, etc.
     */
    class ChannelPostUpdate(
        id: Long,
        val message: MessageStructure,
    ): Update(id)

    /**
     * New version of a channel post that is known to the bot and was edited
     */
    class EditedChannelPostUpdate(
        id: Long,
        val message: MessageStructure,
    ): Update(id)

    /**
     * New incoming inline query
     */
    class InlineQueryUpdate(
        id: Long,
        val query: InlineQuery,
    ): Update(id)

    internal object Serializer: DelegateSerializer<PolymorphicUpdate, Update>(PolymorphicUpdate.serializer()) {
        override fun serialize(data: Update): PolymorphicUpdate = when (data) {
            is MessageUpdate -> PolymorphicUpdate(
                id = data.id,
                message = data.message,
            )
            is EditedMessageUpdate -> PolymorphicUpdate(
                id = data.id,
                editedMessage = data.message,
            )
            is ChannelPostUpdate -> PolymorphicUpdate(
                id = data.id,
                channelPost = data.message,
            )
            is EditedChannelPostUpdate -> PolymorphicUpdate(
                id = data.id,
                editedChannelPost = data.message,
            )
            is InlineQueryUpdate -> PolymorphicUpdate(
                id = data.id,
                inlineQuery = data.query,
            )
            else -> PolymorphicUpdate(data.id)
        }

        override fun deserialize(data: PolymorphicUpdate): Update = when {
            data.message != null -> MessageUpdate(
                id = data.id,
                message = data.message
            )
            data.editedMessage != null -> EditedMessageUpdate(
                id = data.id,
                message = data.editedMessage,
            )
            data.channelPost != null -> ChannelPostUpdate(
                id = data.id,
                message = data.channelPost,
            )
            data.editedChannelPost != null -> ChannelPostUpdate(
                id = data.id,
                message = data.editedChannelPost,
            )
            data.inlineQuery != null -> InlineQueryUpdate(
                id = data.id,
                query = data.inlineQuery,
            )
            else -> Update(data.id)
        }
    }

    internal object TypeListSerializer: KSerializer<List<KClass<out Update>>> by ListSerializer(ClassSerializer)
    internal object ClassSerializer: DelegateSerializer<String, KClass<out Update>>(String.serializer()) {
        override fun serialize(data: KClass<out Update>): String = when (data) {
            MessageUpdate::class -> "message"
            EditedMessageUpdate::class -> "edited_message"
            ChannelPostUpdate::class -> "channel_post"
            EditedChannelPostUpdate::class -> "edited_channel_post"
            InlineQueryUpdate::class -> "inline_query"
            else -> throw IllegalArgumentException("Unknown Message type: $data")
        }

        override fun deserialize(data: String): KClass<out Update> = valueOf(data)
    }

    @Serializable
    internal data class PolymorphicUpdate(
        @SerialName("update_id")
        val id: Long,

        val message: MessageStructure? = null,

        @SerialName("edited_message")
        val editedMessage: MessageStructure? = null,

        @SerialName("channel_post")
        val channelPost: MessageStructure? = null,

        @SerialName("edited_channel_post")
        val editedChannelPost: MessageStructure? = null,

        @SerialName("inline_query")
        val inlineQuery: InlineQuery? = null,

        // TODO: More fields in this object.
    )

    companion object {
        fun valueOf(key: String) = when (key) {
            "message" -> MessageUpdate::class
            "edited_message" -> EditedMessageUpdate::class
            "channel_post" -> ChannelPostUpdate::class
            "edited_channel_post" -> EditedChannelPostUpdate::class
            "inline_query" -> InlineQueryUpdate::class
            else -> throw IllegalArgumentException("Unknown Message type: $key")
        }
    }
}


