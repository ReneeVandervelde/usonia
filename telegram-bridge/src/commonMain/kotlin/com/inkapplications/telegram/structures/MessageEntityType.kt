package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlin.jvm.JvmInline

/**
 * Type associated with a [MessageEntity].
 *
 * The value may be one of any of the companion objects listed. If it does
 * not match any of these, it is an unknown type to this SDK version.
 */
@JvmInline
@Serializable(with = MessageEntityType.InlineSerializer::class)
value class MessageEntityType private constructor(val key: String) {
    companion object {
        val Mention = MessageEntityType("mention")
        val HashTag = MessageEntityType("hashtag")
        val CashTag = MessageEntityType("cashtag")
        val BotCommand = MessageEntityType("bot_command")
        val Url = MessageEntityType("url")
        val Email = MessageEntityType("email")
        val PhoneNumber = MessageEntityType("phone_number")
        val Bold = MessageEntityType("bold")
        val Italic = MessageEntityType("italic")
        val Underline = MessageEntityType("underline")
        val Strikethrough = MessageEntityType("strikethrough")
        val Spoiler = MessageEntityType("spoiler")
        val Code = MessageEntityType("code")
        val Pre = MessageEntityType("pre")
        val TextLink = MessageEntityType("text_link")
        val TextMention = MessageEntityType("text_mention")

        fun values() = setOf(
            Mention,
            HashTag,
            CashTag,
            BotCommand,
            Url,
            Email,
            PhoneNumber,
            Bold,
            Italic,
            Underline,
            Strikethrough,
            Spoiler,
            Code,
            Pre,
            TextLink,
            TextMention,
        )
    }

    internal object InlineSerializer: DelegateSerializer<String, MessageEntityType>(String.serializer()) {
        override fun serialize(data: MessageEntityType): String = data.key
        override fun deserialize(data: String): MessageEntityType = MessageEntityType(data)
    }
}
