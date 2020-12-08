package usonia.foundation

sealed class Bridge {
    abstract val id: Uuid
    abstract val name: String
    abstract val deviceMap: Map<Uuid, String>

    data class Generic(
        override val id: Uuid,
        override val name: String,
        override val deviceMap: Map<Uuid, String>,
        val host: String,
        val port: Int,
        val actionsPath: String?,
        val parameters: ParameterBag,
    ): Bridge()

    data class Hue(
        override val id: Uuid,
        override val name: String,
        override val deviceMap: Map<Uuid, String>,
        val baseUrl: String,
        val token: String,
    ): Bridge()
}
