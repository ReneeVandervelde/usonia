package usonia.notion.api.structures

@JvmInline
value class NotionBearerToken(val value: String) {
    override fun toString(): String {
        return "Bearer Token [REDACTED]"
    }
}
