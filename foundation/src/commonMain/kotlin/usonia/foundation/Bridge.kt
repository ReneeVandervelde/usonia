package usonia.foundation

import kotlinx.serialization.Serializable

@Serializable
data class Bridge(
    val id: Identifier,
    val name: String,
    val service: String,
    val parameters: Map<String, String> = emptyMap(),
)
