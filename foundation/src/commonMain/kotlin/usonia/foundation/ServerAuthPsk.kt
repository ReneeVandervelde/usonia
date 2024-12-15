package usonia.foundation

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class ServerAuthPsk(val psk: String)
