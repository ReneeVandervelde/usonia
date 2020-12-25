package usonia.foundation

data class Device(
    val id: Identifier,
    val name: String,
    val capabilities: Capabilities,
    val fixture: Fixture? = null,
    val siblings: Set<Identifier> = emptySet(),
    val parent: ExternalAssociation? = null,
)

