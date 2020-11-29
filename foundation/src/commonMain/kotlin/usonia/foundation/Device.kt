package usonia.foundation

data class Device(
    val id: Uuid,
    val name: String,
    val capabilities: Capabilities,
    val source: ExternalSource? = null,
    val fixture: Fixture? = null,
    val siblings: Set<Uuid> = emptySet(),
)

