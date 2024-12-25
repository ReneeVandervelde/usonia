package usonia.notion.api.structures.block

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CodeLanguage(val value: String)
{
    companion object
    {
        val KOTLIN = CodeLanguage("kotlin")
        val PLAIN_TEXT = CodeLanguage("plain text")
    }
}
