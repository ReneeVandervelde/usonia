package usonia.foundation

data class Device(
    val id: Uuid,
    val name: String,
    val fixture: Fixture? = null,
    val capabilities: Capabilities,
    val siblings: Set<Uuid> = emptySet()
)

