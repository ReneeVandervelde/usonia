package usonia.foundation

data class Bridge(
    val id: Identifier,
    val name: String,
    val service: String,
    val parameters: Map<String, String>,
)
