package usonia.foundation

data class Bridge(
    val id: Uuid,
    val name: String,
    val service: String,
    val deviceMap: Map<Uuid, String>,
    val parameters: Map<String, String>,
)
