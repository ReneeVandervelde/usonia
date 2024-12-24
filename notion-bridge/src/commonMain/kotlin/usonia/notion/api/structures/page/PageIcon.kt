package usonia.notion.api.structures.page

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PageIconSerializer::class)
sealed interface PageIcon {
    @Serializable
    data class Emoji(
        val emoji: String,
    ): PageIcon

    object Unknown: PageIcon
}

internal class PageIconSerializer: KSerializer<PageIcon> {
    @Serializable
    private data class PageIconSurrogate(
        val type: String,
        val emoji: String? = null,
    )

    override val descriptor = PageIconSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): PageIcon {
        val surrogate = PageIconSurrogate.serializer().deserialize(decoder)
        return when (surrogate.type) {
            "emoji" -> PageIcon.Emoji(surrogate.emoji!!)
            else -> PageIcon.Unknown
        }
    }

    override fun serialize(encoder: Encoder, value: PageIcon) {
        val surrogate = when (value) {
            is PageIcon.Emoji -> PageIconSurrogate(
                type = "emoji",
                emoji = value.emoji,
            )
            PageIcon.Unknown -> throw IllegalArgumentException("Cannot serialize unknown page icon")
        }
        PageIconSurrogate.serializer().serialize(encoder, surrogate)
    }
}
