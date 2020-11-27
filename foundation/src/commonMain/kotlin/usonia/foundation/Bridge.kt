package usonia.foundation

data class Bridge(
    val id: Uuid,
    val name: String,
    val host: String,
    val port: Int,
    val actionsPath: String?,
    val parameters: ParameterBag,
)
