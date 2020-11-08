package usonia.foundation

data class Device(
    val id: Uuid,
    val name: String,
    val capabilities: Capabilities,
    val fixture: Fixture? = null,
    val room: Uuid? = null,
    val siblings: Set<Uuid> = emptySet()
)

