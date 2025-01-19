package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

/**
 * This object represents the contents of a file to be uploaded.
 *
 * TODO: This currently does not support local files.
 */
@Serializable(with = InputFile.InlineSerializer::class)
abstract class InputFile private constructor() {
    data class FileId(val id: String): InputFile()
    data class Url(val url: String): InputFile()

    internal object InlineSerializer: DelegateSerializer<String, InputFile>(String.serializer()) {
        override fun serialize(data: InputFile): String = when (data) {
            is InputFile.FileId -> data.id
            is InputFile.Url -> data.url
            else -> throw IllegalArgumentException("Unsupported data type")
        }

        override fun deserialize(data: String): InputFile = when {
            data.startsWith("http", ignoreCase = true) -> InputFile.Url(data)
            else -> InputFile.FileId(data)
        }
    }
}

